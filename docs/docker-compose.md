# Docker Compose Configuration

File: `docker-compose.yml`

## Full Configuration

```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: nging-gateway
    ports:
      - "9090:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./static:/usr/share/nginx/html:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"

  postgres:
    image: postgres:17-alpine
    container_name: nging-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: nging
      POSTGRES_USER: nging
      POSTGRES_PASSWORD: 12345
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: nging-redis
    ports:
      - "6379:6379"
    command: redis-server --requirepass 12345
    volumes:
      - redisdata:/data

volumes:
  pgdata:
  redisdata:
```

## Service: nginx

### `image: nginx:alpine`

Official nginx image based on Alpine Linux (~25MB vs ~140MB for Debian-based).

### `ports: "9090:80"`

Maps host port 9090 to container port 80. Access the app at `http://localhost:9090`.

### `volumes`

- `./nginx/nginx.conf:/etc/nginx/nginx.conf:ro` — mounts the gateway config. `:ro` means read-only.
- `./static:/usr/share/nginx/html:ro` — mounts the static frontend files (HTML, JS, CSS). Nginx serves these directly for the `/` location. Changes to files on the host are reflected immediately on refresh.

### `extra_hosts: "host.docker.internal:host-gateway"`

Adds a DNS entry inside the container mapping `host.docker.internal` to the host machine's IP. On Linux, this does not exist by default — `host-gateway` resolves to the host's gateway IP (typically `172.17.0.1`). This is how nginx in Docker reaches the Spring Boot services running on the host.

## Service: postgres

### `image: postgres:17-alpine`

PostgreSQL 17 on Alpine Linux. Used by auth-service to store refresh token sessions.

### `ports: "5432:5432"`

Exposes PostgreSQL on the standard port so Spring Boot apps on the host can connect via `localhost:5432`.

### `environment`

- `POSTGRES_DB: nging` — creates a database named `nging` on first startup.
- `POSTGRES_USER: nging` — creates a user named `nging`.
- `POSTGRES_PASSWORD: 12345` — sets the password.

### `volumes: pgdata:/var/lib/postgresql/data`

Named volume for data persistence. The database survives container restarts and `docker compose down`. To fully reset, run `docker volume rm nging_pgdata`.

## Service: redis

### `image: redis:7-alpine`

Redis 7 on Alpine Linux. Used by auth-service to cache access tokens.

### `ports: "6379:6379"`

Exposes Redis on the standard port so Spring Boot apps on the host can connect via `localhost:6379`.

### `command: redis-server --requirepass 12345`

Overrides the default command to enable password authentication. Without this, Redis accepts unauthenticated connections.

### `volumes: redisdata:/data`

Named volume for Redis persistence (RDB snapshots). Optional for this use case since cached access tokens are short-lived (1 hour TTL), but prevents data loss on restart.

## Named Volumes

```yaml
volumes:
  pgdata:
  redisdata:
```

Docker-managed volumes stored at `/var/lib/docker/volumes/`. Listed explicitly so Docker Compose creates them and they persist across `docker compose down`.

## Commands

```bash
# Start everything
docker compose up -d

# View logs
docker compose logs -f
docker logs nging-gateway
docker logs nging-postgres
docker logs nging-redis

# Restart nginx after config change
docker compose restart nginx

# Connect to PostgreSQL
psql -h localhost -U nging -d nging

# Connect to Redis
redis-cli -h localhost -a 12345

# Stop everything
docker compose down

# Stop and delete all data
docker compose down -v
```
