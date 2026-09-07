#!/usr/bin/env bash
# Helm install carland-service into DOKS preprod using CarCatApp/charts (carcat-app).
# Does NOT touch the production droplet.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SECRETS_DIR="${CARCAT_SECRETS_DIR:-$HOME/Documents/carcat_tech/.secrets}"
export KUBECONFIG="${KUBECONFIG:-$SECRETS_DIR/kubeconfig-carcat.yaml}"

NS="preprod"
RELEASE="carland-service"
# Needs 0.1.2+ for firebase volume mounts
CHART_REF="${CHART_REF:-https://github.com/CarCatApp/charts/archive/refs/tags/carcat-app-0.1.2.tar.gz}"

IMAGE_TAG="${IMAGE_TAG:-latest}"

if ! kubectl -n "$NS" get secret carland-service-env >/dev/null 2>&1; then
  echo "Missing secret carland-service-env — run ./deploy/install-secret-preprod.sh first" >&2
  exit 1
fi
if ! kubectl -n "$NS" get secret carland-firebase >/dev/null 2>&1; then
  echo "Missing secret carland-firebase — run ./deploy/install-secret-preprod.sh first" >&2
  exit 1
fi

if [[ -d "${CARCAT_CHARTS_DIR:-$HOME/Documents/carcat_tech/charts}/carcat-app" ]]; then
  CHART_PATH="${CARCAT_CHARTS_DIR:-$HOME/Documents/carcat_tech/charts}/carcat-app"
else
  CHART_PATH="$CHART_REF"
fi

echo "==> helm upgrade --install $RELEASE (ns=$NS, tag=$IMAGE_TAG, chart=$CHART_PATH)"
# No --wait: empty managed DB will CrashLoop until schema/dump is ready (same as auth).
helm upgrade --install "$RELEASE" "$CHART_PATH" \
  --namespace "$NS" \
  -f "$SCRIPT_DIR/values.yaml" \
  -f "$SCRIPT_DIR/values-preprod.yaml" \
  --set "image.tag=$IMAGE_TAG"

kubectl -n "$NS" get pods,svc -l app.kubernetes.io/name=carland-service
echo "Kong route /server-carland → carland-service.preprod.svc:9091"
echo "Note: pods may CrashLoop until carlanddatabase is migrated (waiting on app team)."
