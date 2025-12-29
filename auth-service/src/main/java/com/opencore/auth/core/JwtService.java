package com.opencore.auth.core;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class JwtService {
    private final OpenCoreJwtProperties props;
    private final SecretKey key;

    public JwtService(OpenCoreJwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(String userId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        "typ", "access",
                        "role", role
                ))
                .signWith(key)
                .compact();
    }

    public IssuedRefreshToken issueRefreshToken(String userId, String deviceId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.refreshTtlSeconds());
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(props.issuer())
                .subject(userId)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        "typ", "refresh",
                        "deviceId", deviceId == null ? "" : deviceId
                ))
                .signWith(key)
                .compact();

        return new IssuedRefreshToken(token, jti, exp.getEpochSecond());
    }

    public ParsedToken parseAndVerify(String jwt) {
        var claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        String typ = claims.get("typ", String.class);
        String sub = claims.getSubject();
        String jti = claims.getId();
        long exp = claims.getExpiration().toInstant().getEpochSecond();
        return new ParsedToken(typ, sub, jti, exp);
    }

    public record IssuedRefreshToken(String token, String jti, long expEpochSeconds) {}

    public record ParsedToken(String typ, String subject, String jti, long expEpochSeconds) {}
}
