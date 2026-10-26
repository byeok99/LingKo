#!/usr/bin/env bash
# develop의 정확한 commit을 공용 Backend image로 배포하고 실패 시 직전 image로 되돌린다.
set -Eeuo pipefail

TARGET_SHA=${1:-}
EXPECTED_BRANCH=${LINGKO_DEPLOY_BRANCH:-develop}
LOCK_DIR=${LINGKO_DEPLOY_LOCK_DIR:-/tmp/lingko-backend-deploy.lock}
HEALTH_ATTEMPTS=${LINGKO_DEPLOY_HEALTH_ATTEMPTS:-45}
HEALTH_INTERVAL_SECONDS=${LINGKO_DEPLOY_HEALTH_INTERVAL_SECONDS:-2}
STABILITY_SECONDS=${LINGKO_DEPLOY_STABILITY_SECONDS:-15}

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BACKEND_DIR="$REPO_ROOT/backend"
ENV_FILE="$BACKEND_DIR/.env"
PRODUCTION_COMPOSE_FILE="$BACKEND_DIR/docker-compose.prod.yml"

fail() {
  echo "DEPLOY_ERROR: $*" >&2
  exit 1
}

read_env_value() {
  local key=$1
  awk -v key="$key" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      sub(/\r$/, "", value)
      result = value
    }
    END { print result }
  ' "$ENV_FILE"
}

cleanup_lock() {
  rmdir "$LOCK_DIR" 2>/dev/null || true
}

docker_compose() {
  (
    cd "$BACKEND_DIR"
    docker compose -f docker-compose.yml -f docker-compose.prod.yml "$@"
  )
}

deploy_tag() {
  local tag=$1
  BACKEND_IMAGE_REPOSITORY="$IMAGE_REPOSITORY" \
    BACKEND_IMAGE_TAG="$tag" \
    docker_compose up -d --no-deps backend evaluation-worker
}

runtime_is_healthy() {
  local backend_running
    local backend_restart_count
  local worker_id
  local worker_running
    local worker_restart_count

  backend_running=$(docker inspect --format '{{.State.Running}}' lingko-backend 2>/dev/null || true)
  [[ $backend_running == "true" ]] || return 1
    backend_restart_count=$(docker inspect --format '{{.RestartCount}}' lingko-backend 2>/dev/null || true)
    [[ $backend_restart_count == "0" ]] || return 1

  worker_id=$(docker_compose ps -q evaluation-worker)
  [[ -n $worker_id ]] || return 1
  worker_running=$(docker inspect --format '{{.State.Running}}' "$worker_id" 2>/dev/null || true)
  [[ $worker_running == "true" ]] || return 1
    worker_restart_count=$(docker inspect --format '{{.RestartCount}}' "$worker_id" 2>/dev/null || true)
    [[ $worker_restart_count == "0" ]] || return 1

  curl --fail --silent --show-error --max-time 5 \
    "http://127.0.0.1:$APP_PORT/actuator/health" >/dev/null
}

wait_for_runtime() {
  local attempt
  for ((attempt = 1; attempt <= HEALTH_ATTEMPTS; attempt += 1)); do
    if runtime_is_healthy; then
      # restart policy가 시작 직후의 crash를 잠시 Running으로 가릴 수 있어 안정화 뒤 같은 계약을 재확인한다.
      if ((STABILITY_SECONDS > 0)); then
        sleep "$STABILITY_SECONDS"
      fi
      runtime_is_healthy && return 0
    fi
    if ((attempt < HEALTH_ATTEMPTS)); then
      sleep "$HEALTH_INTERVAL_SECONDS"
    fi
  done
  return 1
}

restore_previous_image() {
  if deploy_tag "$ROLLBACK_TAG" && wait_for_runtime; then
    echo "Rollback completed" >&2
    return 0
  fi
  echo "ROLLBACK_ERROR: previous image did not recover" >&2
  return 1
}

verify_awslogs_group() {
  local log_group=$1
  local marker="LINGKO_AWSLOGS_PREFLIGHT commit=$TARGET_SHA"

  # 실행 중 서비스를 교체하기 전에 Docker driver 초기화와 instance role의 Log Group 접근을 확인한다.
  docker run --rm \
    --entrypoint sh \
    --log-driver awslogs \
    --log-opt "awslogs-region=$AWS_LOGS_REGION" \
    --log-opt "awslogs-group=$log_group" \
    --log-opt awslogs-create-group=false \
    --log-opt mode=non-blocking \
    --log-opt max-buffer-size=4m \
    "$TARGET_IMAGE" \
    -c 'printf "%s\n" "$1"' sh "$marker" >/dev/null
}

[[ $TARGET_SHA =~ ^[0-9a-f]{40}$ ]] || fail "target commit must be a 40-character lowercase SHA"
[[ $HEALTH_ATTEMPTS =~ ^[1-9][0-9]*$ ]] || fail "health attempts must be a positive integer"
[[ $HEALTH_INTERVAL_SECONDS =~ ^[0-9]+$ ]] || fail "health interval must be a non-negative integer"
[[ $STABILITY_SECONDS =~ ^[0-9]+$ ]] || fail "stability seconds must be a non-negative integer"

for required_command in git docker curl awk date grep; do
  command -v "$required_command" >/dev/null || fail "missing command: $required_command"
done
docker info >/dev/null 2>&1 || fail "Docker daemon is unavailable"
docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin is unavailable"

[[ -d $BACKEND_DIR ]] || fail "backend directory is missing"
[[ -f $ENV_FILE ]] || fail "backend/.env is missing"
[[ -f $PRODUCTION_COMPOSE_FILE ]] || fail "backend/docker-compose.prod.yml is missing"

# env_file은 선언된 모든 값을 container에 전달하므로 빈 값이어도 과거 credential 항목 자체를 제거한다.
if grep -Eq '^[[:space:]]*(export[[:space:]]+)?AWS_(ACCESS_KEY|SECRET_KEY|ACCESS_KEY_ID|SECRET_ACCESS_KEY|SESSION_TOKEN)[[:space:]]*=' \
  "$ENV_FILE"; then
  fail "remove AWS credential entries from backend/.env before deployment"
fi

CURRENT_BRANCH=$(git -C "$REPO_ROOT" branch --show-current)
[[ $CURRENT_BRANCH == "$EXPECTED_BRANCH" ]] || fail "expected branch $EXPECTED_BRANCH, got $CURRENT_BRANCH"

CURRENT_SHA=$(git -C "$REPO_ROOT" rev-parse HEAD)
[[ $CURRENT_SHA == "$TARGET_SHA" ]] || fail "HEAD does not match the requested commit"

ORIGIN_URL=$(git -C "$REPO_ROOT" remote get-url origin)
case "$ORIGIN_URL" in
  git@github.com:byeok99/LingKo.git | https://github.com/byeok99/LingKo | https://github.com/byeok99/LingKo.git) ;;
  *) fail "origin does not point to byeok99/LingKo" ;;
esac

# 운영 .env 같은 ignored 파일은 허용하지만 추적 파일 수정은 재현 가능한 배포를 깨므로 중단한다.
TRACKED_CHANGES=$(git -C "$REPO_ROOT" status --porcelain --untracked-files=no)
[[ -z $TRACKED_CHANGES ]] || fail "tracked files are modified on the server"

APP_PORT=$(read_env_value APP_PORT)
APP_PORT=${APP_PORT:-8080}
[[ $APP_PORT =~ ^[0-9]{1,5}$ ]] || fail "APP_PORT must be numeric"
((APP_PORT >= 1 && APP_PORT <= 65535)) || fail "APP_PORT is outside the valid port range"

IMAGE_REPOSITORY=$(read_env_value BACKEND_IMAGE_REPOSITORY)
IMAGE_REPOSITORY=${IMAGE_REPOSITORY:-lingko-backend}
[[ $IMAGE_REPOSITORY =~ ^[A-Za-z0-9./:_-]+$ ]] || fail "BACKEND_IMAGE_REPOSITORY contains unsupported characters"

AWS_LOGS_REGION=${AWS_LOGS_REGION:-$(read_env_value AWS_LOGS_REGION)}
AWS_LOGS_REGION=${AWS_LOGS_REGION:-us-east-1}
AWS_LOGS_API_GROUP=${AWS_LOGS_API_GROUP:-$(read_env_value AWS_LOGS_API_GROUP)}
AWS_LOGS_API_GROUP=${AWS_LOGS_API_GROUP:-/lingko/prod/api}
AWS_LOGS_WORKER_GROUP=${AWS_LOGS_WORKER_GROUP:-$(read_env_value AWS_LOGS_WORKER_GROUP)}
AWS_LOGS_WORKER_GROUP=${AWS_LOGS_WORKER_GROUP:-/lingko/prod/worker}
[[ $AWS_LOGS_REGION =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]] || fail "AWS_LOGS_REGION is invalid"
[[ $AWS_LOGS_API_GROUP =~ ^/[A-Za-z0-9_./-]+$ ]] || fail "AWS_LOGS_API_GROUP is invalid"
[[ $AWS_LOGS_WORKER_GROUP =~ ^/[A-Za-z0-9_./-]+$ ]] || fail "AWS_LOGS_WORKER_GROUP is invalid"

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  fail "another backend deployment is already running"
fi
trap cleanup_lock EXIT

PREVIOUS_IMAGE=$(docker inspect --format '{{.Image}}' lingko-backend 2>/dev/null || true)
[[ -n $PREVIOUS_IMAGE ]] || fail "running backend image was not found for rollback"

ROLLBACK_TAG="rollback-$(date -u +%Y%m%d%H%M%S)"
ROLLBACK_IMAGE="$IMAGE_REPOSITORY:$ROLLBACK_TAG"
TARGET_IMAGE="$IMAGE_REPOSITORY:$TARGET_SHA"

# rollback tag는 배포 전에 고정해 compose 교체가 실패해도 기존 image를 참조할 수 있게 한다.
docker image tag "$PREVIOUS_IMAGE" "$ROLLBACK_IMAGE"

echo "Building backend image for commit $TARGET_SHA"
(
  cd "$BACKEND_DIR"
  docker build --tag "$TARGET_IMAGE" .
)

echo "Verifying CloudWatch Logs delivery prerequisites"
verify_awslogs_group "$AWS_LOGS_API_GROUP" \
  || fail "CloudWatch Logs preflight failed for API"
verify_awslogs_group "$AWS_LOGS_WORKER_GROUP" \
  || fail "CloudWatch Logs preflight failed for evaluation worker"

echo "Replacing backend and evaluation-worker"
if ! deploy_tag "$TARGET_SHA"; then
  echo "Deployment command failed; restoring $ROLLBACK_TAG" >&2
  restore_previous_image || true
  fail "container replacement failed"
fi

if ! wait_for_runtime; then
  echo "Health check failed; restoring $ROLLBACK_TAG" >&2
  restore_previous_image || true
  fail "new deployment did not become healthy"
fi

echo "Deployment completed: $TARGET_SHA"
echo "Rollback image retained: $ROLLBACK_IMAGE"
(
  BACKEND_IMAGE_REPOSITORY="$IMAGE_REPOSITORY" \
    BACKEND_IMAGE_TAG="$TARGET_SHA" \
    docker_compose ps backend evaluation-worker
)
