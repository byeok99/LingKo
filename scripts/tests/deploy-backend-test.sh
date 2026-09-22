#!/usr/bin/env bash
# Backend CD가 잘못된 revision을 배포하거나 health check 실패 후 장애 image를 남기는 회귀를 막는다.
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DEPLOY_SCRIPT="$PROJECT_ROOT/scripts/deploy-backend.sh"
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
case "${1:-} ${2:-}" in
  "info ") exit 0 ;;
  "compose version") exit 0 ;;
  "inspect --format")
    target=${@: -1}
    if [[ " $* " == *"{{.Image}}"* && $target == "lingko-backend" ]]; then
      echo "sha256:previous"
    else
      echo "true"
    fi
    ;;
  "compose ps")
    if [[ " $* " == *" -q evaluation-worker "* ]]; then
      echo "worker-container"
    else
      echo "backend running"
      echo "evaluation-worker running"
    fi
    ;;
  "image tag") exit 0 ;;
  "build --tag") exit 0 ;;
  "compose up")
    if [[ -n ${TEST_UP_FAIL_TAG:-} && ${BACKEND_IMAGE_TAG:-unset} == "$TEST_UP_FAIL_TAG" ]]; then
      exit 66
    fi
    printf '%s' "${BACKEND_IMAGE_TAG:-unset}" >"$TEST_STATE"
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
  chmod +x "$FIXTURE/repo/scripts/deploy-backend.sh"
  printf 'APP_PORT=18080\n' >"$FIXTURE/repo/backend/.env"
  TEST_LOG="$FIXTURE/commands.log"
  TEST_STATE="$FIXTURE/deployed-tag"
  export TEST_LOG TEST_STATE TEST_SHA="$TARGET_SHA"
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
  assert_contains "$TEST_LOG" "docker compose up -d --no-deps backend evaluation-worker tag=$TARGET_SHA"
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
  grep -E "docker compose up -d --no-deps backend evaluation-worker tag=rollback-[0-9]+" "$TEST_LOG" >/dev/null \
    || fail "rollback image was not restored"
  unset TEST_FAIL_TAG
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
test_rolls_back_failed_health_check
test_verifies_rollback_after_replacement_failure

echo "PASS: deploy-backend"
