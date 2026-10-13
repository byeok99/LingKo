#!/usr/bin/env bash
# 운영 로그가 로컬 디스크에 무제한 누적되지 않고 API·Worker별 CloudWatch Logs로 분리되는 계약을 검증한다.
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
BACKEND_DIR="$PROJECT_ROOT/backend"
COMPOSE_FILE="$BACKEND_DIR/docker-compose.yml"
PRODUCTION_COMPOSE_FILE="$BACKEND_DIR/docker-compose.prod.yml"
APPLICATION_CONFIG="$BACKEND_DIR/src/main/resources/application.yaml"
ENV_EXAMPLE="$BACKEND_DIR/.env.example"
DEPLOY_SCRIPT="$PROJECT_ROOT/scripts/deploy-backend.sh"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_contains() {
  local file=$1
  local expected=$2
  grep -F -- "$expected" "$file" >/dev/null || fail "$file does not contain: $expected"
}

command -v docker >/dev/null || fail "docker is required"
command -v python3 >/dev/null || fail "python3 is required"
[[ -f $PRODUCTION_COMPOSE_FILE ]] || fail "production Compose override is required"

# Spring이 별도 파일에 같은 로그를 복제하면 container stdout과 이중 저장되므로 금지한다.
if grep -Eq '^[[:space:]]+(file:|name:[[:space:]]*logs/lingko\.log)' "$APPLICATION_CONFIG"; then
  fail "Spring file logging must be disabled"
fi
assert_contains "$APPLICATION_CONFIG" 'com.lingko.lingko: ${LOG_LEVEL:INFO}'
assert_contains "$ENV_EXAMPLE" 'LOG_LEVEL=INFO'
assert_contains "$ENV_EXAMPLE" 'AWS_LOGS_REGION=us-east-1'
assert_contains "$ENV_EXAMPLE" 'AWS_LOGS_API_GROUP=/lingko/prod/api'
assert_contains "$ENV_EXAMPLE" 'AWS_LOGS_WORKER_GROUP=/lingko/prod/worker'

# Compose의 env_file 경로를 안전하게 충족하는 임시 사본에서 실제 merge 결과를 검사한다.
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
cp "$COMPOSE_FILE" "$fixture/docker-compose.yml"
cp "$PRODUCTION_COMPOSE_FILE" "$fixture/docker-compose.prod.yml"
cp "$ENV_EXAMPLE" "$fixture/.env"
DB_PASSWORD=ci-only-placeholder \
  docker compose --project-directory "$fixture" \
  --env-file "$fixture/.env" \
  -f "$fixture/docker-compose.yml" \
  -f "$fixture/docker-compose.prod.yml" \
  --profile operations \
  config --format json >"$fixture/compose.json"

DB_PASSWORD=ci-only-placeholder \
  docker compose --project-directory "$fixture" \
  --env-file "$fixture/.env" \
  -f "$fixture/docker-compose.yml" \
  --profile operations \
  config --format json >"$fixture/local-compose.json"

python3 - "$fixture/compose.json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    services = json.load(source)["services"]

expected_groups = {
    "backend": "/lingko/prod/api",
    "evaluation-worker": "/lingko/prod/worker",
}
for service_name, expected_group in expected_groups.items():
    logging = services[service_name].get("logging", {})
    if logging.get("driver") != "awslogs":
        raise SystemExit(f"{service_name}: awslogs driver is required")
    options = logging.get("options", {})
    expected = {
        "awslogs-region": "us-east-1",
        "awslogs-group": expected_group,
        "awslogs-create-group": "false",
        "mode": "non-blocking",
        "max-buffer-size": "4m",
    }
    for key, value in expected.items():
        if str(options.get(key)).lower() != value:
            raise SystemExit(f"{service_name}: expected {key}={value}, got {options.get(key)!r}")
    if "awslogs-stream" in options:
        raise SystemExit(f"{service_name}: fixed awslogs-stream must not be used")

for service_name in ("mysql", "guide-prewarm"):
    logging = services[service_name].get("logging", {})
    if logging.get("driver") != "json-file":
        raise SystemExit(f"{service_name}: json-file rotation is required")
    options = logging.get("options", {})
    if options.get("max-size") != "10m" or options.get("max-file") != "3":
        raise SystemExit(f"{service_name}: expected max-size=10m and max-file=3")
PY

python3 - "$fixture/local-compose.json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    services = json.load(source)["services"]

# 로컬 개발이 AWS 자격 증명을 요구하거나 운영 Log Group에 개발 로그를 전송하면 안 된다.
for service_name in ("backend", "evaluation-worker", "mysql", "guide-prewarm"):
    logging = services[service_name].get("logging", {})
    if logging.get("driver") != "json-file":
        raise SystemExit(f"{service_name}: local Compose must use json-file rotation")
    options = logging.get("options", {})
    if options.get("max-size") != "10m" or options.get("max-file") != "3":
        raise SystemExit(f"{service_name}: local rotation must use max-size=10m and max-file=3")
PY

# 실제 container 교체 전에 Docker daemon, instance role, endpoint와 두 Log Group 쓰기 권한을 확인해야 한다.
assert_contains "$DEPLOY_SCRIPT" 'verify_awslogs_group "$AWS_LOGS_API_GROUP"'
assert_contains "$DEPLOY_SCRIPT" 'verify_awslogs_group "$AWS_LOGS_WORKER_GROUP"'
assert_contains "$DEPLOY_SCRIPT" '--log-driver awslogs'
assert_contains "$DEPLOY_SCRIPT" '--log-opt mode=non-blocking'
assert_contains "$DEPLOY_SCRIPT" '--log-opt max-buffer-size=4m'
assert_contains "$DEPLOY_SCRIPT" 'docker-compose.prod.yml'

echo "PASS: cloudwatch-logs"
