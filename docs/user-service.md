# User Service

Port: `7382`
Module: `user-service/`

## Purpose

Serves user data. Enriches each user response with account balance by calling accounting-service internally.

## Dependencies

- `spring-boot-starter-web` — REST API + `RestClient` for service-to-service calls
- `lombok` — `@RequiredArgsConstructor`

Does **not** depend on `nging-common`, PostgreSQL, or Redis. JWT validation is handled centrally by the nginx gateway.

## Endpoints

### GET /users

Returns all users with their account balances.

**Required headers** (set by nginx after JWT validation):
- `X-Auth-User` — authenticated username
- `X-Auth-Client-Id` — client ID from JWT
- `X-Auth-Roles` — comma-separated roles
- `X-Auth-Work-Entities` — comma-separated work entities

**Response (200):**
```json
{
  "auth": {
    "username": "admin",
    "client_id": "CLIENT-001",
    "roles": ["ADMIN", "MANAGER"],
    "work_entities": ["ENTITY-A", "ENTITY-B", "ENTITY-C"]
  },
  "users": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "role": "admin",
      "account_balance": 15000.50
    },
    ...
  ]
}
```

Through nginx: `GET http://localhost:9090/api/users/` with `Authorization: Bearer <token>`

### GET /users/{id}

Returns a single user with their account balance.

**Response (200):**
```json
{
  "auth": {
    "username": "admin",
    "client_id": "CLIENT-001",
    "roles": ["ADMIN", "MANAGER"],
    "work_entities": ["ENTITY-A", "ENTITY-B", "ENTITY-C"]
  },
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "admin",
    "account_balance": 15000.50
  }
}
```

**Response (404):**
```json
{
  "error": "User not found"
}
```

Through nginx: `GET http://localhost:9090/api/users/1` with `Authorization: Bearer <token>`

## Hardcoded Users

| ID | Name       | Email             | Role  |
|----|------------|-------------------|-------|
| 1  | John Doe   | john@example.com  | admin |
| 2  | Jane Smith | jane@example.com  | user  |
| 3  | Bob Wilson | bob@example.com   | user  |

## Key Classes

### UserController

- Reads `X-Auth-User`, `X-Auth-Client-Id`, `X-Auth-Roles`, `X-Auth-Work-Entities` headers from the request (set by nginx after centralized auth).
- Returns auth context in every response under the `"auth"` key.
- Calls `AccountingClient.getBalance()` for each user to fetch account balances.
- If accounting-service is unreachable, the balance field shows `"unavailable"`.

### AccountingClient

- Uses Spring's `RestClient` to call accounting-service at `http://localhost:7384`.
- The base URL is configured via `services.accounting.url` in `application.yaml`.
- This is a direct service-to-service call (does not go through nginx), so no JWT token is needed.

## Configuration

```yaml
server:
  port: 7382
spring:
  application:
    name: user-service
services:
  accounting:
    url: http://localhost:7384
```
