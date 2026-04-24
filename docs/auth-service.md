# Auth Service

Port: `7381`
Module: `auth-service/`

## Purpose

Handles all authentication: issuing JWT access tokens, managing refresh token sessions in PostgreSQL, caching access tokens in Redis, and validating tokens for the nginx gateway.

## Dependencies

- `spring-boot-starter-web` — REST API
- `spring-boot-starter-data-jpa` — JPA/Hibernate for PostgreSQL
- `postgresql` — PostgreSQL JDBC driver
- `spring-boot-starter-data-redis` — Redis client
- `nging-common` — shared `JwtUtil` class
- `lombok` — `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`

## Endpoints

### POST /auth/login

Authenticates credentials, issues access token + refresh token.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "admin"
}
```

**Side effects:**
- Creates a `user_session` row in PostgreSQL (stores SHA-256 hash of refresh token)
- Caches the access token in Redis (TTL: 1 hour)
- Sets `refresh_token` HttpOnly cookie (expires: 7 days)

**Response (401):**
```json
{
  "error": "Invalid username or password"
}
```

**Hardcoded credentials:**

| Username | Password  | Client ID  | Roles          | Work Entities          |
|----------|-----------|------------|----------------|------------------------|
| admin    | admin123  | CLIENT-001 | ADMIN, MANAGER | ENTITY-A, B, C         |
| user     | user123   | CLIENT-002 | USER           | ENTITY-A               |

Through nginx: `POST http://localhost:9090/api/auth/login`

### POST /auth/refresh

Returns an access token using the refresh token cookie. The access token is returned in the response body — the browser stores it in a JS variable (memory only).

**Request:** No body. The `refresh_token` cookie is sent automatically by the browser.

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "admin"
}
```

**Refresh logic:**

| Condition | Action | Cookie |
|-----------|--------|--------|
| Token expired (> 7 days) | Return 401, user must re-login | Cleared |
| Past rotation time (1-7 days) | Invalidate old session, create new one, generate new access token | New cookie |
| Before rotation (< 1 day) | Return cached access token from Redis, or generate new one | Unchanged |

On rotation, the new session gets fresh `rotateAt` and `expiresAt` timestamps. An active user who refreshes regularly will never be forced to re-login.

**Response (401):**
```json
{
  "error": "Refresh token expired"
}
```

Through nginx: `POST http://localhost:9090/api/auth/refresh`

### POST /auth/logout

Invalidates the refresh token session and clears the cookie.

**Request:** No body. The `refresh_token` cookie is sent automatically.

**Response (200):**
```json
{
  "message": "Logged out"
}
```

**Side effects:**
- Marks the `user_session` row as `invalidated = true`
- Deletes the cached access token from Redis
- Clears the `refresh_token` cookie (MaxAge=0)

Through nginx: `POST http://localhost:9090/api/auth/logout`

### GET /auth/validate

Called internally by nginx's `auth_request` directive. Not intended for direct use by clients.

Validates the JWT access token from the `Authorization: Bearer <token>` header.

**Response (200):** Empty body, with response headers:
- `X-Auth-User` — username from JWT `sub` claim
- `X-Auth-Client-Id` — from JWT `clientId` claim
- `X-Auth-Roles` — comma-separated roles from JWT `roles` claim
- `X-Auth-Work-Entities` — comma-separated entities from JWT `workEntities` claim

Nginx captures these headers and forwards them to downstream services.

**Response (401):** Empty body. Nginx returns 401 to the client.

## Refresh Token Cookie Properties

```
refresh_token=<uuid>
HttpOnly        → JavaScript cannot read it (XSS protection)
Path=/api/auth/refresh  → only sent on refresh requests
SameSite=Lax    → not sent on cross-origin POST (CSRF protection)
MaxAge=604800   → 7 days
```

## Database: user_session Table

| Column      | Type         | Purpose                              |
|-------------|--------------|--------------------------------------|
| id          | BIGINT (PK)  | Auto-generated primary key           |
| username    | VARCHAR      | Session owner                        |
| token_hash  | VARCHAR(64)  | SHA-256 hex of the opaque refresh token |
| rotate_at   | TIMESTAMP    | After this, issue a new refresh token |
| expires_at  | TIMESTAMP    | After this, session is dead          |
| created_at  | TIMESTAMP    | When the session was created         |
| invalidated | BOOLEAN      | Soft-delete flag                     |

The refresh token is **never stored in plaintext** — only its SHA-256 hash. A database leak does not compromise active sessions.

Hibernate auto-creates this table from the JPA entity on startup (`ddl-auto: update`).

## Redis: Access Token Cache

- **Key:** `access_token:<sha256-of-refresh-token>`
- **Value:** the JWT access token string
- **TTL:** 1 hour (matches JWT expiration)

When the browser loses the access token (page reload, tab close) and calls `/refresh`, the cached token is returned if still valid. This avoids generating a new JWT on every page load.

Redis is accessed with graceful degradation — if Redis is down, the service generates a new token instead of failing.

## Key Classes

### AuthController

Entry point for login, refresh, logout, validate. Delegates token management to `TokenService`.

### TokenService

Core logic for session management:
- `createSession()` — saves session to PostgreSQL, caches access token in Redis
- `refresh()` — handles the three refresh cases (expired, rotate, reuse) as a `@Transactional` operation
- `invalidateSession()` — soft-deletes session, clears Redis cache
- SHA-256 hashing of refresh tokens

### UserSession (entity)

JPA entity mapping to `user_session` table. Indexed on `tokenHash` (unique) and `username`.

### UserSessionRepository

Spring Data JPA repository with `findByTokenHashAndInvalidatedFalse()`.

### RedisConfig

Configures `RedisTemplate<String, String>` with string serializers.

## Configuration

```yaml
server:
  port: 7381
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/nging
    username: nging
    password: 12345
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  data:
    redis:
      host: localhost
      port: 6379
      password: 12345

auth:
  refresh-token:
    rotate-after-seconds: 86400   # 1 day
    expire-after-seconds: 604800  # 7 days
  access-token:
    cache-ttl-seconds: 3600       # 1 hour
```

## Component Scan

The `@SpringBootApplication` class uses `@ComponentScan` to include `com.example.nging.common` so that the `JwtUtil` bean from the `common` module is discovered.
