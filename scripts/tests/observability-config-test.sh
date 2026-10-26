#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)

python3 -m json.tool "$root_dir/backend/aws/cloudwatch-agent.json" >/dev/null
python3 -m json.tool "$root_dir/backend/aws/cloudwatch-dashboard.json.template" >/dev/null
python3 -m json.tool "$root_dir/backend/aws/s3-instance-role-policy.json.template" >/dev/null
bash -n "$root_dir/scripts/apply-cloudwatch-observability.sh"
bash -n "$root_dir/scripts/apply-ec2-s3-policy.sh"
[[ -x "$root_dir/scripts/verify-s3-instance-role.sh" ]]
bash -n "$root_dir/scripts/verify-s3-instance-role.sh"

grep -Fq '"statsd"' "$root_dir/backend/aws/cloudwatch-agent.json"
grep -Fq '"metric_separator": "_"' "$root_dir/backend/aws/cloudwatch-agent.json"
if grep -Eq '"(parse_data_dog_tags|percentiles)"' "$root_dir/backend/aws/cloudwatch-agent.json"; then
  echo 'CloudWatch Agent JSON contains unsupported StatsD properties' >&2
  exit 1
fi
grep -Fq 'DefaultCredentialsProvider.create()' "$root_dir/backend/src/main/java/com/lingko/lingko/core/config/S3Config.java"
for prefix in evaluation-audio guide-segments images videos; do
  grep -Fq "__BUCKET_ARN__/$prefix/*" "$root_dir/backend/aws/s3-instance-role-policy.json.template"
done

# CloudWatch Agent의 기본 metric_separator는 점을 밑줄로 변환한다. Dashboard와 Alarm이
# Micrometer 원본 이름을 조회하면 운영 지표가 있어도 빈 결과가 되므로 실제 export 이름을 고정한다.
for metric_name in \
  jvm_memory_used \
  statsd_http_server_requests_percentile_95 \
  lingko_evaluation_worker_heartbeat_age_seconds \
  statsd_lingko_evaluation_duration_percentile_95; do
  grep -Fq "$metric_name" "$root_dir/backend/aws/cloudwatch-dashboard.json.template"
done
grep -Fq -- '--metric-name lingko_evaluation_worker_heartbeat_age_seconds' \
  "$root_dir/scripts/apply-cloudwatch-observability.sh"
grep -Fq -- '--metric-name lingko_evaluation_oldest_pending_age_seconds' \
  "$root_dir/scripts/apply-cloudwatch-observability.sh"
grep -Fq -- '--metric-name lingko_evaluation_executions' \
  "$root_dir/scripts/apply-cloudwatch-observability.sh"
[[ $(grep -Fc 'Name=metric_type,Value=gauge' "$root_dir/scripts/apply-cloudwatch-observability.sh") -eq 2 ]]
[[ $(grep -Fc 'Name=metric_type,Value=counter' "$root_dir/scripts/apply-cloudwatch-observability.sh") -eq 1 ]]

# 컨테이너는 IMDSv2만 사용하며 host의 metadata hop limit 2 설정을 적용 script가 보장한다.
prod_compose="$root_dir/backend/docker-compose.prod.yml"
[[ $(grep -Fc 'AWS_EC2_METADATA_V1_DISABLED: "true"' "$prod_compose") -eq 3 ]]
grep -Fq 'modify-instance-metadata-options' "$root_dir/scripts/apply-ec2-s3-policy.sh"
grep -Fq -- '--http-tokens required' "$root_dir/scripts/apply-ec2-s3-policy.sh"
grep -Fq -- '--http-put-response-hop-limit 2' "$root_dir/scripts/apply-ec2-s3-policy.sh"
grep -Fq 's3ExternalIntegrationTest' "$root_dir/scripts/verify-s3-instance-role.sh"
grep -Fq 'AWS_EC2_METADATA_V1_DISABLED=true' "$root_dir/scripts/verify-s3-instance-role.sh"

if rg -n 'AWS_ACCESS_KEY|AWS_SECRET_KEY|StaticCredentialsProvider|AwsBasicCredentials' \
  "$root_dir/backend/src/main" "$root_dir/backend/.env.example" "$root_dir/backend/build.gradle"; then
  echo 'Long-lived AWS credential dependency remains' >&2
  exit 1
fi

DB_PASSWORD=test docker compose \
  -f "$root_dir/backend/docker-compose.yml" \
  -f "$root_dir/backend/docker-compose.prod.yml" config --quiet

echo 'observability configuration test: PASS'
