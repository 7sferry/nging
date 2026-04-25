#!/usr/bin/env bash
set -euo pipefail

# Blue-green rollout script.
#
# How it works:
#   1. Detect current active slot (blue or green) from the Service selector
#   2. Build the new image
#   3. Create/update the inactive slot Deployment with the new image
#   4. Wait until the new pods are ready
#   5. Switch the Service selector to the new slot (instant cutover)
#   6. Scale down the old slot
#
# This guarantees all traffic switches at once — no mixed versions.

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

# --- Detect current active slot ---
CURRENT_SLOT=$(kubectl get svc "$SERVICE" -n "$NAMESPACE" -o jsonpath='{.spec.selector.slot}' 2>/dev/null || echo "blue")
if [[ "$CURRENT_SLOT" == "blue" ]]; then
  NEW_SLOT="green"
else
  NEW_SLOT="blue"
fi

echo "==> Current active slot: $CURRENT_SLOT"
echo "==> Deploying to slot:   $NEW_SLOT"

# --- Build the image ---
IMAGE_TAG="nging/$SERVICE:$NEW_SLOT"
echo "==> Building image $IMAGE_TAG..."
if [[ "$SERVICE" == "nginx" ]]; then
  NGINX_CTX=$(mktemp -d)
  cp "$PROJECT_DIR/nginx/Dockerfile" "$NGINX_CTX/"
  cp -r "$PROJECT_DIR/static" "$NGINX_CTX/html"
  docker build -t "$IMAGE_TAG" "$NGINX_CTX"
  rm -rf "$NGINX_CTX"
else
  docker build -t "$IMAGE_TAG" -f "$PROJECT_DIR/$SERVICE/Dockerfile" "$PROJECT_DIR"
fi

# --- Get the current deployment spec as a base ---
NEW_DEPLOY="$SERVICE-$NEW_SLOT"
OLD_DEPLOY="$SERVICE-$CURRENT_SLOT"

# Read replicas from the current active deployment (or default to 1)
REPLICAS=$(kubectl get deployment "$SERVICE" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")

# Export the current deployment, modify it for the new slot
echo "==> Creating deployment $NEW_DEPLOY..."
kubectl get deployment "$SERVICE" -n "$NAMESPACE" -o json | \
  jq --arg name "$NEW_DEPLOY" \
     --arg slot "$NEW_SLOT" \
     --arg image "$IMAGE_TAG" \
     --argjson replicas "$REPLICAS" \
     '
       .metadata.name = $name |
       del(.metadata.resourceVersion, .metadata.uid, .metadata.creationTimestamp, .metadata.generation, .metadata.annotations, .status) |
       .spec.replicas = $replicas |
       .spec.selector.matchLabels.slot = $slot |
       .spec.template.metadata.labels.slot = $slot |
       .spec.template.spec.containers[0].image = $image
     ' | kubectl apply -f -

# --- Wait for new deployment to be ready ---
echo "==> Waiting for $NEW_DEPLOY to be ready..."
kubectl rollout status "deployment/$NEW_DEPLOY" -n "$NAMESPACE" --timeout=180s

# --- Switch Service to new slot (instant cutover) ---
echo "==> Switching traffic: $CURRENT_SLOT -> $NEW_SLOT"
kubectl patch svc "$SERVICE" -n "$NAMESPACE" -p "{\"spec\":{\"selector\":{\"slot\":\"$NEW_SLOT\"}}}"

echo "==> Traffic is now on $NEW_SLOT!"

# --- Scale down old deployment ---
echo "==> Scaling down old deployment ($OLD_DEPLOY)..."
if kubectl get deployment "$OLD_DEPLOY" -n "$NAMESPACE" &>/dev/null; then
  kubectl scale "deployment/$OLD_DEPLOY" -n "$NAMESPACE" --replicas=0
fi
# Also scale down the original deployment if it still exists with the base name
if [[ "$OLD_DEPLOY" != "$SERVICE" ]]; then
  kubectl scale "deployment/$SERVICE" -n "$NAMESPACE" --replicas=0 2>/dev/null || true
fi

echo ""
echo "==> Blue-green rollout complete!"
echo "    Active slot: $NEW_SLOT"
kubectl get pods -n "$NAMESPACE" -l "app=$SERVICE"
