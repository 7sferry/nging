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
