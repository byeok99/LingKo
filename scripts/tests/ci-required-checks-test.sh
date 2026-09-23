#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

assert_check() {
  local workflow_file=$1
  local job_id=$2
  local expected_name=$3
  local workflow="$PROJECT_ROOT/.github/workflows/$workflow_file"
  local actual_name

  [[ -f "$workflow" ]] || fail "$workflow_file workflow가 없습니다."

  # Required status check는 workflow 이름이 아니라 job 이름을 사용하므로 job별 고유 이름을 계약으로 고정한다.
  actual_name=$(awk -v job="  $job_id:" '
    $0 == job { in_job = 1; next }
    in_job && /^  [^[:space:]]/ { exit }
    in_job && /^    name: / {
      sub(/^    name: /, "")
      print
      exit
    }
  ' "$workflow")

  [[ "$actual_name" == "$expected_name" ]] \
    || fail "${workflow_file}의 $job_id check 이름은 '$expected_name'이어야 합니다."
}

assert_check backend-ci.yml backend-test "Backend test"
assert_check flutter-ci.yml flutter-test "Flutter test"
assert_check docker-ci.yml docker-build "Docker build"

echo "PASS: ci-required-checks"
