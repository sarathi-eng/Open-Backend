package com.opencore.auth.core;

import com.opencore.auth.api.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AuthService {
    private final JwtService jwt;
    private final InMemoryOtpStore otpStore;
    private final InMemorySessionStore sessions;
    private final BruteForceProtector bruteForce;
    private final OpenCoreJwtProperties props;

    public AuthService(OpenCoreJwtProperties props) {
        this.props = props;
        this.jwt = new JwtService(props);
        this.otpStore = new InMemoryOtpStore();
        this.sessions = new InMemorySessionStore();
        this.bruteForce = new BruteForceProtector();
    }

    public OtpRequestResponse requestOtp(OtpRequestRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");
        }

        var issued = otpStore.issue(email);
        // In production: send OTP via email provider.
        return new OtpRequestResponse(issued.requestId(), issued.otp());
    }

    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        String otp = request.otp() == null ? "" : request.otp().trim();
        String requestId = request.requestId() == null ? "" : request.requestId().trim();
        if (email.isBlank() || otp.isBlank() || requestId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email, otp, requestId required");
        }

        String key = "email=" + email + "|ip=" + (clientIp == null ? "" : clientIp);
        if (bruteForce.isBlocked(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many failed attempts");
        }

        boolean ok = otpStore.verify(email, otp, requestId);
        if (!ok) {
            bruteForce.onFailure(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid otp");
        }

        bruteForce.onSuccess(key);

        String userId = userIdFromEmail(email);
        String role = "User";

        String access = jwt.issueAccessToken(userId, role);
        var issuedRefresh = jwt.issueRefreshToken(userId, request.deviceId());
        sessions.put(
                issuedRefresh.jti(),
                new InMemorySessionStore.Session(userId, request.deviceId(), issuedRefresh.expEpochSeconds(), false)
        );

        return new OtpVerifyResponse(access, issuedRefresh.token(), props.accessTtlSeconds());
    }

    public TokenRefreshResponse refresh(TokenRefreshRequest request, String clientIp) {
        String token = request.refreshToken() == null ? "" : request.refreshToken().trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken required");
        }

        JwtService.ParsedToken parsed;
        try {
            parsed = jwt.parseAndVerify(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
        }

        if (!"refresh".equals(parsed.typ()) || parsed.jti() == null || parsed.jti().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
        }

        var session = sessions.get(parsed.jti());
        if (session == null || session.revoked() || session.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "session revoked or expired");
        }

        // Rotate refresh token.
        sessions.revoke(parsed.jti());
        var newRefresh = jwt.issueRefreshToken(session.userId(), session.deviceId());
        sessions.put(newRefresh.jti(), new InMemorySessionStore.Session(session.userId(), session.deviceId(), newRefresh.expEpochSeconds(), false));

        String access = jwt.issueAccessToken(session.userId(), "User");
        return new TokenRefreshResponse(access, newRefresh.token(), props.accessTtlSeconds());
    }

    public void revoke(SessionRevokeRequest request) {
        String token = request.refreshToken() == null ? "" : request.refreshToken().trim();
        if (token.isBlank()) {
            return;
        }
        try {
            JwtService.ParsedToken parsed = jwt.parseAndVerify(token);
            if (parsed.jti() != null && !parsed.jti().isBlank()) {
                sessions.revoke(parsed.jti());
            }
        } catch (Exception ignored) {
            // Idempotent
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String userIdFromEmail(String email) {
        return UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
