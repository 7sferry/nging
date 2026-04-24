#!/bin/bash
set -e

# ──────────────────────────────────────────────────────────────
# Blue-Green Deploy Script (fully containerized)
#
# Usage:
#   ./deploy.sh <color> [--instances N] [--service NAME]
#
# Examples:
#   ./deploy.sh blue                      # First deploy: build + start all on blue
#   ./deploy.sh green                     # Zero-downtime switch all to green
#   ./deploy.sh blue --instances 3        # Switch to blue with 3 instances each
#   ./deploy.sh green --service auth      # Switch only auth-service to green
#   ./deploy.sh rollback                  # Rollback all services to previous color (no rebuild)
#   ./deploy.sh rollback --service auth   # Rollback only auth-service
#   ./deploy.sh stop                      # Stop all containers and infrastructure
#   ./deploy.sh status                    # Show what's running
# ──────────────────────────────────────────────────────────────

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
NETWORK="nging-app"
SERVICES=("auth" "user" "accounting")
DOCKER_IMAGES=("auth-service" "user-service" "accounting-service")

# Defaults
INSTANCES=1
TARGET_SERVICE="all"

# ─── Parse args ───
ACTION=${1:?Usage: $0 <blue|green|status> [--instances N] [--service NAME]}
shift

while [[ $# -gt 0 ]]; do
    case $1 in
        --instances) INSTANCES=$2; shift 2 ;;
        --service)   TARGET_SERVICE=$2; shift 2 ;;
        *)           echo "Unknown arg: $1"; exit 1 ;;
    esac
done

if [[ $INSTANCES -lt 1 || $INSTANCES -gt 3 ]]; then
    echo "Error: instances must be 1-3"
    exit 1
fi

# ─── Status ───
if [[ "$ACTION" == "status" ]]; then
    echo "=== Running containers ==="
    docker ps --filter "network=$NETWORK" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" || echo "  (none)"
    echo ""
    echo "=== Nginx upstreams ==="
    grep "server " "$PROJECT_DIR/nginx/nginx.conf" | grep -v "listen\|proxy_pass\|proxy_set" | sed 's/^/  /'
    exit 0
fi

# ─── Stop all ───
if [[ "$ACTION" == "stop" ]]; then
    echo "=== Stopping all service containers ==="
    for container in $(docker ps --filter "network=$NETWORK" --format "{{.Names}}" | grep -E "^(auth|user|accounting)-"); do
        echo "  Stopping $container..."
        docker stop --time 30 "$container" > /dev/null && docker rm "$container" > /dev/null
        echo "    Stopped $container"
    done
    echo ""

    echo "=== Stopping infrastructure ==="
    cd "$PROJECT_DIR"
    docker compose down
    echo ""

    echo "=== All stopped (data volumes preserved) ==="
    exit 0
fi

# ─── Generate nginx config ───
generate_nginx_conf() {
    local active_color=$1   # color to use for the deployed service(s)
    local target_svc=$2     # "all" or specific service name (e.g. "auth")
    local output="$PROJECT_DIR/nginx/nginx.conf"

    local auth_servers="" user_servers="" accounting_servers=""

    # Collect running containers for each service
    # For the deployed service: only include active_color
    # For other services: include any color (they weren't redeployed)
    # Always exclude -old containers being drained
    for svc in auth user accounting; do
        local servers=""
        for container in $(docker ps --filter "network=$NETWORK" --filter "name=${svc}-" --format "{{.Names}}" | grep -v -- '-old$' | sort); do
            if [[ -n "$active_color" ]]; then
                if [[ "$target_svc" == "all" || "$target_svc" == "$svc" ]]; then
                    if [[ ! "$container" =~ -${active_color}- ]]; then
                        continue
                    fi
                fi
            fi
            servers+="        server ${container}:8080 max_fails=1 fail_timeout=5s;"$'\n'
        done

        case $svc in
            auth)       auth_servers="$servers" ;;
            user)       user_servers="$servers" ;;
            accounting) accounting_servers="$servers" ;;
        esac
    done

    cat > "$output" <<NGINX_EOF
events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    # Rate limiting zones (per client IP)
    limit_req_zone \$binary_remote_addr zone=api_general:10m rate=20r/s;
    limit_req_zone \$binary_remote_addr zone=api_auth:10m rate=5r/s;

    # Return JSON on rate limit (429)
    limit_req_status 429;

    map \$uri \$no_cache {
        ~^/api/  "no-store";
        default  "";
    }

    upstream auth_service {
${auth_servers}    }

    upstream user_service {
${user_servers}    }

    upstream accounting_service {
${accounting_servers}    }

    server {
        listen 9090;

        add_header Cache-Control \$no_cache always;

        # Custom 429 error response
        error_page 429 = @rate_limited;
        location @rate_limited {
            default_type application/json;
            return 429 '{"error": "Too many requests. Please try again later."}';
        }

        location = /_validate {
            internal;
            proxy_pass http://auth_service/auth/validate;
            proxy_http_version 1.1;
            proxy_method GET;
            proxy_pass_request_body off;
            proxy_set_header Content-Length "0";
            proxy_set_header Host \$host;
            proxy_set_header Connection "";
            proxy_set_header Transfer-Encoding "";
            proxy_set_header Authorization \$http_authorization;
        }

        location /api/auth/ {
            limit_req zone=api_auth burst=10 nodelay;

            proxy_pass http://auth_service/auth/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
        }

        location /api/users/ {
            limit_req zone=api_general burst=40 nodelay;

            auth_request /_validate;
            auth_request_set \$auth_user \$upstream_http_x_auth_user;
            auth_request_set \$auth_client_id \$upstream_http_x_auth_client_id;
            auth_request_set \$auth_roles \$upstream_http_x_auth_roles;
            auth_request_set \$auth_work_entities \$upstream_http_x_auth_work_entities;

            proxy_pass http://user_service/users/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
            proxy_set_header X-Auth-User \$auth_user;
            proxy_set_header X-Auth-Client-Id \$auth_client_id;
            proxy_set_header X-Auth-Roles \$auth_roles;
            proxy_set_header X-Auth-Work-Entities \$auth_work_entities;
            proxy_set_header Authorization \$http_authorization;
        }

        location /api/contacts/ {
            limit_req zone=api_general burst=40 nodelay;

            auth_request /_validate;
            auth_request_set \$auth_user \$upstream_http_x_auth_user;

            proxy_pass http://user_service/contacts/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
            proxy_set_header Authorization \$http_authorization;
        }

        location /api/accounts/ {
            limit_req zone=api_general burst=40 nodelay;

            auth_request /_validate;
            auth_request_set \$auth_user \$upstream_http_x_auth_user;
            auth_request_set \$auth_client_id \$upstream_http_x_auth_client_id;
            auth_request_set \$auth_roles \$upstream_http_x_auth_roles;
            auth_request_set \$auth_work_entities \$upstream_http_x_auth_work_entities;

            proxy_pass http://accounting_service/accounts/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
            proxy_set_header X-Auth-User \$auth_user;
            proxy_set_header X-Auth-Client-Id \$auth_client_id;
            proxy_set_header X-Auth-Roles \$auth_roles;
            proxy_set_header X-Auth-Work-Entities \$auth_work_entities;
            proxy_set_header Authorization \$http_authorization;
        }

        location /api/ {
            return 404 '{"error": "Not found"}';
            add_header Content-Type application/json always;
        }

        location ~* \.html\$ {
            root /usr/share/nginx/html;
            add_header Cache-Control "no-cache" always;
        }

        location ~* \.(css|js)\$ {
            root /usr/share/nginx/html;
            add_header Cache-Control "public, max-age=3600, must-revalidate" always;
        }

        location / {
            root /usr/share/nginx/html;
            index login.html;
            try_files \$uri \$uri/ /login.html;
        }
    }
}
NGINX_EOF

    echo "  Generated nginx.conf with $active_color containers"
}

# ─── Rollback ───
if [[ "$ACTION" == "rollback" ]]; then
    ROLLBACK_SVCS=("${SERVICES[@]}")
    if [[ "$TARGET_SERVICE" != "all" ]]; then
        ROLLBACK_SVCS=("${TARGET_SERVICE%-service}")
    fi

    echo "=== Rollback: ${ROLLBACK_SVCS[*]} ==="

    # For each service being rolled back, detect current color from nginx config
    for svc in "${ROLLBACK_SVCS[@]}"; do
        # Find which color is currently in nginx for this service
        svc_current=""
        if grep -q "${svc}-blue-" "$PROJECT_DIR/nginx/nginx.conf" 2>/dev/null; then
            svc_current="blue"
        fi
        if grep -q "${svc}-green-" "$PROJECT_DIR/nginx/nginx.conf" 2>/dev/null; then
            svc_current="green"
        fi

        svc_rollback_to=$([[ "$svc_current" == "blue" ]] && echo "green" || echo "blue")

        # Check if rollback target has running containers
        svc_rollback_count=$(docker ps --filter "network=$NETWORK" --filter "name=${svc}-${svc_rollback_to}-" --format "{{.Names}}" | grep -cv -- '-old$' 2>/dev/null || echo 0)

        if [[ "$svc_rollback_count" -eq 0 ]]; then
            echo "ERROR: No ${svc}-${svc_rollback_to} containers running to roll back to."
            echo "  ${svc} is currently on $svc_current, no $svc_rollback_to containers available."
            echo "  You may need to redeploy: ./deploy.sh $svc_rollback_to --service $svc"
            exit 1
        fi

        echo "  $svc: $svc_current -> $svc_rollback_to ($svc_rollback_count containers)"

        # Stop current color containers for this service
        for container in $(docker ps --filter "network=$NETWORK" --filter "name=${svc}-${svc_current}-" --format "{{.Names}}" | grep -v -- '-old$'); do
            echo "  Stopping $container..."
            docker stop --time 30 "$container" > /dev/null && docker rm "$container" > /dev/null
            echo "    Stopped $container"
        done
    done
    echo ""

    # Regenerate nginx config from remaining running containers
    echo "=== Updating nginx ==="
    generate_nginx_conf "" "all"
    docker exec nging-gateway nginx -t && docker exec nging-gateway nginx -s reload
    echo "  Nginx reloaded"
    echo ""

    echo "=== Rollback complete ==="
    echo ""
    docker ps --filter "network=$NETWORK" --format "table {{.Names}}\t{{.Status}}" | sort
    exit 0
fi

if [[ "$ACTION" != "blue" && "$ACTION" != "green" ]]; then
    echo "Error: action must be 'blue', 'green', 'rollback', 'stop', or 'status'"
    exit 1
fi

OTHER=$([[ "$ACTION" == "blue" ]] && echo "green" || echo "blue")

# ─── Ensure nginx.conf exists ───
if [[ ! -f "$PROJECT_DIR/nginx/nginx.conf" ]]; then
    echo "=== Creating nginx.conf from template ==="
    cp "$PROJECT_DIR/nginx/nginx.conf.template" "$PROJECT_DIR/nginx/nginx.conf"
fi

# ─── Ensure infra is running ───
echo "=== Ensuring infrastructure ==="
cd "$PROJECT_DIR"
docker compose up -d nginx postgres redis
echo ""

# ─── Build Docker images (multi-stage: Maven build inside container) ───
build_image() {
    local svc=$1
    echo "  Building image: $svc (this includes Maven build)"
    docker build -q -t "nging-$svc:latest" -f "$PROJECT_DIR/$svc/Dockerfile" "$PROJECT_DIR" > /dev/null
    echo "    Done: nging-$svc:latest"
}

echo "=== Building Docker images ==="
if [[ "$TARGET_SERVICE" == "all" ]]; then
    for img in "${DOCKER_IMAGES[@]}"; do
        build_image "$img"
    done
else
    build_image "$TARGET_SERVICE-service"
fi
echo ""

# ─── Start new containers ───
start_containers() {
    local svc=$1       # auth, user, accounting
    local color=$2
    local count=$3
    local image="nging-${svc}-service:latest"
    local env_args=""

    # Service-specific env vars
    if [[ "$svc" == "auth" ]]; then
        env_args="-e SPRING_DATASOURCE_URL=jdbc:postgresql://nging-postgres:5432/postgres \
                  -e SPRING_DATASOURCE_USERNAME=postgres \
                  -e SPRING_DATASOURCE_PASSWORD=12345 \
                  -e SPRING_DATA_REDIS_HOST=nging-redis \
                  -e SPRING_DATA_REDIS_PASSWORD=12345"
    elif [[ "$svc" == "user" ]]; then
        env_args="-e SERVICES_ACCOUNTING_URL=http://accounting:8080"
    fi

    for i in $(seq 1 "$count"); do
        local name="${svc}-${color}-${i}"

        # If container is currently running, rename it so it keeps serving
        # traffic until the new container is healthy (zero-downtime)
        if docker ps -q --filter "name=^${name}$" 2>/dev/null | grep -q .; then
            echo "  Renaming running $name -> ${name}-old (keeps serving until new is healthy)"
            docker rename "$name" "${name}-old" 2>/dev/null || true
        fi

        # Remove stopped/non-running container with same name
        docker rm -f "$name" 2>/dev/null || true

        echo "  Starting $name"

        # Network alias: "accounting" so user-service can reach it by name
        local alias_arg=""
        if [[ "$svc" == "accounting" ]]; then
            alias_arg="--network-alias accounting"
        fi

        # Mount host log directory so logs persist across deploys
        local log_dir="$PROJECT_DIR/logs/${name}"
        mkdir -p "$log_dir"

        docker run -d \
            --name "$name" \
            --network "$NETWORK" $alias_arg \
            -v "$log_dir:/app/logs" \
            $env_args \
            --restart unless-stopped \
            "$image" > /dev/null

        echo "    Started $name"
    done
}

echo "=== Starting $ACTION containers (x$INSTANCES) ==="
if [[ "$TARGET_SERVICE" == "all" ]]; then
    for svc in "${SERVICES[@]}"; do
        start_containers "$svc" "$ACTION" "$INSTANCES"
    done
else
    local_svc="${TARGET_SERVICE%-service}"
    start_containers "$local_svc" "$ACTION" "$INSTANCES"
fi
echo ""

# ─── Wait for health ───
wait_healthy() {
    local name=$1
    local retries=60
    echo -n "  Waiting for $name"
    while [[ $retries -gt 0 ]]; do
        local status
        status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "missing")
        if [[ "$status" == "healthy" ]]; then
            echo " OK"
            return 0
        fi
        echo -n "."
        sleep 2
        retries=$((retries - 1))
    done
    echo " FAILED"
    return 1
}

echo "=== Waiting for health checks ==="
HEALTH_OK=true
if [[ "$TARGET_SERVICE" == "all" ]]; then
    for svc in "${SERVICES[@]}"; do
        for i in $(seq 1 "$INSTANCES"); do
            wait_healthy "${svc}-${ACTION}-${i}" || HEALTH_OK=false
        done
    done
else
    local_svc="${TARGET_SERVICE%-service}"
    for i in $(seq 1 "$INSTANCES"); do
        wait_healthy "${local_svc}-${ACTION}-${i}" || HEALTH_OK=false
    done
fi

if [[ "$HEALTH_OK" != true ]]; then
    echo ""
    echo "ERROR: Some containers failed health checks. Aborting."
    echo "  Check logs: docker logs <container-name>"
    echo "  Run './deploy.sh status' to see what's running."
    exit 1
fi
echo ""

echo "=== Updating nginx ==="
NGINX_TARGET=$([[ "$TARGET_SERVICE" == "all" ]] && echo "all" || echo "${TARGET_SERVICE%-service}")
generate_nginx_conf "$ACTION" "$NGINX_TARGET"
docker exec nging-gateway nginx -t && docker exec nging-gateway nginx -s reload
echo "  Nginx reloaded"
echo ""

# ─── Stop old renamed same-color containers ───
echo "=== Stopping old same-color containers ==="
for container in $(docker ps --filter "network=$NETWORK" --filter "name=-old" --format "{{.Names}}"); do
    echo "  Stopping $container (graceful, 30s timeout)..."
    docker stop --time 30 "$container" > /dev/null && docker rm "$container" > /dev/null
    echo "    Stopped $container"
done
echo ""

# ─── Scale down: stop excess same-color containers ───
echo "=== Scaling down excess $ACTION containers ==="
if [[ "$TARGET_SERVICE" == "all" ]]; then
    for svc in "${SERVICES[@]}"; do
        for container in $(docker ps --filter "network=$NETWORK" --filter "name=${svc}-${ACTION}-" --format "{{.Names}}" | grep -v -- '-old$' | sort); do
            # Extract instance number from container name (e.g. auth-blue-3 -> 3)
            idx=$(echo "$container" | grep -oE '[0-9]+$')
            if [[ "$idx" -gt "$INSTANCES" ]]; then
                echo "  Stopping $container (scaled down)..."
                docker stop --time 30 "$container" > /dev/null && docker rm "$container" > /dev/null
                echo "    Stopped $container"
            fi
        done
    done
else
    local_svc="${TARGET_SERVICE%-service}"
    for container in $(docker ps --filter "network=$NETWORK" --filter "name=${local_svc}-${ACTION}-" --format "{{.Names}}" | grep -v -- '-old$' | sort); do
        idx=$(echo "$container" | grep -oE '[0-9]+$')
        if [[ "$idx" -gt "$INSTANCES" ]]; then
            echo "  Stopping $container (scaled down)..."
            docker stop --time 30 "$container" > /dev/null && docker rm "$container" > /dev/null
            echo "    Stopped $container"
        fi
    done
fi
echo ""

# ─── Keep old other-color containers for rollback ───
echo "=== Previous $OTHER containers kept for rollback ==="
if [[ "$TARGET_SERVICE" == "all" ]]; then
    for svc in "${SERVICES[@]}"; do
        for container in $(docker ps --filter "name=${svc}-${OTHER}-" --format "{{.Names}}" | grep -v -- '-old$'); do
            echo "  $container still running (use './deploy.sh rollback' to switch back)"
        done
    done
else
    local_svc="${TARGET_SERVICE%-service}"
    for container in $(docker ps --filter "name=${local_svc}-${OTHER}-" --format "{{.Names}}" | grep -v -- '-old$'); do
        echo "  $container still running (use './deploy.sh rollback' to switch back)"
    done
fi
echo ""

echo "=== Deployment complete ==="
echo ""
docker ps --filter "network=$NETWORK" --format "table {{.Names}}\t{{.Status}}" | sort
echo ""
echo "Rollback: ./deploy.sh rollback"
