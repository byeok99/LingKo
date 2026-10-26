#!/usr/bin/env bash
# 운영 EC2의 Instance Profile만 전달한 격리 container에서 S3 전체 생명주기를 검증한다.
set -Eeuo pipefail

bucket_name=${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}
s3_region=${AWS_S3_REGION:?AWS_S3_REGION is required}
expected_role=${LINGKO_EC2_ROLE_NAME:-LingKoEc2SsmRole}
confirm_production=${CONFIRM_PRODUCTION:-false}
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/.." && pwd)
backend_dir="$repo_root/backend"

fail() {
  echo "S3_ROLE_VERIFY_ERROR: $*" >&2
  exit 1
}

[[ $confirm_production == true ]] || fail "CONFIRM_PRODUCTION=true is required"
[[ $bucket_name =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]] || fail "invalid S3 bucket name"
[[ $s3_region =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]] || fail "invalid AWS_S3_REGION"
command -v curl >/dev/null || fail "missing command: curl"
command -v docker >/dev/null || fail "missing command: docker"
command -v git >/dev/null || fail "missing command: git"
docker info >/dev/null 2>&1 || fail "Docker daemon is unavailable"

# Host가 의도한 role을 가진 EC2인지 비밀 자격 증명 없이 IMDSv2로 확인한다.
token=$(curl --fail --silent --show-error --max-time 2 \
  -X PUT \
  -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' \
  http://169.254.169.254/latest/api/token) || fail "IMDSv2 token request failed"
role=$(curl --fail --silent --show-error --max-time 2 \
  -H "X-aws-ec2-metadata-token: $token" \
  http://169.254.169.254/latest/meta-data/iam/security-credentials/) \
  || fail "Instance Profile role lookup failed"
[[ $role == "$expected_role" ]] || fail "unexpected Instance Profile role"

sha=$(git -C "$repo_root" rev-parse HEAD)
[[ $sha =~ ^[0-9a-f]{40}$ ]] || fail "invalid repository commit"
test_image="lingko-s3-role-check:$sha"

docker build --target build --tag "$test_image" "$backend_dir"

# 어떤 host AWS 환경변수나 .env도 전달하지 않아 성공 시 IMDS role 사용이 입증된다.
docker run --rm \
  --entrypoint gradle \
  -e AWS_S3_BUCKET="$bucket_name" \
  -e AWS_S3_REGION="$s3_region" \
  -e AWS_EC2_METADATA_V1_DISABLED=true \
  "$test_image" \
  --no-daemon s3ExternalIntegrationTest

echo "S3_INSTANCE_ROLE_VERIFY=PASS"
echo "ROLE=$role"
echo "COMMIT=$sha"
