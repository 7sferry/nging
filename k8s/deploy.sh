#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
NAMESPACE="nging"

# Use minikube's bundled kubectl if kubectl is not installed
if ! command -v kubectl &>/dev/null && command -v minikube &>/dev/null; then
  kubectl() { minikube kubectl -- "$@"; }
  export -f kubectl
  echo "==> Using 'minikube kubectl' (kubectl not found on PATH)"
fi

# Use minikube's Docker daemon so images are available to the cluster
if command -v minikube &>/dev/null; then
  echo "==> Configuring Docker to use minikube's daemon..."
  eval $(minikube docker-env)
fi

echo "==> Building Docker images..."
docker build -t nging/auth-service:latest       -f "$PROJECT_DIR/auth-service/Dockerfile"       "$PROJECT_DIR"
docker build -t nging/user-service:latest        -f "$PROJECT_DIR/user-service/Dockerfile"        "$PROJECT_DIR"
docker build -t nging/accounting-service:latest  -f "$PROJECT_DIR/accounting-service/Dockerfile"  "$PROJECT_DIR"
# Nginx needs static files — build from a temp context to avoid root .dockerignore
NGINX_CTX=$(mktemp -d)
cp "$PROJECT_DIR/nginx/Dockerfile" "$NGINX_CTX/"
cp -r "$PROJECT_DIR/static" "$NGINX_CTX/html"
docker build -t nging/nginx:latest "$NGINX_CTX"
rm -rf "$NGINX_CTX"

echo "==> Applying Kubernetes manifests..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/secrets.yaml"

echo "==> Deploying infrastructure (PostgreSQL, Redis)..."
kubectl apply -f "$SCRIPT_DIR/postgres.yaml"
kubectl apply -f "$SCRIPT_DIR/redis.yaml"

echo "==> Waiting for PostgreSQL to be ready..."
kubectl rollout status statefulset/postgres -n "$NAMESPACE" --timeout=120s

echo "==> Waiting for Redis to be ready..."
kubectl rollout status statefulset/redis -n "$NAMESPACE" --timeout=120s

echo "==> Deploying application services..."
kubectl apply -f "$SCRIPT_DIR/auth-service.yaml"
kubectl apply -f "$SCRIPT_DIR/user-service.yaml"
kubectl apply -f "$SCRIPT_DIR/accounting-service.yaml"

echo "==> Deploying Nginx gateway..."
kubectl apply -f "$SCRIPT_DIR/nginx.yaml"

echo "==> Waiting for deployments to roll out..."
kubectl rollout status deployment/auth-service       -n "$NAMESPACE" --timeout=180s
kubectl rollout status deployment/user-service        -n "$NAMESPACE" --timeout=180s
kubectl rollout status deployment/accounting-service  -n "$NAMESPACE" --timeout=180s
kubectl rollout status deployment/nginx               -n "$NAMESPACE" --timeout=120s

echo ""
echo "==> Deployment complete!"
echo ""
kubectl get pods -n "$NAMESPACE"
echo ""

if command -v minikube &>/dev/null; then
  echo "Access the app:"
  echo "  minikube service nginx -n $NAMESPACE --url"
else
  echo "Access the app at NodePort 30090 on any cluster node."
fi
