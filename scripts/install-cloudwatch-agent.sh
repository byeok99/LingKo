#!/usr/bin/env bash
# EC2 Instance Profile만 사용해 최소 OS 지표용 CloudWatch Agent를 설치·갱신한다.
set -Eeuo pipefail

EXPECTED_FINGERPRINT=937616F3450B7D806CBD9725D58167303B789C72
AWS_REGION=us-east-1
AGENT_CONTROL=/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl
AGENT_CONFIG=/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

fail() {
  echo "CLOUDWATCH_AGENT_ERROR: $*" >&2
  exit 1
}

[[ ${EUID:-$(id -u)} -eq 0 ]] || fail "run this script as root"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CONFIG_SOURCE="$REPO_ROOT/backend/aws/cloudwatch-agent.json"
[[ -f $CONFIG_SOURCE ]] || fail "configuration file is missing: $CONFIG_SOURCE"

for required_command in curl dpkg dpkg-query gpg install systemctl awk mktemp; do
  command -v "$required_command" >/dev/null || fail "missing command: $required_command"
done

if [[ ! -r /etc/os-release ]]; then
  fail "unable to identify the operating system"
fi
# shellcheck disable=SC1091
source /etc/os-release
[[ ${ID:-} == ubuntu ]] || fail "only Ubuntu is supported"

case "$(dpkg --print-architecture)" in
  amd64) PACKAGE_ARCH=amd64 ;;
  arm64) PACKAGE_ARCH=arm64 ;;
  *) fail "unsupported CPU architecture" ;;
esac

TEMP_DIR=$(mktemp -d)
cleanup() {
  rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

PACKAGE="$TEMP_DIR/amazon-cloudwatch-agent.deb"
SIGNATURE="$PACKAGE.sig"
PUBLIC_KEY="$TEMP_DIR/amazon-cloudwatch-agent.gpg"
KEYRING="$TEMP_DIR/keyring"
PACKAGE_BASE_URL="https://amazoncloudwatch-agent-${AWS_REGION}.s3.${AWS_REGION}.amazonaws.com/ubuntu/${PACKAGE_ARCH}/latest"

# TLS와 AWS 공개키 fingerprint를 모두 검증해 변조된 package를 설치하지 않는다.
curl --fail --location --silent --show-error --proto '=https' --tlsv1.2 \
  "$PACKAGE_BASE_URL/amazon-cloudwatch-agent.deb" --output "$PACKAGE"
curl --fail --location --silent --show-error --proto '=https' --tlsv1.2 \
  "$PACKAGE_BASE_URL/amazon-cloudwatch-agent.deb.sig" --output "$SIGNATURE"
curl --fail --location --silent --show-error --proto '=https' --tlsv1.2 \
  "https://amazoncloudwatch-agent.s3.amazonaws.com/assets/amazon-cloudwatch-agent.gpg" \
  --output "$PUBLIC_KEY"

install -d -m 0700 "$KEYRING"
gpg --batch --homedir "$KEYRING" --import "$PUBLIC_KEY" >/dev/null 2>&1
ACTUAL_FINGERPRINT=$(gpg --batch --homedir "$KEYRING" --with-colons --fingerprint \
  | awk -F: '$1 == "fpr" { print $10; exit }')
[[ $ACTUAL_FINGERPRINT == "$EXPECTED_FINGERPRINT" ]] \
  || fail "AWS signing key fingerprint mismatch"
gpg --batch --homedir "$KEYRING" --verify "$SIGNATURE" "$PACKAGE" >/dev/null 2>&1 \
  || fail "CloudWatch Agent package signature verification failed"

dpkg -i -E "$PACKAGE"

install -d -o root -g root -m 0755 "$(dirname -- "$AGENT_CONFIG")"
install -o root -g root -m 0644 "$CONFIG_SOURCE" "$AGENT_CONFIG"

# fetch-config가 schema를 검증하고 systemd service를 시작하므로 잘못된 설정은 즉시 실패한다.
"$AGENT_CONTROL" -a fetch-config -m ec2 -s -c "file:$AGENT_CONFIG"
systemctl enable amazon-cloudwatch-agent.service >/dev/null
systemctl is-active --quiet amazon-cloudwatch-agent.service \
  || fail "CloudWatch Agent service is not active"

echo "CLOUDWATCH_AGENT=active"
echo "CLOUDWATCH_AGENT_VERSION=$(dpkg-query -W -f='${Version}' amazon-cloudwatch-agent)"
echo "CLOUDWATCH_AGENT_CONFIG=$AGENT_CONFIG"
"$AGENT_CONTROL" -a status
