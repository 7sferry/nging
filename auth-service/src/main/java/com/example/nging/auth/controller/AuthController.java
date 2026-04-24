package com.example.nging.auth.controller;

import com.example.nging.auth.service.TokenService;
import com.example.nging.common.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Value("${auth.refresh-token.expire-after-seconds}")
    private long refreshTokenExpireSeconds;

    private record UserInfo(String password, String clientId, List<String> roles, List<String> workEntities) {}

    private static final Map<String, UserInfo> USERS = Map.of(
            "admin", new UserInfo("admin123", "CLIENT-001", List.of("ADMIN", "MANAGER"), List.of("ENTITY-A", "ENTITY-B", "ENTITY-C")),
            "user", new UserInfo("user123", "CLIENT-002", List.of("USER"), List.of("ENTITY-A"))
    );

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        UserInfo userInfo = USERS.get(request.username());
        if (userInfo == null || !userInfo.password().equals(request.password())) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid username or password"));
        }

        String accessToken = generateAccessToken(request.username(), userInfo);
        String rawRefreshToken = tokenService.createSession(request.username(), accessToken);

        setRefreshCookie(response, rawRefreshToken, (int) refreshTokenExpireSeconds);

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "username", request.username()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshToken(request);
        if (rawRefreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
        }

        var result = tokenService.refresh(rawRefreshToken, this::generateAccessTokenForUser);

        if (result.error() != null) {
            clearRefreshCookie(response);
            return ResponseEntity.status(401).body(Map.of("error", result.error()));
        }

        if (result.rotated()) {
            setRefreshCookie(response, result.refreshToken(), (int) refreshTokenExpireSeconds);
        }

        return ResponseEntity.ok(Map.of(
                "access_token", result.accessToken(),
                "username", result.username()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshToken(request);
        if (rawRefreshToken != null) {
            tokenService.invalidateSession(rawRefreshToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/validate")
    public ResponseEntity<Void> validate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        try {
            var claims = jwtUtil.parseToken(header.substring(7));
            String roles = String.join(",", (List<String>) claims.get("roles"));
            String workEntities = String.join(",", (List<String>) claims.get("workEntities"));

            return ResponseEntity.ok()
                    .header("X-Auth-User", claims.getSubject())
                    .header("X-Auth-Client-Id", (String) claims.get("clientId"))
                    .header("X-Auth-Roles", roles)
                    .header("X-Auth-Work-Entities", workEntities)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    private String generateAccessTokenForUser(String username) {
        UserInfo userInfo = USERS.get(username);
        if (userInfo == null) return null;
        return generateAccessToken(username, userInfo);
    }

    private String generateAccessToken(String username, UserInfo userInfo) {
        return jwtUtil.generateToken(username, userInfo.clientId(), userInfo.roles(), userInfo.workEntities());
    }

    private void setRefreshCookie(HttpServletResponse response, String value, int maxAge) {
        Cookie cookie = new Cookie("refresh_token", value);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public record LoginRequest(String username, String password) {}
}
