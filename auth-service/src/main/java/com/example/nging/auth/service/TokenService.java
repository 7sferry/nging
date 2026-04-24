package com.example.nging.auth.service;

import com.example.nging.auth.entity.UserSession;
import com.example.nging.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserSessionRepository sessionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.refresh-token.rotate-after-seconds}")
    private long rotateAfterSeconds;

    @Value("${auth.refresh-token.expire-after-seconds}")
    private long expireAfterSeconds;

    @Value("${auth.access-token.cache-ttl-seconds}")
    private long accessTokenCacheTtlSeconds;

    public record RefreshResult(String accessToken, String refreshToken, boolean rotated, String username,
                                String error) {}

    /**
     * Create a new session on login. Returns the raw (plaintext) refresh token for the cookie.
     */
    public String createSession(String username, String accessToken) {
        String rawToken = UUID.randomUUID().toString();
        String hash = sha256(rawToken);
        Instant now = Instant.now();

        UserSession session = UserSession.builder()
                .username(username)
                .tokenHash(hash)
                .rotateAt(now.plusSeconds(rotateAfterSeconds))
                .expiresAt(now.plusSeconds(expireAfterSeconds))
                .createdAt(now)
                .invalidated(false)
                .build();
        sessionRepository.save(session);

        cacheAccessToken(hash, accessToken);

        return rawToken;
    }

    /**
     * Refresh an access token using the refresh token.
     *
     * @param rawRefreshToken  the plaintext refresh token from the cookie
     * @param accessTokenGenerator  function that generates a new JWT given a username
     */
    @Transactional
    public RefreshResult refresh(String rawRefreshToken, Function<String, String> accessTokenGenerator) {
        String hash = sha256(rawRefreshToken);
        var opt = sessionRepository.findByTokenHashAndInvalidatedFalse(hash);

        if (opt.isEmpty()) {
            return new RefreshResult(null, null, false, null, "Invalid refresh token");
        }

        UserSession session = opt.get();
        Instant now = Instant.now();

        // CASE 1: Expired — must re-login
        if (now.isAfter(session.getExpiresAt())) {
            session.setInvalidated(true);
            sessionRepository.save(session);
            deleteAccessTokenCache(hash);
            return new RefreshResult(null, null, false, null, "Refresh token expired");
        }

        // CASE 2: Past rotation time — rotate refresh token, issue new access token
        if (now.isAfter(session.getRotateAt())) {
            session.setInvalidated(true);
            sessionRepository.save(session);
            deleteAccessTokenCache(hash);

            String newAccessToken = accessTokenGenerator.apply(session.getUsername());

            String newRawToken = UUID.randomUUID().toString();
            String newHash = sha256(newRawToken);
            UserSession newSession = UserSession.builder()
                    .username(session.getUsername())
                    .tokenHash(newHash)
                    .rotateAt(now.plusSeconds(rotateAfterSeconds))
                    .expiresAt(now.plusSeconds(expireAfterSeconds))
                    .createdAt(now)
                    .invalidated(false)
                    .build();
            sessionRepository.save(newSession);

            cacheAccessToken(newHash, newAccessToken);

            return new RefreshResult(newAccessToken, newRawToken, true, session.getUsername(), null);
        }

        // CASE 3: Before rotation — reuse refresh token, try cache for access token
        String cachedAccessToken = getAccessTokenFromCache(hash);
        if (cachedAccessToken != null) {
            return new RefreshResult(cachedAccessToken, null, false, session.getUsername(), null);
        }

        String newAccessToken = accessTokenGenerator.apply(session.getUsername());
        cacheAccessToken(hash, newAccessToken);

        return new RefreshResult(newAccessToken, null, false, session.getUsername(), null);
    }

    /**
     * Invalidate a session on logout.
     */
    @Transactional
    public void invalidateSession(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        sessionRepository.findByTokenHashAndInvalidatedFalse(hash)
                .ifPresent(session -> {
                    session.setInvalidated(true);
                    sessionRepository.save(session);
                    deleteAccessTokenCache(hash);
                });
    }

    private void cacheAccessToken(String tokenHash, String accessToken) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey(tokenHash),
                    accessToken,
                    Duration.ofSeconds(accessTokenCacheTtlSeconds)
            );
        } catch (Exception e) {
            // Redis down — degrade gracefully, just skip caching
        }
    }

    private String getAccessTokenFromCache(String tokenHash) {
        try {
            return redisTemplate.opsForValue().get(cacheKey(tokenHash));
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteAccessTokenCache(String tokenHash) {
        try {
            redisTemplate.delete(cacheKey(tokenHash));
        } catch (Exception e) {
            // ignore
        }
    }

    private String cacheKey(String tokenHash) {
        return "access_token:" + tokenHash;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
