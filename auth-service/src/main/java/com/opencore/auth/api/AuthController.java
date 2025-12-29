package com.opencore.auth.api;

import com.opencore.auth.api.dto.*;
import com.opencore.auth.core.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/email-otp/request")
    public ResponseEntity<OtpRequestResponse> requestOtp(@RequestBody OtpRequestRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.requestOtp(request, clientIp(http)));
    }

    @PostMapping("/login/email-otp/verify")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(@RequestBody OtpVerifyRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.verifyOtp(request, clientIp(http)));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.refresh(request, clientIp(http)));
    }

    @PostMapping("/sessions/revoke")
    public ResponseEntity<SessionRevokeResponse> revoke(@RequestBody SessionRevokeRequest request) {
        authService.revoke(request);
        return ResponseEntity.ok(new SessionRevokeResponse(true));
    }

    @GetMapping("/oauth2/{provider}/start")
    public ResponseEntity<String> oauthStart(@PathVariable String provider) {
        return ResponseEntity.status(501).body("oauth2 start not implemented: " + provider);
    }

    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<String> oauthCallback(@PathVariable String provider) {
        return ResponseEntity.status(501).body("oauth2 callback not implemented: " + provider);
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
