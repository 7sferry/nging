# Getting Started

## Prerequisites

- Java 25 (JetBrains JDK via IntelliJ)
- Docker and Docker Compose
- IntelliJ IDEA

## Setup

### 1. Start infrastructure

```bash
cd /home/ferry/IdeaProjects/nging
docker compose up -d
```

This starts three containers:
- **nginx** — gateway on port 9090, serves static UI
- **PostgreSQL** — database on port 5432 (db: `nging`, user: `nging`, pass: `12345`)
- **Redis** — cache on port 6379 (pass: `12345`)

### 2. Load the Maven project in IntelliJ

Open `pom.xml` as a Maven project. IntelliJ will detect the multi-module structure: `common`, `auth-service`, `user-service`, `accounting-service`.

### 3. Start the services

Run each application's `main()` from IntelliJ:

| Application            | Class                    | Port |
|------------------------|--------------------------|------|
| Auth Service           | `AuthApplication`        | 7381 |
| User Service           | `UserApplication`        | 7382 |
| Accounting Service     | `AccountingApplication`  | 7384 |

Start order does not matter. User-service shows `"unavailable"` for balances if accounting-service isn't running yet.

On first startup, Hibernate auto-creates the `user_session` table in PostgreSQL.

### 4. Open the app

Go to `http://localhost:9090` in a browser. You'll be served `login.html`.

Login with:
- Username: `admin`, Password: `admin123`
- Username: `user`, Password: `user123`

After login you'll see the dashboard with all users and their account balances.

## Testing with curl

### Login

```bash
curl -s -c cookies.txt -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

The `-c cookies.txt` flag saves the `refresh_token` cookie to a file.

### Get all users

```bash
TOKEN="<paste access_token from login response>"

curl -s http://localhost:9090/api/users/ \
  -H "Authorization: Bearer $TOKEN"
```

### Get single user

```bash
curl -s http://localhost:9090/api/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Get all balances

```bash
curl -s http://localhost:9090/api/accounts/balances \
  -H "Authorization: Bearer $TOKEN"
```

### Refresh access token

```bash
curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:9090/api/auth/refresh
```

The `-b cookies.txt` sends the saved refresh cookie. `-c cookies.txt` saves the new one if rotated.

### Logout

```bash
curl -s -b cookies.txt -X POST http://localhost:9090/api/auth/logout
```

### Test without token (should return 401)

```bash
curl -s -w "\n%{http_code}" http://localhost:9090/api/users/
```

## Troubleshooting

### nginx returns 502 Bad Gateway

One or more services aren't running. Check which ports are listening:

```bash
ss -tlnp | grep -E '738[124]'
```

### nginx returns 401 on all requests

The auth-service might not be running on port 7381. The `auth_request` subrequest fails, so nginx rejects everything.

### "unavailable" balance in user responses

The accounting-service isn't running on port 7384. User-service catches the connection error and returns `"unavailable"`.

### auth-service fails to start — PostgreSQL connection refused

PostgreSQL container might not be ready yet. Wait a few seconds and try again, or check:

```bash
docker logs nging-postgres
```

### auth-service fails to start — Redis connection refused

Redis container might not be ready:

```bash
docker logs nging-redis
```

### Docker can't reach host services

The `extra_hosts: "host.docker.internal:host-gateway"` mapping might not work on all Linux setups. Verify:

```bash
docker exec nging-gateway ping -c1 host.docker.internal
```

If it fails, use the host's Docker bridge IP instead (usually `172.17.0.1`).

### Reset everything

```bash
docker compose down -v   # stops containers and deletes volumes (database + redis data)
docker compose up -d     # fresh start
```
