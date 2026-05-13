#!/bin/bash
# SkyWays Airlines — Kubernetes Rollback Script
# Rolls back one or all services to the previous revision.
# Usage:
#   ./rollback.sh                        # rolls back all services
#   ./rollback.sh booking-service        # rolls back one service
#   ./rollback.sh --revision 3           # rolls back to a specific revision
#   ./rollback.sh booking-service --revision 2

set -euo pipefail

NAMESPACE="skyways-core"
TARGET_SERVICE="${1:-all}"
REVISION="${REVISION:-0}"   # 0 = previous revision

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

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --revision)
        REVISION="$2"; shift 2 ;;
      --namespace)
        NAMESPACE="$2"; shift 2 ;;
      *)
        if [[ "$1" != "all" ]]; then TARGET_SERVICE="$1"; fi
        shift ;;
    esac
  done
}

rollback_service() {
  local service="skyways-$1"
  local revision_arg=""
  if [ "$REVISION" -gt 0 ] 2>/dev/null; then
    revision_arg="--to-revision=$REVISION"
  fi

  log "Rolling back deployment/$service (revision=$REVISION) ..."
  kubectl rollout undo deployment/"$service" \
    -n "$NAMESPACE" \
    $revision_arg

  log "Waiting for rollback to complete: $service ..."
  kubectl rollout status deployment/"$service" \
    -n "$NAMESPACE" \
    --timeout=300s || fail "Rollback failed for $service"

  log "  $service rolled back successfully."
  kubectl get deployment "$service" -n "$NAMESPACE" \
    -o jsonpath='{"  Image: "}{.spec.template.spec.containers[0].image}{"\n"}'
}

show_history() {
  local service="skyways-$1"
  log "Revision history for $service:"
  kubectl rollout history deployment/"$service" -n "$NAMESPACE"
}

# ─── Main ─────────────────────────────────────────────────────────────────────
parse_args "$@"

kubectl cluster-info > /dev/null 2>&1 || fail "kubectl cannot reach cluster"

if [ "$TARGET_SERVICE" = "all" ]; then
  log "Rolling back ALL SkyWays services in namespace: $NAMESPACE"
  for svc in "${SERVICES[@]}"; do
    show_history "$svc"
  done
  read -rp "Proceed with rollback of all services? [y/N] " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    log "Rollback cancelled."
    exit 0
  fi
  for svc in "${SERVICES[@]}"; do
    rollback_service "$svc"
  done
else
  # Strip "skyways-" prefix if provided
  TARGET_SERVICE="${TARGET_SERVICE#skyways-}"
  show_history "$TARGET_SERVICE"
  rollback_service "$TARGET_SERVICE"
fi

log ""
log "========================================"
log "Rollback complete."
log "========================================"
kubectl get pods -n "$NAMESPACE"
