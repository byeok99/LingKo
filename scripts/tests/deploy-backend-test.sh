#!/usr/bin/env bash
# Backend CD가 잘못된 revision을 배포하거나 health check 실패 후 장애 image를 남기는 회귀를 막는다.
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DEPLOY_SCRIPT="$PROJECT_ROOT/scripts/deploy-backend.sh"
PRODUCTION_COMPOSE_FILE="$PROJECT_ROOT/backend/docker-compose.prod.yml"
TARGET_SHA=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_contains() {
  local file=$1
  local expected=$2
  grep -F -- "$expected" "$file" >/dev/null || fail "'$expected' was not recorded"
}

write_fake_commands() {
  local bin_dir=$1

  cat >"$bin_dir/git" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ ${1:-} == "-C" ]]; then
  shift 2
fi
case "${1:-} ${2:-}" in
  "rev-parse HEAD") echo "$TEST_SHA" ;;
  "branch --show-current") echo "develop" ;;
  "status --porcelain")
    [[ ${TEST_DIRTY:-false} == "true" ]] && echo " M backend/Dockerfile"
    true
    ;;
  "remote get-url") echo "https://github.com/byeok99/LingKo.git" ;;
  *) echo "unexpected git command: $*" >&2; exit 64 ;;
esac
EOF

  cat >"$bin_dir/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
echo "docker $* tag=${BACKEND_IMAGE_TAG:-unset}" >>"$TEST_LOG"
if [[ ${1:-} == "compose" && " $* " == *" ps "* ]]; then
  if [[ " $* " == *" -q evaluation-worker "* ]]; then
    echo "worker-container"
  else
    echo "backend running"
    echo "evaluation-worker running"
  fi
  exit 0
fi
if [[ ${1:-} == "compose" && " $* " == *" up "* ]]; then
  if [[ -n ${TEST_UP_FAIL_TAG:-} && ${BACKEND_IMAGE_TAG:-unset} == "$TEST_UP_FAIL_TAG" ]]; then
    exit 66
  fi
  printf '%s' "${BACKEND_IMAGE_TAG:-unset}" >"$TEST_STATE"
  exit 0
fi
case "${1:-} ${2:-}" in
  "info ") exit 0 ;;
  "compose version") exit 0 ;;
  "inspect --format")
    target=${@: -1}
    if [[ " $* " == *"{{.Image}}"* && $target == "lingko-backend" ]]; then
      echo "sha256:previous"
    elif [[ " $* " == *"{{.RestartCount}}"* ]]; then
      deployed_tag=$(cat "$TEST_STATE" 2>/dev/null || true)
      if [[ $target == "worker-container" && -n ${TEST_RESTART_TAG:-} && $deployed_tag == "$TEST_RESTART_TAG" ]]; then
        echo "1"
      else
        echo "0"
      fi
    else
      echo "true"
    fi
    ;;
  "image tag") exit 0 ;;
  "build --tag") exit 0 ;;
  "run --rm")
    if [[ -n ${TEST_AWSLOGS_FAIL_GROUP:-} && " $* " == *" awslogs-group=$TEST_AWSLOGS_FAIL_GROUP "* ]]; then
      exit 67
    fi
    exit 0
    ;;
  *) echo "unexpected docker command: $*" >&2; exit 65 ;;
esac
EOF

  cat >"$bin_dir/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
deployed_tag=$(cat "$TEST_STATE" 2>/dev/null || true)
echo "curl tag=$deployed_tag" >>"$TEST_LOG"
if [[ -n ${TEST_FAIL_TAG:-} && $deployed_tag == "$TEST_FAIL_TAG" ]]; then
  exit 22
fi
exit 0
EOF

  chmod +x "$bin_dir/git" "$bin_dir/docker" "$bin_dir/curl"
}

new_fixture() {
  FIXTURE=$(mktemp -d)
  mkdir -p "$FIXTURE/repo/scripts" "$FIXTURE/repo/backend" "$FIXTURE/bin"
  cp "$DEPLOY_SCRIPT" "$FIXTURE/repo/scripts/deploy-backend.sh"
  cp "$PRODUCTION_COMPOSE_FILE" "$FIXTURE/repo/backend/docker-compose.prod.yml"
  chmod +x "$FIXTURE/repo/scripts/deploy-backend.sh"
  printf 'APP_PORT=18080\n' >"$FIXTURE/repo/backend/.env"
  TEST_LOG="$FIXTURE/commands.log"
  TEST_STATE="$FIXTURE/deployed-tag"
  export TEST_LOG TEST_STATE TEST_SHA="$TARGET_SHA" LINGKO_DEPLOY_STABILITY_SECONDS=0
  write_fake_commands "$FIXTURE/bin"
}

cleanup_fixture() {
  rm -rf "$FIXTURE"
}

test_rejects_invalid_sha() {
  new_fixture
  if PATH="$FIXTURE/bin:$PATH" "$FIXTURE/repo/scripts/deploy-backend.sh" not-a-sha >/dev/null 2>&1; then
    fail "invalid SHA was accepted"
  fi
  [[ ! -s $TEST_LOG ]] || fail "commands ran for an invalid SHA"
  cleanup_fixture
}

test_rejects_dirty_checkout() {
  new_fixture
  export TEST_DIRTY=true
  if PATH="$FIXTURE/bin:$PATH" "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null 2>&1; then
    fail "dirty checkout was accepted"
  fi
  assert_contains "$TEST_LOG" "docker info"
  if grep -F "docker build" "$TEST_LOG" >/dev/null; then
    fail "image build ran for a dirty checkout"
  fi
  unset TEST_DIRTY
  cleanup_fixture
}

test_deploys_exact_commit_image() {
  new_fixture
  PATH="$FIXTURE/bin:$PATH" \
    LINGKO_DEPLOY_LOCK_DIR="$FIXTURE/lock" \
    LINGKO_DEPLOY_HEALTH_ATTEMPTS=1 \
    "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null
  assert_contains "$TEST_LOG" "docker build --tag lingko-backend:$TARGET_SHA ."
  assert_contains "$TEST_LOG" "docker run --rm --entrypoint sh --log-driver awslogs --log-opt awslogs-region=us-east-1 --log-opt awslogs-group=/lingko/prod/api"
  assert_contains "$TEST_LOG" "docker run --rm --entrypoint sh --log-driver awslogs --log-opt awslogs-region=us-east-1 --log-opt awslogs-group=/lingko/prod/worker"
  assert_contains "$TEST_LOG" "docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-deps backend evaluation-worker tag=$TARGET_SHA"
  [[ $(grep -Fc 'docker inspect --format {{.RestartCount}} worker-container' "$TEST_LOG") -ge 2 ]] \
    || fail "worker stability was not checked twice"
  cleanup_fixture
}

test_stops_before_replacement_when_awslogs_preflight_fails() {
  new_fixture
  export TEST_AWSLOGS_FAIL_GROUP=/lingko/prod/worker
  if PATH="$FIXTURE/bin:$PATH" \
    LINGKO_DEPLOY_LOCK_DIR="$FIXTURE/lock" \
    LINGKO_DEPLOY_HEALTH_ATTEMPTS=1 \
    "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null 2>&1; then
    fail "failed awslogs preflight reported success"
  fi
  if grep -F " up -d --no-deps backend evaluation-worker " "$TEST_LOG" >/dev/null; then
    fail "container replacement ran after awslogs preflight failure"
  fi
  unset TEST_AWSLOGS_FAIL_GROUP
  cleanup_fixture
}

test_rejects_long_lived_aws_credentials() {
  new_fixture
  printf 'AWS_ACCESS_KEY=legacy-id\nAWS_SECRET_KEY=legacy-secret\n' >>"$FIXTURE/repo/backend/.env"
  if PATH="$FIXTURE/bin:$PATH" \
    LINGKO_DEPLOY_LOCK_DIR="$FIXTURE/lock" \
    LINGKO_DEPLOY_HEALTH_ATTEMPTS=1 \
    "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null 2>&1; then
    fail "long-lived AWS credentials were accepted"
  fi
  if grep -F "docker build" "$TEST_LOG" >/dev/null; then
    fail "image build ran with long-lived AWS credentials"
  fi
  cleanup_fixture
}

test_rolls_back_failed_health_check() {
  new_fixture
  export TEST_FAIL_TAG="$TARGET_SHA"
  if PATH="$FIXTURE/bin:$PATH" \
    LINGKO_DEPLOY_LOCK_DIR="$FIXTURE/lock" \
    LINGKO_DEPLOY_HEALTH_ATTEMPTS=1 \
    "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null 2>&1; then
    fail "unhealthy deployment reported success"
  fi
  grep -E "docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-deps backend evaluation-worker tag=rollback-[0-9]+" "$TEST_LOG" >/dev/null \
    || fail "rollback image was not restored"
  unset TEST_FAIL_TAG
  cleanup_fixture
}

test_rolls_back_restarted_worker() {
  new_fixture
  export TEST_RESTART_TAG="$TARGET_SHA"
  if PATH="$FIXTURE/bin:$PATH" \
    LINGKO_DEPLOY_LOCK_DIR="$FIXTURE/lock" \
    LINGKO_DEPLOY_HEALTH_ATTEMPTS=1 \
    "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null 2>&1; then
    fail "restarted worker reported healthy"
  fi
  grep -E "docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-deps backend evaluation-worker tag=rollback-[0-9]+" "$TEST_LOG" >/dev/null \
    || fail "rollback image was not restored after worker restart"
  unset TEST_RESTART_TAG
  cleanup_fixture
}

test_verifies_rollback_after_replacement_failure() {
  new_fixture
  export TEST_UP_FAIL_TAG="$TARGET_SHA"
  if PATH="$FIXTURE/bin:$PATH" \
    LINGKO_DEPLOY_LOCK_DIR="$FIXTURE/lock" \
    LINGKO_DEPLOY_HEALTH_ATTEMPTS=1 \
    "$FIXTURE/repo/scripts/deploy-backend.sh" "$TARGET_SHA" >/dev/null 2>&1; then
    fail "failed replacement reported success"
  fi
  grep -E "curl tag=rollback-[0-9]+" "$TEST_LOG" >/dev/null \
    || fail "rollback health was not verified after replacement failure"
  unset TEST_UP_FAIL_TAG
  cleanup_fixture
}

[[ -x $DEPLOY_SCRIPT ]] || fail "missing executable: $DEPLOY_SCRIPT"

test_rejects_invalid_sha
test_rejects_dirty_checkout
test_deploys_exact_commit_image
test_stops_before_replacement_when_awslogs_preflight_fails
test_rejects_long_lived_aws_credentials
test_rolls_back_failed_health_check
test_rolls_back_restarted_worker
test_verifies_rollback_after_replacement_failure

echo "PASS: deploy-backend"
