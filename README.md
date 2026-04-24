# nging

Microservices demo with Nginx API gateway, JWT authentication, and blue-green zero-downtime deployment.

## Architecture

```
                       ┌────────────────┐
                       │  Nginx :9090   │
                       │  (API Gateway) │
                       └──┬─────┬─────┬─┘
                          │     │     │
             /api/auth/   │     │     │  /api/accounts/
                          │     │     │
                 ┌────────▼┐ ┌──▼────┐ ┌▼────────────┐
                 │  auth   │ │ user  │ │  accounting  │
                 │ service │ │service│ │   service    │
                 └──┬───┬──┘ └──────┘ └──────────────┘
                    │   │
             ┌──────▼┐ ┌▼──────┐
             │Postgres│ │ Redis │
             └────────┘ └───────┘
```

| Service | What it does |
|---------|-------------|
| **auth-service** | Login, logout, token refresh, JWT validation. Stores refresh tokens in PostgreSQL, caches access tokens in Redis. |
| **user-service** | User profiles and contacts. Calls accounting-service internally for balance data. |
| **accounting-service** | Account balance data. |
| **nginx** | API gateway. Routes requests, validates JWTs via subrequest, serves static frontend. |

All protected routes (`/api/users/`, `/api/contacts/`, `/api/accounts/`) pass through nginx `auth_request` to auth-service before forwarding.

## Prerequisites

| Tool | Version |
|------|---------|
| Docker | 20.10+ |
| Docker Compose | v2+ |

Java and Maven are **not** needed on the host. Builds run inside Docker using multi-stage Dockerfiles.

## Getting Started

### 1. Clone and prepare

```bash
git clone <repo-url>
cd nging
chmod +x deploy.sh
```

### 2. Deploy

```bash
./deploy.sh blue
```

This will:
1. Start infrastructure (nginx, postgres, redis)
2. Build all Docker images (Maven builds inside the container)
3. Start one instance of each service on **blue**
4. Wait for health checks
5. Generate nginx config and reload

The first build downloads Maven dependencies and takes a few minutes. Subsequent builds use Docker layer cache.

### 3. Verify

```bash
./deploy.sh status
```

Test login:

```bash
curl -s http://localhost:9090/api/auth/login \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Open http://localhost:9090 in a browser for the login page.

### Default users

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER |

## deploy.sh Reference

```
./deploy.sh <command> [options]
```

### Commands

| Command | Description |
|---------|-------------|
| `blue` | Build and deploy services on the **blue** slot |
| `green` | Build and deploy services on the **green** slot |
| `rollback` | Switch nginx back to the previous color. No rebuild, takes seconds. Supports `--service`. |
| `stop` | Stop all service containers and infrastructure. Data volumes are preserved. |
| `status` | Show running containers and current nginx upstreams |

### Options (for `blue` / `green` / `rollback`)

| Option | Default | Description |
|--------|---------|-------------|
| `--instances N` | 1 | Number of instances per service, 1-3 (deploy only) |
| `--service NAME` | all | Target a specific service: `auth`, `user`, or `accounting` |

### Examples

```bash
# First deploy — start everything on blue
./deploy.sh blue

# Update code, deploy to green (zero-downtime switch)
./deploy.sh green

# Green is broken — instant rollback to blue (no rebuild)
./deploy.sh rollback

# Rollback only auth-service, keep other services on green
./deploy.sh rollback --service auth

# Scale auth-service to 3 instances on blue
./deploy.sh blue --instances 3 --service auth

# Deploy all services with 2 instances each on green
./deploy.sh green --instances 2

# Rebuild and deploy only user-service to green
./deploy.sh green --service user

# See what's running
./deploy.sh status

# Stop everything
./deploy.sh stop
```

### How blue-green works

1. **Deploy green** — builds new images, starts green containers alongside running blue ones, waits for health checks, switches nginx to green.
2. **Blue stays running** — the old blue containers are kept alive but receive no traffic. This enables instant rollback.
3. **Rollback** — `./deploy.sh rollback` points nginx back to blue and stops green. No rebuild needed, takes seconds.
4. **Next deploy** — deploy to blue again with updated code, and the cycle continues.

When redeploying the **same** color (e.g. blue when blue is already active), existing containers are renamed and kept serving traffic until the new ones pass health checks — no downtime.

## API Endpoints

All endpoints go through nginx on port **9090**.

### Auth (no token required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Login, returns access token + refresh token |
| POST | `/api/auth/refresh` | Refresh access token using refresh token |
| POST | `/api/auth/logout` | Invalidate refresh token |

### Users (token required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/contacts/{userId}` | Get user contacts |

### Accounts (token required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/accounts/balances` | List all balances |
| GET | `/api/accounts/balance/{userId}` | Get balance by user ID |

Protected endpoints require `Authorization: Bearer <token>` header. Get the token from `/api/auth/login`.

### Example: full flow

```bash
# Login
TOKEN=$(curl -s http://localhost:9090/api/auth/login \
  -X POST -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')

# Get users
curl -s http://localhost:9090/api/users/ \
  -H "Authorization: Bearer $TOKEN"

# Get balances
curl -s http://localhost:9090/api/accounts/balances \
  -H "Authorization: Bearer $TOKEN"
```

## Local Development (without Docker)

For running services directly on the host (useful for debugging).

**Requires:** Java 25, Maven 3.9+ (or use the included `./mvnw` wrapper)

```bash
# Start infrastructure only
docker compose up -d postgres redis nginx

# Build all modules
./mvnw clean install -DskipTests

# Run each service in separate terminals
cd auth-service && ../mvnw spring-boot:run        # port 7381
cd user-service && ../mvnw spring-boot:run         # port 7382
cd accounting-service && ../mvnw spring-boot:run   # port 7384
```

Update `nginx/nginx.conf` upstream servers to `host.docker.internal:<port>` instead of container names, then reload:

```bash
docker exec nging-gateway nginx -s reload
```

## Project Structure

```
nging/
├── common/                    # Shared library (JwtUtil)
├── auth-service/              # Authentication + token management
├── user-service/              # User profiles, contacts
├── accounting-service/        # Account balances
├── nginx/
│   ├── nginx.conf             # Generated by deploy.sh (do not edit)
│   └── nginx.conf.template    # Template with upstream placeholders
├── static/                    # Frontend (login.html, dashboard.html)
├── docker-compose.yml         # Infrastructure (nginx, postgres, redis)
├── deploy.sh                  # Blue-green deployment script
└── init-db.sql                # PostgreSQL schema (user_session table)
```

## Infrastructure

| Service | Container | Port | Purpose |
|---------|-----------|------|---------|
| Nginx | nging-gateway | 9090 | API gateway + static files |
| PostgreSQL | nging-postgres | 5432 | Refresh token storage |
| Redis | nging-redis | 6379 | Access token cache |
