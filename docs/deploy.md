# Kubernetes Deployment Guide

## Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/) installed and running
- Docker installed
- `jq` installed (used by rollout script)

## Initial Setup

```bash
# Start minikube
minikube start

# Deploy everything (builds images + applies all manifests)
./k8s/deploy.sh

# Get the access URL
minikube service nginx -n nging --url
```

## Architecture

```
Internet -> Nginx (NodePort 30090)
               ├── /api/auth/     -> auth-service (ClusterIP)
               ├── /api/users/    -> user-service (ClusterIP)
               └── /api/accounts/ -> accounting-service (ClusterIP)

Infrastructure:
  PostgreSQL (StatefulSet) <- used by auth-service
  Redis (StatefulSet)      <- used by auth-service
```

All app services use blue-green deployment with HPA autoscaling.

## Blue-Green Rollout

Deploy a new version with zero downtime and no mixed responses:

```bash
./k8s/rollout.sh user-service
./k8s/rollout.sh auth-service
./k8s/rollout.sh accounting-service
./k8s/rollout.sh nginx
```

How it works:

1. Detects the current active slot (blue or green) from the Service selector
2. Builds the new image and creates a Deployment for the inactive slot
3. Waits until all new pods pass readiness probes
4. Patches the Service selector — traffic switches instantly, all at once
5. Scales down the old slot

Traffic never hits a mix of versions.

## Scaling

Check which slot is currently active:

```bash
minikube kubectl -- get svc user-service -n nging -o jsonpath='{.spec.selector.slot}'
```

Scale the active slot:

```bash
minikube kubectl -- scale deployment/user-service-<slot> -n nging --replicas=3
```

Or as a one-liner:

```bash
SLOT=$(minikube kubectl -- get svc user-service -n nging -o jsonpath='{.spec.selector.slot}') && \
minikube kubectl -- scale deployment/user-service-$SLOT -n nging --replicas=3
```

The replica count carries forward on the next rollout.

## Monitoring

### Overall status

```bash
minikube kubectl -- get all -n nging
```

### Pods

```bash
minikube kubectl -- get pods -n nging
```

### Deployment rollout status

```bash
minikube kubectl -- rollout status deployment/auth-service -n nging
minikube kubectl -- rollout status deployment/user-service -n nging
minikube kubectl -- rollout status deployment/accounting-service -n nging
minikube kubectl -- rollout status deployment/nginx -n nging
```

### Infrastructure (StatefulSets)

```bash
minikube kubectl -- get statefulset -n nging
```

### Autoscaler (HPA)

```bash
minikube kubectl -- get hpa -n nging
```

### Logs

```bash
minikube kubectl -- logs -l app=auth-service -n nging --tail=50
```

### Debug a failing pod

```bash
minikube kubectl -- describe pod -l app=auth-service -n nging
```

Replace `auth-service` with `user-service`, `accounting-service`, `postgres`, `redis`, or `nginx` as needed.

## Nginx Config Changes

After editing `k8s/nginx.yaml`:

```bash
minikube kubectl -- apply -f k8s/nginx.yaml
minikube kubectl -- rollout restart deployment/nginx -n nging
```

## Shutdown

Scale down everything (preserves data):

```bash
minikube kubectl -- scale deployment --all -n nging --replicas=0
minikube kubectl -- scale statefulset --all -n nging --replicas=0
```

Delete everything:

```bash
minikube kubectl -- delete namespace nging
```

To bring it all back after deletion, run `./k8s/deploy.sh` again.

## Test Scripts

All test scripts are in `k8s/`:

| Script | Purpose |
|--------|---------|
| `node k8s/zero-downtime-test.js` | Verify zero downtime during rollout |
| `node k8s/bluegreen-test.js` | Verify blue-green cutover is atomic (no flip-flop) |
| `node k8s/rate-limit-test.js` | Verify nginx rate limiting works |

Pass `AUTH_TOKEN` env var or edit the token in each script before running.