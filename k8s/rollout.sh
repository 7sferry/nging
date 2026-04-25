#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
NAMESPACE="nging"

SERVICES=("auth-service" "user-service" "accounting-service" "nginx")

usage() {
  echo "Usage: $0 <service>"
  echo ""
  echo "Available services:"
  for s in "${SERVICES[@]}"; do
    echo "  $s"
  done
  exit 1
}

if [[ $# -lt 1 ]]; then
  usage
fi

SERVICE="$1"

# Validate service name
VALID=false
for s in "${SERVICES[@]}"; do
  if [[ "$s" == "$SERVICE" ]]; then
    VALID=true
    break
  fi
done

if [[ "$VALID" == false ]]; then
  echo "Error: unknown service '$SERVICE'"
  usage
fi

# Use minikube's bundled kubectl if kubectl is not installed
if ! command -v kubectl &>/dev/null && command -v minikube &>/dev/null; then
  kubectl() { minikube kubectl -- "$@"; }
fi

# Point to minikube's Docker daemon
if command -v minikube &>/dev/null; then
  echo "==> Configuring Docker to use minikube's daemon..."
  eval $(minikube docker-env)
fi

# Build the image
echo "==> Building $SERVICE image..."
if [[ "$SERVICE" == "nginx" ]]; then
  NGINX_CTX=$(mktemp -d)
  cp "$PROJECT_DIR/nginx/Dockerfile" "$NGINX_CTX/"
  cp -r "$PROJECT_DIR/static" "$NGINX_CTX/html"
  docker build -t "nging/$SERVICE:latest" "$NGINX_CTX"
  rm -rf "$NGINX_CTX"
else
  docker build -t "nging/$SERVICE:latest" -f "$PROJECT_DIR/$SERVICE/Dockerfile" "$PROJECT_DIR"
fi

# Rolling restart
echo "==> Rolling out $SERVICE..."
kubectl rollout restart "deployment/$SERVICE" -n "$NAMESPACE"
kubectl rollout status "deployment/$SERVICE" -n "$NAMESPACE" --timeout=180s

echo ""
echo "==> $SERVICE rolled out successfully!"
kubectl get pods -n "$NAMESPACE" -l "app=$SERVICE"
