# Deployment

## How It Works

The system is split into two layers managed by different tools:

| Layer | Managed by | What |
|-------|-----------|------|
| **Infrastructure** | `docker-compose.yml` | nginx, postgres, redis |
| **Services** | `deploy.sh` | auth-service, user-service, accounting-service |

Services are **not** defined in docker-compose. Instead, `deploy.sh` creates service containers directly with `docker run`, giving it full control over naming, blue-green slots, scaling, and rolling replacement.

```
./deploy.sh blue
    │
    │   docker-compose.yml                    deploy.sh (docker run)
    │   ┌──────────────────┐                  ┌──────────────────────┐
    ├──►│  nging-gateway   │ (nginx)          │  auth-blue-1         │
    │   │  nging-postgres  │ (database)   ┌──►│  auth-blue-2         │
    │   │  nging-redis     │ (cache)      │   │  user-blue-1         │
    │   └──────────────────┘              │   │  accounting-blue-1   │
    │                                     │   └──────────────────────┘
    └─────────────────────────────────────┘
```

All containers share the `nging-app` Docker network so they can reach each other by container name.

## Deployment Flow

When you run `./deploy.sh <color>`, the script executes these steps in order:

```
1. Start infrastructure
   docker compose up -d nginx postgres redis

2. Build Docker images (multi-stage: Maven builds inside the container)
   docker build -t nging-auth-service:latest ...
   docker build -t nging-user-service:latest ...
   docker build -t nging-accounting-service:latest ...

3. Start service containers
   docker run --name auth-blue-1 --network nging-app ...
   docker run --name user-blue-1 --network nging-app ...
   docker run --name accounting-blue-1 --network nging-app ...

4. Wait for health checks (GET /actuator/health on each container)

5. Generate nginx.conf from template with container names as upstreams

6. Reload nginx (nginx -s reload)

7. Stop old containers (renamed same-color, excess from scale-down)

8. Keep previous color's containers running (for rollback)
```

## Container Naming

Containers follow the pattern `{service}-{color}-{instance}`:

```
auth-blue-1          # auth service, blue slot, instance 1
user-green-2         # user service, green slot, instance 2
accounting-blue-3    # accounting service, blue slot, instance 3
```

During same-color redeployment, existing containers are temporarily renamed with an `-old` suffix (e.g. `auth-blue-1-old`) so they keep serving traffic until the replacement is healthy.

## Blue-Green Deployment

Blue and green are two independent slots. Only one slot per service is active in nginx at a time.

### Normal flow (different colors)

```
State: auth-blue-1 is running and active in nginx

./deploy.sh green --service auth

1. Build new image
2. Start auth-green-1
3. Wait for health check
4. Switch nginx: auth-blue-1 → auth-green-1
5. Keep auth-blue-1 running (for rollback)

State: auth-green-1 is active, auth-blue-1 is idle
```

Both containers are running, but only green receives traffic. This enables instant rollback.

### Same-color redeployment (rolling update)

```
State: auth-blue-1 is running and active in nginx

./deploy.sh blue --service auth

1. Build new image
2. Rename auth-blue-1 → auth-blue-1-old (keeps serving)
3. Start new auth-blue-1
4. Wait for health check
5. Switch nginx to new auth-blue-1
6. Stop auth-blue-1-old

State: new auth-blue-1 is active
```

The old container serves traffic during the transition. No downtime, but no rollback target (the old container is gone).

## Rollback

Rollback switches nginx back to the previous color's containers. No rebuild, no restart — just a config reload.

### Prerequisites

Rollback only works when the previous color's containers are still running. This is the case after a cross-color deployment (blue → green or green → blue). It does **not** work after a same-color redeployment because the old containers are replaced.

### How it works

```
State: auth-green-1 is active, auth-blue-1 is idle

./deploy.sh rollback --service auth

1. Detect auth is currently on green (from nginx.conf)
2. Check auth-blue containers exist
3. Stop auth-green-1
4. Regenerate nginx.conf with auth-blue-1
5. Reload nginx

State: auth-blue-1 is active again
```

### Per-service rollback

Each service can be on a different color. Rollback targets a specific service or all:

```
# Rollback everything
./deploy.sh rollback

# Rollback only auth (user and accounting stay on their current color)
./deploy.sh rollback --service auth
```

## Scaling

Services can run 1 to 3 instances. Nginx load-balances across all instances of a service using round-robin.

### Scale up

```
./deploy.sh blue --instances 3 --service auth
```

Creates `auth-blue-1`, `auth-blue-2`, `auth-blue-3`. Nginx config:

```
upstream auth_service {
    server auth-blue-1:8080 max_fails=1 fail_timeout=5s;
    server auth-blue-2:8080 max_fails=1 fail_timeout=5s;
    server auth-blue-3:8080 max_fails=1 fail_timeout=5s;
}
```

### Scale down

```
./deploy.sh blue --instances 1 --service auth
```

Starts new `auth-blue-1`, then stops `auth-blue-2` and `auth-blue-3`. Nginx is updated to only include instance 1.

## Nginx Config Generation

The script generates `nginx/nginx.conf` from `nginx/nginx.conf.template` by replacing placeholders with running container names:

**Template** (`nginx.conf.template`):
```
upstream auth_service {
{{AUTH_SERVERS}}
}
```

**Generated** (`nginx.conf`):
```
upstream auth_service {
    server auth-blue-1:8080 max_fails=1 fail_timeout=5s;
    server auth-blue-2:8080 max_fails=1 fail_timeout=5s;
}
```

When deploying a single service, the generator:
- Filters the **deployed service** to only include the target color
- Keeps **other services** on whatever color they're currently running

This means you can have auth on green and user on blue at the same time.

## Health Checks

Each Dockerfile defines a health check:

```dockerfile
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
```

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `interval` | 10s | Time between checks |
| `timeout` | 5s | Max time for a single check |
| `start-period` | 30s | Grace period for Spring Boot startup (failures don't count) |
| `retries` | 5 | Consecutive failures before marking unhealthy |

The deploy script polls `docker inspect` every 2 seconds for up to 120 seconds, waiting for status `healthy`. If any container fails, the deployment aborts and the old containers continue serving.

## Logging

Each service writes logs to both console and a rolling file:

| Setting | Value |
|---------|-------|
| Log file | `/app/logs/{service-name}.log` |
| Max file size | 10MB (rolls to new file) |
| Max history | 7 days |
| Total size cap | 100MB |
| Pattern | `timestamp [thread] level logger - message` |

Log files are mounted to the host at `logs/{container-name}/`:

```
logs/
├── auth-blue-1/auth-service.log
├── auth-blue-2/auth-service.log
├── user-blue-1/user-service.log
└── accounting-blue-1/accounting-service.log
```

Logs persist across redeploys because they're on the host filesystem, not inside the container.

To view logs:
```bash
# Docker stdout/stderr (console output)
docker logs auth-blue-1
docker logs auth-blue-1 -f        # follow

# Log files on host
cat logs/auth-blue-1/auth-service.log
tail -f logs/auth-blue-1/auth-service.log
```

## Docker Images

Each service uses a multi-stage Dockerfile:

```
Stage 1: eclipse-temurin:25-jdk-alpine
  - Copies source code and Maven wrapper
  - Runs Maven build inside the container
  - Produces a fat JAR

Stage 2: eclipse-temurin:25-jre-alpine
  - Copies only the JAR from stage 1
  - Runs on port 8080
  - ~150MB final image (JRE only, no build tools)
```

No Java or Maven installation needed on the host. The first build is slow (downloads dependencies), subsequent builds use Docker layer cache.

## Network

All containers join the `nging-app` Docker bridge network. Services reach each other by container name via Docker's embedded DNS:

| From | To | Address |
|------|----|---------|
| nginx | auth-service | `auth-blue-1:8080` |
| nginx | user-service | `user-blue-1:8080` |
| nginx | accounting-service | `accounting-blue-1:8080` |
| user-service | accounting-service | `accounting:8080` (network alias) |
| auth-service | postgres | `nging-postgres:5432` |
| auth-service | redis | `nging-redis:6379` |

The accounting-service containers get a `--network-alias accounting` so user-service can always reach them at `accounting:8080` regardless of color or instance number.

## Environment Variables

Service-specific config is passed via `-e` flags on `docker run`:

### auth-service
| Variable | Value |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://nging-postgres:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `12345` |
| `SPRING_DATA_REDIS_HOST` | `nging-redis` |
| `SPRING_DATA_REDIS_PASSWORD` | `12345` |

### user-service
| Variable | Value |
|----------|-------|
| `SERVICES_ACCOUNTING_URL` | `http://accounting:8080` |

### accounting-service
No additional environment variables. Uses defaults from `application.yaml`.

## Command Reference

```bash
# Deploy all services on blue
./deploy.sh blue

# Deploy all services on green with 2 instances each
./deploy.sh green --instances 2

# Deploy only auth-service on green
./deploy.sh green --service auth

# Scale auth-service to 3 instances
./deploy.sh blue --instances 3 --service auth

# Scale back down to 1 instance
./deploy.sh blue --instances 1 --service auth

# Rollback all services
./deploy.sh rollback

# Rollback only auth-service
./deploy.sh rollback --service auth

# Show running containers and nginx upstreams
./deploy.sh status

# Stop everything (data preserved)
./deploy.sh stop
```

## Lifecycle Summary

```
First deploy                    ./deploy.sh blue
  ├── auth-blue-1, user-blue-1, accounting-blue-1 running
  └── nginx routes to blue

Update code, deploy green       ./deploy.sh green
  ├── auth-green-1, user-green-1, accounting-green-1 started
  ├── nginx switches to green
  └── blue containers kept idle (rollback ready)

Green is broken, rollback       ./deploy.sh rollback
  ├── nginx switches back to blue
  └── green containers stopped

Fix code, deploy green again    ./deploy.sh green
  └── cycle continues...

Scale up auth                   ./deploy.sh green --instances 3 --service auth
  └── auth-green-1, auth-green-2, auth-green-3 running

Scale down auth                 ./deploy.sh green --instances 1 --service auth
  └── auth-green-2, auth-green-3 stopped

Done for the day                ./deploy.sh stop
  └── everything stopped, data volumes preserved
```
