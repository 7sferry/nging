# Architecture Overview

## System Diagram

```
                         ┌──────────────────────────────────────────────┐
                         │            Nginx Gateway (port 9090)        │
                         │                                              │
  Browser ──────────────►│  /api/auth/*     ──► auth-service (7381)     │
                         │  /api/users/*    ──► user-service (7382)     │
                         │  /api/accounts/* ──► accounting-service(7384)│
                         │  /*              ──► static HTML/JS/CSS      │
                         └──────────────────────────────────────────────┘
                                                    │
                                    ┌───────────────┘
                                    ▼
                         user-service ──────► accounting-service
                            (internal service-to-service call)

  ┌─────────────┐     ┌─────────────┐
  │  PostgreSQL  │     │    Redis    │
  │  (port 5432) │     │ (port 6379) │
  └──────┬───────┘     └──────┬──────┘
         │                    │
         └────── auth-service ┘
```

## Services

| Service             | Port | Purpose                                        |
|---------------------|------|------------------------------------------------|
| auth-service        | 7381 | Login, JWT validation, refresh/access tokens   |
| user-service        | 7382 | User data + fetches balance from accounting    |
| accounting-service  | 7384 | Account balance data                           |
| nginx               | 9090 | API gateway, centralized auth, static UI       |

## Infrastructure

| Component  | Port | Purpose                                       |
|------------|------|-----------------------------------------------|
| PostgreSQL | 5432 | Stores refresh token sessions (`user_session`) |
| Redis      | 6379 | Caches access tokens for fast recovery         |

## Authentication Flow

### Login

```
1. Browser POST /api/auth/login with credentials
2. auth-service validates credentials
3. auth-service generates JWT access token (with clientId, roles, workEntities)
4. auth-service creates session in PostgreSQL (stores SHA-256 hash of refresh token)
5. auth-service caches access token in Redis (keyed by token hash, TTL 1 hour)
6. auth-service returns access_token in body + sets refresh_token HttpOnly cookie
7. Browser stores access token in a JS variable (memory only)
```

### API Request (with valid access token)

```
1. Browser sends request with Authorization: Bearer <access_token>
2. Nginx makes auth_request subrequest to auth-service /auth/validate
3. auth-service validates JWT
   - 200 → nginx captures X-Auth-User, X-Auth-Client-Id, X-Auth-Roles, X-Auth-Work-Entities
   - 401 → nginx returns 401 to browser
4. Nginx forwards request + auth headers to target service
```

### Token Refresh (access token lost or expired)

```
1. Browser calls POST /api/auth/refresh (refresh_token cookie sent automatically)
2. auth-service looks up session in PostgreSQL by SHA-256 hash
3. Check refresh token state:
   a. Expired (> 7 days) → 401, user must re-login
   b. Past rotation time (> 1 day, < 7 days) → rotate: invalidate old session,
      create new session, set new cookie, generate new access token
   c. Before rotation time (< 1 day) → check Redis cache for access token,
      return cached if valid, else generate new one
4. Browser receives access token in memory variable
```

## Token Strategy

| Token          | Type         | Storage                    | Lifetime |
|----------------|--------------|----------------------------|----------|
| Access token   | JWT          | JS variable (memory only)  | 1 hour   |
| Refresh token  | Opaque UUID  | HttpOnly cookie            | 7 days   |

The access token is never persisted — not in localStorage, sessionStorage, or cookies. When the page reloads or the tab closes, it is gone. The browser recovers it by calling `/api/auth/refresh`, which either returns a cached token from Redis or generates a new one.

## JWT Payload

```json
{
  "sub": "admin",
  "clientId": "CLIENT-001",
  "roles": ["ADMIN", "MANAGER"],
  "workEntities": ["ENTITY-A", "ENTITY-B", "ENTITY-C"],
  "iat": 1745...,
  "exp": 1745...
}
```

## Service-to-Service Communication

User-service calls accounting-service directly on `localhost:7384` to fetch account balances. This is an internal call that does not go through nginx, so no JWT is needed.

## Module Structure

```
nging/
├── pom.xml                 # Parent POM (aggregator, packaging=pom)
├── common/                 # Shared library (JwtUtil)
├── auth-service/           # Authentication microservice
├── user-service/           # User data microservice
├── accounting-service/     # Account balance microservice
├── static/                 # Plain HTML/JS/CSS (served by nginx)
├── nginx/                  # Nginx gateway configuration
├── docker-compose.yml      # Nginx, PostgreSQL, Redis
└── docs/                   # Documentation
```
