#!/usr/bin/env bash
# CloudShell에서 Dashboard와 핵심 Alarm을 동일 입력으로 재현한다.
set -euo pipefail

region=${AWS_REGION:-us-east-1}
instance_id=${EC2_INSTANCE_ID:?EC2_INSTANCE_ID is required}
topic_arn=${ALERT_TOPIC_ARN:?ALERT_TOPIC_ARN is required}
expected_account_id=${EXPECTED_AWS_ACCOUNT_ID:?EXPECTED_AWS_ACCOUNT_ID is required}
confirm_production=${CONFIRM_PRODUCTION:-false}
root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
template="$root_dir/backend/aws/cloudwatch-dashboard.json.template"

for required_command in aws jq sed mktemp; do
  command -v "$required_command" >/dev/null || {
    echo "Missing command: $required_command" >&2
    exit 1
  }
done
dashboard=$(mktemp)
trap 'rm -f "$dashboard"' EXIT

[[ $confirm_production == true ]] || { echo "CONFIRM_PRODUCTION=true is required" >&2; exit 1; }
[[ $expected_account_id =~ ^[0-9]{12}$ ]] || { echo "Invalid EXPECTED_AWS_ACCOUNT_ID" >&2; exit 1; }
[[ $instance_id =~ ^i-([0-9a-f]{8}|[0-9a-f]{17})$ ]] || { echo "Invalid EC2_INSTANCE_ID" >&2; exit 1; }

account_id=$(aws sts get-caller-identity --query Account --output text)
[[ $account_id == "$expected_account_id" ]] || {
  echo "AWS account does not match EXPECTED_AWS_ACCOUNT_ID" >&2
  exit 1
}
[[ $topic_arn == "arn:aws:sns:$region:$account_id:lingko-prod-alerts" ]] || {
  echo "ALERT_TOPIC_ARN must reference lingko-prod-alerts in $region" >&2
  exit 1
}
aws sns get-topic-attributes --region "$region" --topic-arn "$topic_arn" >/dev/null

instance_state=$(aws ec2 describe-instances \
  --region "$region" \
  --instance-ids "$instance_id" \
  --query 'Reservations[0].Instances[0].State.Name' \
  --output text)
[[ $instance_state == running ]] || {
  echo "EC2 instance must be running" >&2
  exit 1
}

require_metric() {
  local namespace=$1
  local metric_name=$2
  shift 2
  local count
  count=$(aws cloudwatch list-metrics \
    --region "$region" \
    --namespace "$namespace" \
    --metric-name "$metric_name" \
    --dimensions "$@" \
    --recently-active PT3H \
    --query 'length(Metrics)' \
    --output text)
  [[ $count -gt 0 ]] || {
    echo "Required metric is not active: $namespace/$metric_name" >&2
    exit 1
  }
}

# 빈 Dashboard와 즉시 ALARM 상태가 되는 Worker 알림을 만들지 않도록 연속 지표를 먼저 확인한다.
require_metric CWAgent mem_used_percent Name=InstanceId,Value="$instance_id"
require_metric CWAgent disk_used_percent \
  Name=InstanceId,Value="$instance_id" Name=path,Value=/ Name=fstype,Value=ext4
for service in api worker; do
  require_metric CWAgent jvm_memory_used \
    Name=InstanceId,Value="$instance_id" Name=environment,Value=prod \
    Name=service,Value="$service" Name=metric_type,Value=gauge
done
require_metric CWAgent lingko_evaluation_worker_heartbeat_age_seconds \
  Name=InstanceId,Value="$instance_id" Name=environment,Value=prod \
  Name=service,Value=worker Name=metric_type,Value=gauge

sed -e "s/__REGION__/$region/g" -e "s/__INSTANCE_ID__/$instance_id/g" "$template" > "$dashboard"
aws cloudwatch put-dashboard --region "$region" --dashboard-name LingKo-Prod --dashboard-body "file://$dashboard"

put_alarm() {
  aws cloudwatch put-metric-alarm --region "$region" --alarm-actions "$topic_arn" --ok-actions "$topic_arn" "$@"
}

put_alarm --alarm-name lingko-prod-ec2-memory-warning --namespace CWAgent --metric-name mem_used_percent \
  --dimensions Name=InstanceId,Value="$instance_id" --statistic Average --period 60 \
  --evaluation-periods 5 --datapoints-to-alarm 3 --threshold 80 --comparison-operator GreaterThanOrEqualToThreshold \
  --treat-missing-data missing
put_alarm --alarm-name lingko-prod-agent-metrics-missing --namespace CWAgent --metric-name mem_used_percent \
  --dimensions Name=InstanceId,Value="$instance_id" --statistic Average --period 60 \
  --evaluation-periods 3 --datapoints-to-alarm 3 --threshold 100 --comparison-operator GreaterThanThreshold \
  --treat-missing-data breaching
put_alarm --alarm-name lingko-prod-ec2-disk-warning --namespace CWAgent --metric-name disk_used_percent \
  --dimensions Name=InstanceId,Value="$instance_id" Name=path,Value=/ Name=fstype,Value=ext4 --statistic Average --period 60 \
  --evaluation-periods 3 --datapoints-to-alarm 2 --threshold 80 --comparison-operator GreaterThanOrEqualToThreshold \
  --treat-missing-data missing
put_alarm --alarm-name lingko-prod-ec2-disk-critical --namespace CWAgent --metric-name disk_used_percent \
  --dimensions Name=InstanceId,Value="$instance_id" Name=path,Value=/ Name=fstype,Value=ext4 --statistic Average --period 60 \
  --evaluation-periods 2 --datapoints-to-alarm 2 --threshold 90 --comparison-operator GreaterThanOrEqualToThreshold \
  --treat-missing-data missing
put_alarm --alarm-name lingko-prod-ec2-status-critical --namespace AWS/EC2 --metric-name StatusCheckFailed \
  --dimensions Name=InstanceId,Value="$instance_id" --statistic Maximum --period 60 \
  --evaluation-periods 2 --datapoints-to-alarm 2 --threshold 1 --comparison-operator GreaterThanOrEqualToThreshold \
  --treat-missing-data missing
put_alarm --alarm-name lingko-prod-worker-heartbeat-critical --namespace CWAgent \
  --metric-name lingko_evaluation_worker_heartbeat_age_seconds \
  --dimensions Name=InstanceId,Value="$instance_id" Name=environment,Value=prod Name=service,Value=worker \
  Name=metric_type,Value=gauge \
  --statistic Maximum --period 60 --evaluation-periods 3 --datapoints-to-alarm 3 --threshold 900 \
  --comparison-operator GreaterThanOrEqualToThreshold --treat-missing-data breaching
put_alarm --alarm-name lingko-prod-evaluation-backlog-warning --namespace CWAgent \
  --metric-name lingko_evaluation_oldest_pending_age_seconds \
  --dimensions Name=InstanceId,Value="$instance_id" Name=environment,Value=prod Name=service,Value=worker \
  Name=metric_type,Value=gauge \
  --statistic Maximum --period 60 --evaluation-periods 3 --datapoints-to-alarm 2 --threshold 900 \
  --comparison-operator GreaterThanOrEqualToThreshold --treat-missing-data missing
put_alarm --alarm-name lingko-prod-evaluation-terminal-failure-warning --namespace CWAgent \
  --metric-name lingko_evaluation_executions \
  --dimensions Name=InstanceId,Value="$instance_id" Name=environment,Value=prod Name=service,Value=worker \
  Name=metric_type,Value=counter Name=outcome,Value=terminal_failure \
  --statistic Sum --period 300 --evaluation-periods 1 --datapoints-to-alarm 1 --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold --treat-missing-data notBreaching

alarm_names=(
  lingko-prod-ec2-memory-warning
  lingko-prod-agent-metrics-missing
  lingko-prod-ec2-disk-warning
  lingko-prod-ec2-disk-critical
  lingko-prod-ec2-status-critical
  lingko-prod-worker-heartbeat-critical
  lingko-prod-evaluation-backlog-warning
  lingko-prod-evaluation-terminal-failure-warning
)
aws cloudwatch get-dashboard --region "$region" --dashboard-name LingKo-Prod >/dev/null
alarms=$(aws cloudwatch describe-alarms \
  --region "$region" \
  --alarm-names "${alarm_names[@]}" \
  --query 'MetricAlarms' \
  --output json)
jq -e --arg topic "$topic_arn" '
  length == 8 and
  all(.[]; (.ActionsEnabled == true) and
           (.AlarmActions | index($topic) != null) and
           (.OKActions | index($topic) != null))
' <<<"$alarms" >/dev/null || {
  echo "Dashboard or Alarm postcondition verification failed" >&2
  exit 1
}

echo "Applied and verified LingKo-Prod dashboard and core alarms in $region"
jq -r '.[] | [.AlarmName, .StateValue] | @tsv' <<<"$alarms"
