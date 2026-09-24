#!/usr/bin/env bash
# 검증된 develop commit에만 CloudWatch Agent를 적용하는 SSM parameters JSON을 생성한다.
set -euo pipefail

commit_sha=${1:-}

if [[ ! $commit_sha =~ ^[0-9a-f]{40}$ ]]; then
  echo "CLOUDWATCH_COMMAND_ERROR: invalid commit SHA" >&2
  exit 2
fi

command -v jq >/dev/null || {
  echo "CLOUDWATCH_COMMAND_ERROR: missing command: jq" >&2
  exit 1
}

# git 동기화는 ubuntu 소유권을 유지하고 Agent 설치·systemd 검증만 root로 실행한다.
jq -cn --arg sha "$commit_sha" '{
  commands: [
    ("sudo -u ubuntu -H bash -lc " + (
      [
        "set -euo pipefail",
        "cd /home/ubuntu/LingKo",
        "test -z \"$(git status --porcelain --untracked-files=no)\"",
        "git fetch --prune origin develop",
        ("test \"$(git rev-parse origin/develop)\" = \"" + $sha + "\""),
        "git switch develop",
        ("git merge --ff-only \"" + $sha + "\"")
      ] | join("; ") | @sh
    )),
    "sudo /home/ubuntu/LingKo/scripts/install-cloudwatch-agent.sh",
    "sudo systemctl restart amazon-cloudwatch-agent.service",
    "sleep 15",
    "sudo systemctl is-active --quiet amazon-cloudwatch-agent.service",
    "test \"$(sudo systemctl is-enabled amazon-cloudwatch-agent.service)\" = enabled",
    "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a status",
    "sudo journalctl -u amazon-cloudwatch-agent.service --since=-3min --no-pager"
  ],
  executionTimeout: ["900"]
}'
