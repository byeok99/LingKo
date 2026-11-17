#!/usr/bin/env bash
# host guard가 허용 대상 밖의 요청을 실제로 막는지 검증한다.
#
# guard는 "실수로 운영을 때리는" 사고를 막는 유일한 장치이므로, 주석이나 review가 아니라
# 자동 test로 고정한다. 서버가 떠 있지 않아도 실행된다. guard는 init context에서 판정하므로
# HTTP 요청이 성공하는지와 무관하게 결과가 나온다.
set -Eeuo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PERFORMANCE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
SCENARIO="$PERFORMANCE_DIR/scenarios/public-read.js"

failures=0

fail() {
  echo "❌ $*" >&2
  failures=$((failures + 1))
}

pass() {
  echo "✅ $*"
}

command -v k6 >/dev/null 2>&1 || {
  echo "k6를 찾을 수 없습니다. brew install k6 후 다시 실행하세요." >&2
  exit 1
}
[[ -f "$SCENARIO" ]] || {
  echo "scenario 파일이 없습니다: $SCENARIO" >&2
  exit 1
}

# guard만 평가되도록 최소 실행으로 돌린다. guard는 init context에서 판정하므로
# 서버가 없어도 결과가 나온다. 알 수 없는 flag는 guard보다 먼저 실패해 거짓 통과를
# 만들므로, flag 오류가 감지되면 test를 바로 중단한다.
run_guard() {
  cd "$PERFORMANCE_DIR"
  local output
  output=$(env "$@" \
    PERF_MODE=smoke \
    k6 run --quiet --summary-mode disabled --no-usage-report \
    --vus 1 --iterations 1 \
    "$SCENARIO" 2>&1 || true)

  if grep -q "unknown flag" <<<"$output"; then
    echo "k6 flag 호환성 오류로 guard를 평가하지 못했습니다: $(grep -m1 'unknown flag' <<<"$output")" >&2
    exit 1
  fi
  printf '%s' "$output"
}

expect_rejected() {
  local label="$1"
  shift
  local output
  output=$(run_guard "$@")

  if grep -q "GUARD_REJECTED" <<<"$output"; then
    pass "$label → 차단됨"
  else
    fail "$label → 차단되지 않았습니다. 출력: $(head -3 <<<"$output" | tr '\n' ' ')"
  fi
}

expect_guard_passed() {
  local label="$1"
  shift
  local output
  output=$(run_guard "$@")

  # 서버가 없으면 HTTP는 실패하지만 guard 단계는 통과해야 한다.
  if grep -q "GUARD_REJECTED" <<<"$output"; then
    fail "$label → 허용되어야 하는데 차단됐습니다. 출력: $(grep -m1 GUARD_REJECTED <<<"$output")"
  else
    pass "$label → guard 통과"
  fi
}

echo "== host guard =="

# 외부 host는 PERF_TARGET 없이 무조건 차단한다.
expect_rejected "외부 host" \
  PERF_WORKER_DISABLED=true BASE_URL=http://example.com:8080

# 운영으로 보이는 host는 staging 확인을 줘도 차단한다.
expect_rejected "운영 추정 host(amazonaws.com)" \
  PERF_WORKER_DISABLED=true BASE_URL=http://ec2-1-2-3-4.compute.amazonaws.com:8080 \
  PERF_TARGET=staging PERF_CONFIRM_STAGING=true

# staging은 확인 변수가 하나만 있으면 통과하지 못한다.
expect_rejected "staging 확인 누락" \
  PERF_WORKER_DISABLED=true BASE_URL=http://staging.internal:8080 PERF_TARGET=staging

# URL에 credential이 들어가면 결과 파일로 샐 수 있으므로 형식 자체를 막는다.
expect_rejected "URL 내 인증 정보" \
  PERF_WORKER_DISABLED=true BASE_URL=http://user:pw@127.0.0.1:8080

# 형식이 깨진 값도 조용히 기본값으로 넘어가지 않는다.
expect_rejected "형식 오류" \
  PERF_WORKER_DISABLED=true BASE_URL=127.0.0.1:8080

echo "== 외부 호출 차단 선언 =="

# Worker 비활성화 선언이 없으면 Azure·Replicate 비용 위험이 있으므로 실행을 막는다.
expect_rejected "PERF_WORKER_DISABLED 누락" \
  BASE_URL=http://127.0.0.1:8080

echo "== 허용 대상 =="

expect_guard_passed "기본값(환경변수 없음)" \
  PERF_WORKER_DISABLED=true

expect_guard_passed "localhost" \
  PERF_WORKER_DISABLED=true BASE_URL=http://localhost:8080

expect_guard_passed "staging + 확인 2개" \
  PERF_WORKER_DISABLED=true BASE_URL=http://staging.internal:8080 \
  PERF_TARGET=staging PERF_CONFIRM_STAGING=true

echo
if ((failures > 0)); then
  # 한글이 바로 뒤에 붙으면 bash가 변수명에 포함시키므로 중괄호로 경계를 명시한다.
  echo "guard test 실패: ${failures}건" >&2
  exit 1
fi
echo "guard test 통과"
