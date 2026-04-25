To deploy

# Start minikube if not running
minikube start

# Deploy everything
./k8s/deploy.sh

# Get the access URL
minikube service nginx -n nging --url


● Here are the useful commands. All target the nging namespace:

Overall status

minikube kubectl -- get all -n nging

Pods (running instances)

minikube kubectl -- get pods -n nging

Specific deployments

minikube kubectl -- rollout status deployment/auth-service -n nging
minikube kubectl -- rollout status deployment/user-service -n nging
minikube kubectl -- rollout status deployment/accounting-service -n nging
minikube kubectl -- rollout status deployment/nginx -n nging

Infrastructure (StatefulSets)

minikube kubectl -- get statefulset -n nging

HPA (autoscaler status)

minikube kubectl -- get hpa -n nging

Logs for a specific service

minikube kubectl -- logs -l app=auth-service -n nging --tail=50

Describe a failing pod (for debugging)

minikube kubectl -- describe pod -l app=auth-service -n nging


# 1. Build inside minikube's Docker
eval $(minikube docker-env)

# 2. Rebuild the image
docker build -t nging/user-service:latest -f user-service/Dockerfile .

# 3. Restart the deployment (triggers a new rollout)
minikube kubectl -- rollout restart deployment/user-service -n nging

# 4. Watch the rollout (optional)
minikube kubectl -- rollout status deployment/user-service -n nging


./k8s/rollout.sh user-service                                                                                                                                                     
./k8s/rollout.sh auth-service
./k8s/rollout.sh accounting-service                                                                                                                                               
./k8s/rollout.sh nginx 

## scale
minikube kubectl -- scale deployment/user-service -n nging --replicas=3

To verify:                                                                                                                                                                        
minikube kubectl -- get pods -n nging -l app=user-service

# for changes to take effect
minikube kubectl -- apply -f k8s/nginx.yaml
minikube kubectl -- rollout restart deployment/nginx -n nging



# turn off all deployments
minikube kubectl -- scale deployment --all -n nging --replicas=0
minikube kubectl -- scale statefulset --all -n nging --replicas=0                                                                                                                 

# Blue-green deployment
The rollout script now does this:

1. Detects the current active slot (blue or green) from the Service selector
2. Builds the new image and creates a new Deployment for the inactive slot
3. Waits until all new pods pass readiness probes
4. Patches the Service selector in one atomic operation — traffic switches instantly, all at once
5. Scales down the old slot

Traffic never hits a mix of versions.

Usage (same as before)

./k8s/rollout.sh user-service

First deploy

You need to redeploy first since the manifests now have slot: blue labels:

minikube kubectl -- delete namespace nging                                                                                                                                        
./k8s/deploy.sh

After that, each ./k8s/rollout.sh call will alternate between blue and green.                                                                                                     

# Scale the active slot deployment directly

To check which slot is active:

minikube kubectl -- get svc user-service -n nging -o jsonpath='{.spec.selector.slot}'

minikube kubectl -- scale deployment/user-service-green -n nging --replicas=3
