#!/usr/bin/env bash
# Create/update K8s secrets for carland in preprod from local files + droplet pull.
# Does NOT modify the production droplet.
set -euo pipefail

SECRETS_DIR="${CARCAT_SECRETS_DIR:-$HOME/Documents/carcat_tech/.secrets}"
export KUBECONFIG="${KUBECONFIG:-$SECRETS_DIR/kubeconfig-carcat.yaml}"
NS="preprod"
SECRET="carland-service-env"
FIREBASE_SECRET="carland-firebase"

PASS_JSON="$SECRETS_DIR/postgres_preprod_user_passwords.json"
REDIS_PASS_FILE="$SECRETS_DIR/redis_preprod_password.txt"
FIREBASE_FILE="$SECRETS_DIR/firebase.json"
ENV_FILE="$SECRETS_DIR/carland_preprod_env.env"

if [[ ! -f "$PASS_JSON" ]]; then
  echo "Missing $PASS_JSON" >&2
  exit 1
fi
if [[ ! -f "$REDIS_PASS_FILE" ]]; then
  echo "Missing $REDIS_PASS_FILE" >&2
  exit 1
fi
if [[ ! -f "$FIREBASE_FILE" ]]; then
  echo "Missing $FIREBASE_FILE — copy from droplet /root/secrets/firebase.json" >&2
  exit 1
fi

DB_PASS="$(python3 -c "import json; print(json.load(open('$PASS_JSON'))['carland'])")"
REDIS_PASS="$(tr -d '[:space:]' < "$REDIS_PASS_FILE")"
PG_HOST="$(tr -d '[:space:]' < "$SECRETS_DIR/postgres_preprod_host.txt")"
DB_URL="jdbc:postgresql://${PG_HOST}:25060/carlanddatabase?sslmode=require"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "==> fetching env keys from droplet into $ENV_FILE (one-time)"
  ssh -i "${SSH_KEY:-$HOME/.ssh/id_ed25519_carcat}" -o BatchMode=yes root@142.93.169.121 \
    'grep -E "^[A-Z][A-Z0-9_]*=" /root/carland.env' \
    > "$ENV_FILE"
  chmod 600 "$ENV_FILE"
fi
if [[ ! -s "$ENV_FILE" ]]; then
  echo "Fill $ENV_FILE with carland env keys" >&2
  exit 1
fi

kubectl create namespace "$NS" --dry-run=client -o yaml | kubectl apply -f -

MERGED="$(mktemp)"
trap 'rm -f "$MERGED"' EXIT

# Drop droplet DB/Redis lines — replace with managed DOKS values
grep -Ev '^(DB_URL|DB_USERNAME|DB_PASSWORD|REDIS_HOST|REDIS_PORT|REDIS_PASSWORD)=' "$ENV_FILE" > "$MERGED" || true
{
  printf 'DB_URL=%s\n' "$DB_URL"
  printf 'DB_USERNAME=carland\n'
  printf 'DB_PASSWORD=%s\n' "$DB_PASS"
  printf 'REDIS_HOST=redis\n'
  printf 'REDIS_PORT=6379\n'
  printf 'REDIS_PASSWORD=%s\n' "$REDIS_PASS"
} >> "$MERGED"

kubectl -n "$NS" create secret generic "$SECRET" \
  --from-env-file="$MERGED" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n "$NS" create secret generic "$FIREBASE_SECRET" \
  --from-file=firebase.json="$FIREBASE_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secrets $SECRET + $FIREBASE_SECRET updated in $NS"
