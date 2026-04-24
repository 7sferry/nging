package com.example.nging.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private static final String SECRET = "my-super-secret-key-for-jwt-demo-at-least-32-bytes";
    private static final long EXPIRATION_MS = 300_000; // 5 minutes

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateToken(String username, String clientId, List<String> roles, List<String> workEntities) {
        return Jwts.builder()
                .subject(username)
                .claim("clientId", clientId)
                .claim("roles", roles)
                .claim("workEntities", workEntities)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
