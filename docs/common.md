# Common Module

Module: `common/`
Artifact: `nging-common`

## Purpose

A shared library containing `JwtUtil` — the JWT token generation and validation logic. This is a plain JAR (not an executable Spring Boot app), used as a dependency by `auth-service`.

## Dependencies

- `spring-context` — for the `@Component` annotation so `JwtUtil` can be auto-discovered.
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` — JJWT library for creating and parsing JWT tokens.

## JwtUtil

Location: `com.example.nging.common.jwt.JwtUtil`

### Configuration

- **Secret key:** `my-super-secret-key-for-jwt-demo-at-least-32-bytes` (hardcoded for demo; in production, use environment variables or a secrets manager).
- **Algorithm:** HMAC-SHA (determined by JJWT based on key length).
- **Token expiration:** 1 hour (3,600,000 ms).

### Methods

#### `generateToken(String username, String clientId, List<String> roles, List<String> workEntities)`

Creates a signed JWT with:
- `sub` (subject): username
- `clientId`: the client identifier
- `roles`: list of roles (e.g., `["ADMIN", "MANAGER"]`)
- `workEntities`: list of work entities (e.g., `["ENTITY-A", "ENTITY-B"]`)
- `iat` (issued at): current time
- `exp` (expiration): current time + 1 hour

Returns the compact JWT string.

#### `parseToken(String token)`

Parses and validates a JWT string. Returns the `Claims` object containing the token payload.

Throws an exception if the token is invalid, expired, or tampered with.

## Who Uses This Module

Only `auth-service` depends on `nging-common`. It uses `JwtUtil` to:
1. Generate access tokens on login and refresh (`generateToken`).
2. Validate access tokens on the `/_validate` subrequest from nginx (`parseToken`).

Other services (user-service, accounting-service) do not depend on this module.

## Build Note

This module does **not** include `spring-boot-maven-plugin`. It produces a plain JAR, not an executable Spring Boot JAR.
