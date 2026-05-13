#!/bin/bash
# SkyWays Airlines — Full Kubernetes Deployment Script
# Usage: ./deploy.sh [--env production|staging] [--image-tag <tag>]
# Prerequisites: kubectl, Docker registry credentials, GCP project configured

set -euo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────
ENV="${ENV:-production}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY="${REGISTRY:-gcr.io/skyways-project}"
NAMESPACE_CORE="skyways-core"
NAMESPACE_DATA="skyways-data"
NAMESPACE_MONITORING="skyways-monitoring"

SERVICES=(
  "registry"
  "config-server"
  "gateway"
  "user-service"
  "flight-service"
  "booking-service"
  "payment-service"
  "notification-service"
  "saga-orchestrator"
)

# ─── Helpers ──────────────────────────────────────────────────────────────────
log()  { echo "[$(date '+%Y-%m-%dT%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

wait_for_rollout() {
  local deployment="$1"
  local namespace="$2"
  log "Waiting for rollout: $deployment in $namespace ..."
  kubectl rollout status deployment/"$deployment" \
    -n "$namespace" \
    --timeout=300s || fail "Rollout failed for $deployment"
}

# ─── Pre-flight checks ────────────────────────────────────────────────────────
log "Pre-flight checks..."
kubectl cluster-info > /dev/null 2>&1 || fail "kubectl cannot reach cluster"
log "Cluster reachable."

# ─── Step 1: Namespaces ───────────────────────────────────────────────────────
log "Applying namespaces..."
kubectl apply -f k8s/namespaces/namespaces.yaml

# ─── Step 2: ConfigMaps & Secrets ─────────────────────────────────────────────
log "Applying ConfigMaps..."
kubectl apply -f k8s/configmaps/app-configmap.yaml -n "$NAMESPACE_CORE"

log "Applying Secrets (values must be pre-populated from Secret Manager)..."
kubectl apply -f k8s/secrets/app-secrets.yaml -n "$NAMESPACE_CORE"

# ─── Step 3: Build & Push Docker Images ───────────────────────────────────────
log "Building and pushing Docker images (tag: $IMAGE_TAG)..."
for service in "${SERVICES[@]}"; do
  log "  Building $service..."
  docker build \
    -f "docker/Dockerfile.$service" \
    -t "$REGISTRY/skyways-$service:$IMAGE_TAG" \
    . --quiet
  docker push "$REGISTRY/skyways-$service:$IMAGE_TAG"
  log "  Pushed $REGISTRY/skyways-$service:$IMAGE_TAG"
done

# ─── Step 4: Update image tags in deployments ─────────────────────────────────
log "Patching deployment image tags to $IMAGE_TAG..."
for service in "${SERVICES[@]}"; do
  kubectl set image deployment/skyways-"$service" \
    skyways-"$service"="$REGISTRY/skyways-$service:$IMAGE_TAG" \
    -n "$NAMESPACE_CORE" 2>/dev/null || true
done

# ─── Step 5: Apply all K8s manifests ─────────────────────────────────────────
log "Applying Kubernetes manifests..."
kubectl apply -f k8s/deployments/ -n "$NAMESPACE_CORE"
kubectl apply -f k8s/hpa/         -n "$NAMESPACE_CORE"
kubectl apply -f k8s/ingress/     -n "$NAMESPACE_CORE"

# ─── Step 6: Kafka topic provisioning ─────────────────────────────────────────
log "Provisioning Kafka topics..."
KAFKA_POD=$(kubectl get pod -n "$NAMESPACE_DATA" -l app=kafka \
  -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [ -n "$KAFKA_POD" ]; then
  kubectl cp kafka/create-topics.sh "$NAMESPACE_DATA/$KAFKA_POD:/tmp/create-topics.sh"
  kubectl exec -n "$NAMESPACE_DATA" "$KAFKA_POD" -- \
    bash /tmp/create-topics.sh
else
  log "  WARNING: No Kafka pod found in $NAMESPACE_DATA — topic creation skipped."
fi

# ─── Step 7: Wait for all rollouts ───────────────────────────────────────────
log "Waiting for all deployments to become ready..."
for service in "${SERVICES[@]}"; do
  wait_for_rollout "skyways-$service" "$NAMESPACE_CORE"
done

# ─── Step 8: Smoke test ───────────────────────────────────────────────────────
log "Running smoke test against gateway health endpoint..."
GATEWAY_IP=$(kubectl get svc skyways-gateway -n "$NAMESPACE_CORE" \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
if [ -n "$GATEWAY_IP" ]; then
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://$GATEWAY_IP/actuator/health" || echo "000")
  if [ "$HTTP_CODE" = "200" ]; then
    log "Gateway health check PASSED (HTTP 200)."
  else
    log "WARNING: Gateway health returned HTTP $HTTP_CODE."
  fi
else
  log "  INFO: LoadBalancer IP not yet assigned — smoke test skipped."
fi

# ─── Done ────────────────────────────────────────────────────────────────────
log ""
log "========================================"
log "SkyWays deployment complete!"
log "  Environment : $ENV"
log "  Image tag   : $IMAGE_TAG"
log "  Namespace   : $NAMESPACE_CORE"
log "========================================"
kubectl get pods -n "$NAMESPACE_CORE"
