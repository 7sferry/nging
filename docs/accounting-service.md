# Accounting Service

Port: `7384`
Module: `accounting-service/`

## Purpose

Serves account balance data. Called internally by user-service to enrich user responses, and also exposed through nginx for direct access.

## Dependencies

- `spring-boot-starter-web` — REST API
- `lombok`

Does **not** depend on `nging-common`, PostgreSQL, or Redis. JWT validation is handled centrally by the nginx gateway.

## Endpoints

### GET /accounts/balance/{userId}

Returns the account balance for a specific user.

**Response (200):**
```json
{
  "user_id": 1,
  "balance": 15000.50
}
```

**Response (404):**
```json
{
  "error": "No account found for user 99"
}
```

Through nginx: `GET http://localhost:9090/api/accounts/balance/1` with `Authorization: Bearer <token>`

### GET /accounts/balances

Returns all account balances.

**Response (200):**
```json
{
  "accounts": [
    { "user_id": 1, "balance": 15000.50 },
    { "user_id": 2, "balance": 8250.75 },
    { "user_id": 3, "balance": 3420.00 }
  ]
}
```

Through nginx: `GET http://localhost:9090/api/accounts/balances` with `Authorization: Bearer <token>`

## Hardcoded Balances

| User ID | Balance     |
|---------|-------------|
| 1       | $15,000.50  |
| 2       | $8,250.75   |
| 3       | $3,420.00   |

## Access Patterns

1. **Internal (from user-service):** Direct call to `http://localhost:7384/accounts/balance/{userId}`. No authentication required — trusted service-to-service call.

2. **External (through nginx):** `http://localhost:9090/api/accounts/*`. JWT validated by nginx's `auth_request` before the request reaches this service.

## Configuration

```yaml
server:
  port: 7384
spring:
  application:
    name: accounting-service
```
