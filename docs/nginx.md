# Nginx Gateway Configuration

File: `nginx/nginx.conf`

## Overview

Nginx acts as the single entry point (API gateway) for all client requests. It handles routing to backend services, centralized JWT authentication, and serving the static frontend.

## Configuration Breakdown

### events block

```nginx
events {
    worker_connections 1024;
}
```

- `worker_connections 1024` — maximum number of simultaneous connections each worker process can handle. 1024 is fine for development.

### http block — includes and defaults

```nginx
http {
    include       mime.types;
    default_type  application/octet-stream;
    ...
}
```

- `include mime.types` — loads the default MIME type mappings so nginx serves `.css` as `text/css`, `.js` as `application/javascript`, etc. Without this, static files may be served as `application/octet-stream` and the browser won't parse them.
- `default_type application/octet-stream` — fallback MIME type for unknown file extensions.

### upstream blocks

```nginx
upstream auth_service {
    server host.docker.internal:7381;
}
```

- `upstream` defines a named group of backend servers that nginx can proxy to.
- `host.docker.internal` resolves to the host machine's IP from inside the Docker container. This lets nginx (running in Docker) reach Spring Boot services running on the host.
- Three upstreams: `auth_service` (7381), `user_service` (7382), `accounting_service` (7384).
- In production, each upstream could list multiple servers for load balancing.

### server block

```nginx
server {
    listen 80;
    ...
}
```

- `listen 80` — nginx listens on port 80 inside the container. Docker maps host port 9090 to container port 80.

### Centralized Auth: /_validate

```nginx
location = /_validate {
    internal;
    proxy_pass http://auth_service/auth/validate;
    proxy_pass_request_body off;
    proxy_set_header Content-Length "";
    proxy_set_header Authorization $http_authorization;
}
```

This is the centralized authentication endpoint. Only nginx's own `auth_request` subrequests can trigger it.

- `location = /_validate` — exact match. The `=` means only this exact path matches. The underscore is a naming convention for internal routes, it has no special meaning.
- `internal` — rejects any direct external request with 404. Only triggered internally by `auth_request`.
- `proxy_pass http://auth_service/auth/validate` — forwards to auth-service's validation endpoint.
- `proxy_pass_request_body off` — strips the body. Validation only needs the Authorization header.
- `proxy_set_header Content-Length ""` — clears Content-Length since body was stripped.
- `proxy_set_header Authorization $http_authorization` — forwards the original Bearer token.

### Auth Service Route: /api/auth/

```nginx
location /api/auth/ {
    proxy_pass http://auth_service/auth/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

- No `auth_request` — login, refresh, and logout are public (they establish/manage authentication, not consume it).
- `proxy_pass http://auth_service/auth/` — strips `/api` prefix. `/api/auth/login` becomes `/auth/login`.
- Cookies (including `refresh_token`) are passed through by default in the `Cookie` header.
- `proxy_set_header Host $host` — preserves the original Host header.
- `proxy_set_header X-Real-IP $remote_addr` — the direct client IP.
- `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for` — appends client IP to the proxy chain.
- `proxy_set_header X-Forwarded-Proto $scheme` — tells backend if original request was HTTP or HTTPS.

### Protected Routes: /api/users/ and /api/accounts/

```nginx
location /api/users/ {
    auth_request /_validate;
    auth_request_set $auth_user $upstream_http_x_auth_user;
    auth_request_set $auth_client_id $upstream_http_x_auth_client_id;
    auth_request_set $auth_roles $upstream_http_x_auth_roles;
    auth_request_set $auth_work_entities $upstream_http_x_auth_work_entities;

    proxy_pass http://user_service/users/;
    proxy_set_header X-Auth-User $auth_user;
    proxy_set_header X-Auth-Client-Id $auth_client_id;
    proxy_set_header X-Auth-Roles $auth_roles;
    proxy_set_header X-Auth-Work-Entities $auth_work_entities;
    proxy_set_header Authorization $http_authorization;
    ...
}
```

- `auth_request /_validate` — before proxying, nginx makes a subrequest to `/_validate`. If 200, proceed. If 401/403, return that to the client.
- `auth_request_set` — captures response headers from the validation subrequest into nginx variables. The `$upstream_http_` prefix reads headers from the upstream response.
- `proxy_set_header X-Auth-*` — forwards the captured auth context as request headers to the backend service.
- Four headers forwarded: `X-Auth-User`, `X-Auth-Client-Id`, `X-Auth-Roles`, `X-Auth-Work-Entities`.

The `/api/accounts/` block follows the same pattern.

### Static UI: /

```nginx
location / {
    root /usr/share/nginx/html;
    index login.html;
    try_files $uri $uri/ /login.html;
}
```

- No `auth_request` — the UI is public. Authentication happens when JavaScript calls API endpoints.
- `root /usr/share/nginx/html` — serves files from this directory. Maps to the `static/` directory on the host via Docker volume mount.
- `index login.html` — when visiting `/`, serve `login.html`.
- `try_files $uri $uri/ /login.html` — try the requested path as a file, then as a directory, then fall back to `login.html`. This handles SPA-style routing.
- The `include mime.types` in the http block ensures CSS and JS files are served with correct Content-Type headers.

### Location Matching Order

Nginx evaluates locations in this priority:

1. `= /_validate` — exact match, highest priority
2. `/api/auth/` — prefix match (longer prefix wins)
3. `/api/users/` — prefix match
4. `/api/accounts/` — prefix match
5. `/` — catch-all for static files, lowest priority

## Rate Limiting

Rate limiting is applied at the nginx level to protect backend services from excessive requests.

### Configuration

Two rate limit zones are defined in the `http` block:

```nginx
limit_req_zone $binary_remote_addr zone=api_auth:10m rate=5r/s;
limit_req_zone $binary_remote_addr zone=api_general:10m rate=20r/s;
limit_req_status 429;
```

- `$binary_remote_addr` — tracks each client by IP address (4 bytes per IPv4, saves memory vs string form).
- `zone=api_auth:10m` — allocates 10MB shared memory for tracking state. 10MB holds ~160,000 unique IPs.
- `rate=5r/s` — base rate of 5 requests per second (internally: 1 request per 200ms).
- `limit_req_status 429` — return HTTP 429 (Too Many Requests) instead of the default 503.

### Zones and where they apply

| Zone | Rate | Burst | Applied to |
|------|------|-------|------------|
| `api_auth` | 5r/s | 10 | `/api/auth/` (login, register, token) |
| `api_general` | 20r/s | 40 | `/api/users/`, `/api/contacts/`, `/api/accounts/` |

Auth endpoints have a stricter limit because they are the primary target for brute force attacks. 5 requests per second is sufficient for normal login usage but makes automated password guessing impractical.

The `/_validate` internal subrequest is **not** rate limited — it is triggered by nginx itself, not directly by clients.

### How rate + burst works

Think of it as a parking lot:

- **rate** = number of spots that free up per second (the refill speed)
- **burst** = number of overflow spots (extra capacity for short spikes)

Example with `rate=5r/s burst=3 nodelay`:

```
Capacity at any moment: 4 (1 base + 3 burst)

Client sends 6 requests at t=0:
  Request 1 → processed (base slot)
  Request 2 → processed (burst slot 1)
  Request 3 → processed (burst slot 2)
  Request 4 → processed (burst slot 3)
  Request 5 → 429 rejected (no slots left)
  Request 6 → 429 rejected

Slots refill at base rate (1 per 200ms):
  t=200ms  → 1 slot available
  t=400ms  → 2 slots available
  t=600ms  → 3 slots available
  t=800ms  → all 4 slots available
```

The `nodelay` flag means burst requests are processed immediately rather than queued. Without it, request 2 would wait 200ms, request 3 would wait 400ms, etc.

### 429 error response

A custom error page returns a JSON body:

```nginx
error_page 429 = @rate_limited;
location @rate_limited {
    default_type application/json;
    return 429 '{"error": "Too many requests. Please try again later."}';
}
```

Without the custom error page, nginx would return an HTML response which is not useful for API clients.

### Current limits summary

| Endpoint | Max instant burst | Sustained rate | Per |
|----------|-------------------|----------------|-----|
| `/api/auth/*` | 11 requests | 5 req/s | IP |
| `/api/users/*` | 41 requests | 20 req/s | IP |
| `/api/contacts/*` | 41 requests | 20 req/s | IP |
| `/api/accounts/*` | 41 requests | 20 req/s | IP |
| Static files (`/`) | No limit | — | — |

Max instant burst = 1 (base) + burst value.

### Tuning

To adjust limits, modify the values in `deploy.sh` inside the `generate_nginx_conf()` function and in `nginx/nginx.conf.template`:

- **Lower `rate`** → stricter sustained limit, slower slot refill
- **Higher `burst`** → more tolerance for traffic spikes
- **Remove `nodelay`** → burst requests are queued instead of instant (adds latency but smooths traffic)

After changing, redeploy to regenerate nginx.conf:

```bash
./deploy.sh blue   # or whatever color is active
```

## Reloading Configuration

Since `nginx.conf` is bind-mounted via a Docker volume, file changes on the host are visible inside the container immediately. However, **nginx does not watch the file** — it only reads the config on startup or when explicitly told to reload.

### Apply changes (zero downtime)

```bash
docker exec nging-gateway nginx -s reload
```

What happens internally:

1. Nginx master process reads the new config.
2. If valid, it starts new worker processes with the new config.
3. Old workers finish handling their current in-flight requests.
4. Old workers shut down gracefully.
5. New workers take over.

No connections are dropped. This is one of nginx's key design features.

### Validate config before reloading

```bash
docker exec nging-gateway nginx -t
```

If valid:
```
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

If invalid:
```
nginx: [emerg] unknown directive "typo" in /etc/nginx/nginx.conf:5
nginx: configuration file /etc/nginx/nginx.conf test is failed
```

If you run `nginx -s reload` with a bad config, nginx **keeps the old config running** and logs the error. No downtime, no crash.

### Avoid `docker compose restart`

```bash
# DON'T do this — causes downtime (stops then starts the container)
docker compose restart nginx

# DO this — zero downtime reload
docker exec nging-gateway nginx -s reload
```
