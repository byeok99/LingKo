#!/usr/bin/env bash
# CloudShell에서 EC2 role에 LingKo 전용 S3 prefix 권한을 idempotent하게 적용한다.
set -euo pipefail

role_name=${LINGKO_EC2_ROLE_NAME:-LingKoEc2SsmRole}
bucket_name=${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}
policy_name=${LINGKO_S3_POLICY_NAME:-LingKoS3RuntimeAccess}
region=${AWS_REGION:-us-east-1}
instance_id=${EC2_INSTANCE_ID:?EC2_INSTANCE_ID is required}
expected_account_id=${EXPECTED_AWS_ACCOUNT_ID:?EXPECTED_AWS_ACCOUNT_ID is required}
confirm_production=${CONFIRM_PRODUCTION:-false}
root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
template="$root_dir/backend/aws/s3-instance-role-policy.json.template"

for required_command in aws jq sed mktemp; do
  command -v "$required_command" >/dev/null || {
    echo "Missing command: $required_command" >&2
    exit 1
  }
done
rendered=$(mktemp)
trap 'rm -f "$rendered"' EXIT

[[ $confirm_production == true ]] || {
  echo "CONFIRM_PRODUCTION=true is required" >&2
  exit 1
}
[[ $expected_account_id =~ ^[0-9]{12}$ ]] || {
  echo "Invalid EXPECTED_AWS_ACCOUNT_ID" >&2
  exit 1
}
[[ $instance_id =~ ^i-([0-9a-f]{8}|[0-9a-f]{17})$ ]] || {
  echo "Invalid EC2_INSTANCE_ID" >&2
  exit 1
}
[[ $bucket_name =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]] || {
  echo "Invalid S3 bucket name" >&2
  exit 1
}

account_id=$(aws sts get-caller-identity --query Account --output text)
[[ $account_id == "$expected_account_id" ]] || {
  echo "AWS account does not match EXPECTED_AWS_ACCOUNT_ID" >&2
  exit 1
}

# policy 대상 role이 실제 운영 instance profile에 포함됐는지 확인해 다른 role 오적용을 막는다.
profile_arn=$(aws ec2 describe-instances \
  --region "$region" \
  --instance-ids "$instance_id" \
  --query 'Reservations[0].Instances[0].IamInstanceProfile.Arn' \
  --output text)
[[ $profile_arn == arn:aws:iam::"$account_id":instance-profile/* ]] || {
  echo "EC2 instance profile is missing or belongs to another account" >&2
  exit 1
}
profile_name=${profile_arn##*/}
profile_json=$(aws iam get-instance-profile --instance-profile-name "$profile_name" --output json)
jq -e --arg role "$role_name" '.InstanceProfile.Roles | any(.RoleName == $role)' \
  <<<"$profile_json" >/dev/null || {
  echo "EC2 instance profile does not contain $role_name" >&2
  exit 1
}

sed "s|__BUCKET_ARN__|arn:aws:s3:::$bucket_name|g" "$template" > "$rendered"
aws iam put-role-policy \
  --role-name "$role_name" \
  --policy-name "$policy_name" \
  --policy-document "file://$rendered"

# Docker bridge를 한 hop 거쳐도 SDK가 임시 role 자격 증명을 받도록 hop limit은 2로 두되,
# IMDSv1은 instance와 JVM 양쪽에서 차단한다.
aws ec2 modify-instance-metadata-options \
  --region "$region" \
  --instance-id "$instance_id" \
  --http-endpoint enabled \
  --http-tokens required \
  --http-put-response-hop-limit 2 >/dev/null

for _ in $(seq 1 30); do
  metadata_options=$(aws ec2 describe-instances \
    --region "$region" \
    --instance-ids "$instance_id" \
    --query 'Reservations[0].Instances[0].MetadataOptions' \
    --output json)
  if jq -e '.State == "applied" and .HttpEndpoint == "enabled" and .HttpTokens == "required" and .HttpPutResponseHopLimit == 2' \
    <<<"$metadata_options" >/dev/null; then
    echo "Applied $policy_name and IMDSv2-only container metadata settings"
    exit 0
  fi
  sleep 2
done

echo "Timed out waiting for EC2 metadata options" >&2
exit 1
