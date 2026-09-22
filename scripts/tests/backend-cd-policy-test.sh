#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
WORKFLOW="$PROJECT_ROOT/.github/workflows/backend-cd.yml"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

[[ -f "$WORKFLOW" ]] || fail "Backend CD workflow가 없습니다."

# 운영 배포가 병합만으로 시작되지 않는다는 정책을 정적 검사해 우발적인 자동 배포 회귀를 막는다.
trigger_block=$(awk '
  /^on:$/ { capture = 1; next }
  capture && /^[^[:space:]]/ { exit }
  capture { print }
' "$WORKFLOW")

grep -Eq '^  workflow_dispatch:' <<<"$trigger_block" \
  || fail "운영 배포에는 workflow_dispatch 트리거가 필요합니다."

grep -Eq '^      confirm_production:$' <<<"$trigger_block" \
  || fail "운영 배포에는 명시적인 확인 입력이 필요합니다."

grep -Eq '^        type: boolean$' <<<"$trigger_block" \
  || fail "운영 배포 확인 입력은 boolean이어야 합니다."

if grep -Eq '^  (push|pull_request|schedule):' <<<"$trigger_block"; then
  fail "운영 배포는 자동 트리거를 허용하지 않습니다."
fi

grep -Eq '^    environment: production$' "$WORKFLOW" \
  || fail "운영 배포 job은 production Environment를 사용해야 합니다."

echo "PASS: backend-cd-policy"
