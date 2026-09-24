#!/usr/bin/env bash
# CloudWatch Agent가 비용이 큰 불필요 지표나 불안정한 dimension을 추가하지 않도록 계약을 검증한다.
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
CONFIG="$PROJECT_ROOT/backend/aws/cloudwatch-agent.json"
INSTALLER="$PROJECT_ROOT/scripts/install-cloudwatch-agent.sh"
COMMAND_BUILDER="$PROJECT_ROOT/scripts/build-cloudwatch-agent-command.sh"
WORKFLOW="$PROJECT_ROOT/.github/workflows/cloudwatch-agent.yml"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

[[ -f $CONFIG ]] || fail "CloudWatch Agent 설정 파일이 없습니다."
[[ -x $INSTALLER ]] || fail "CloudWatch Agent 설치 스크립트가 없거나 실행 가능하지 않습니다."
[[ -x $COMMAND_BUILDER ]] || fail "SSM 명령 생성 스크립트가 없거나 실행 가능하지 않습니다."
[[ -f $WORKFLOW ]] || fail "CloudWatch Agent 운영 적용 workflow가 없습니다."

python3 - "$CONFIG" <<'PY'
import json
import sys

config_path = sys.argv[1]
with open(config_path, encoding="utf-8") as source:
    config = json.load(source)

agent = config["agent"]
metrics = config["metrics"]
collected = metrics["metrics_collected"]

assert agent == {"metrics_collection_interval": 60, "region": "us-east-1"}
assert metrics["namespace"] == "CWAgent"
assert metrics["append_dimensions"] == {"InstanceId": "${aws:InstanceId}"}
assert set(collected) == {"mem", "disk"}
assert collected["mem"] == {"measurement": ["used_percent"]}

disk = collected["disk"]
assert disk["resources"] == ["/"]
assert disk["measurement"] == ["used_percent", "inodes_free", "inodes_total"]
assert disk["drop_device"] is True
assert set(disk["ignore_file_system_types"]) == {
    "sysfs", "devtmpfs", "tmpfs", "overlay", "squashfs"
}
PY

bash -n "$INSTALLER"
bash -n "$COMMAND_BUILDER"

# workflow에 넣기 전 실제 jq 결과를 검사해 shell quoting 오류로 운영 명령이 중단되는 회귀를 막는다.
test_sha=0123456789abcdef0123456789abcdef01234567
parameters=$("$COMMAND_BUILDER" "$test_sha")
jq -e \
  --arg sha "$test_sha" \
  '.executionTimeout == ["900"]
   and (.commands | length == 8)
   and (.commands[0] | contains($sha))
   and (.commands[1] | contains("scripts/install-cloudwatch-agent.sh"))
   and (.commands[2] == "sudo systemctl restart amazon-cloudwatch-agent.service")
   and (.commands[5] | contains("systemctl is-enabled amazon-cloudwatch-agent.service"))
   and (.commands[7] | contains("--since=-3min"))' \
  <<<"$parameters" >/dev/null \
  || fail "SSM 명령 JSON 계약이 올바르지 않습니다."

if "$COMMAND_BUILDER" 'invalid-sha' >/dev/null 2>&1; then
  fail "잘못된 commit SHA를 SSM 명령에 허용했습니다."
fi

# 공급망 검증과 장기 자격 증명 금지를 설치 계약으로 고정한다.
grep -F '937616F3450B7D806CBD9725D58167303B789C72' "$INSTALLER" >/dev/null \
  || fail "AWS 공개키 fingerprint 검증이 없습니다."
grep -F 'gpg --batch' "$INSTALLER" >/dev/null \
  || fail "CloudWatch Agent 패키지 서명 검증이 없습니다."
grep -F 'amazon-cloudwatch-agent-ctl' "$INSTALLER" >/dev/null \
  || fail "CloudWatch Agent 시작 명령이 없습니다."
grep -F 'systemctl enable amazon-cloudwatch-agent.service' "$INSTALLER" >/dev/null \
  || fail "재부팅 후 자동 시작 설정이 없습니다."

if grep -Eq 'AWS_(ACCESS_KEY_ID|SECRET_ACCESS_KEY)|aws_access_key_id|aws_secret_access_key' \
  "$CONFIG" "$INSTALLER" "$COMMAND_BUILDER" "$WORKFLOW"; then
  fail "장기 AWS 자격 증명 참조를 허용하지 않습니다."
fi

# 운영 EC2 변경은 develop 병합과 분리된 수동 승인 흐름으로만 실행한다.
trigger_block=$(awk '
  /^on:$/ { capture = 1; next }
  capture && /^[^[:space:]]/ { exit }
  capture { print }
' "$WORKFLOW")

grep -Eq '^  workflow_dispatch:' <<<"$trigger_block" \
  || fail "CloudWatch Agent 적용에는 workflow_dispatch가 필요합니다."
grep -Eq '^      confirm_production:$' <<<"$trigger_block" \
  || fail "CloudWatch Agent 적용에는 명시적인 운영 확인 입력이 필요합니다."
grep -Eq '^        type: boolean$' <<<"$trigger_block" \
  || fail "운영 확인 입력은 boolean이어야 합니다."
if grep -Eq '^  (push|pull_request|schedule):' <<<"$trigger_block"; then
  fail "CloudWatch Agent 적용은 자동 trigger를 허용하지 않습니다."
fi

grep -Eq '^    environment: production$' "$WORKFLOW" \
  || fail "CloudWatch Agent job은 production Environment를 사용해야 합니다."
grep -F 'AWS-RunShellScript' "$WORKFLOW" >/dev/null \
  || fail "SSM Run Command 적용 경로가 없습니다."
grep -F 'actions/checkout@' "$WORKFLOW" >/dev/null \
  || fail "SSM 명령 생성 스크립트를 위한 checkout 단계가 없습니다."
grep -F 'build-cloudwatch-agent-command.sh' "$WORKFLOW" >/dev/null \
  || fail "검증된 SSM 명령 생성 스크립트를 사용하지 않습니다."

echo "PASS: cloudwatch-agent"
