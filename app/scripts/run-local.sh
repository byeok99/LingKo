#!/usr/bin/env bash

set -euo pipefail

platform="${1:-}"
device_id="${DEVICE_ID:-${2:-}}"
google_server_client_id="${GOOGLE_SERVER_CLIENT_ID:-${GOOGLE_ID:-}}"
api_url="${LINGKO_API_BASE_URL:-${API_URL:-}}"
android_rewarded_ad_unit_id="${ADMOB_ANDROID_REWARDED_AD_UNIT_ID:-}"
ios_rewarded_ad_unit_id="${ADMOB_IOS_REWARDED_AD_UNIT_ID:-}"

usage() {
  cat <<'EOF'
Usage:
  GOOGLE_SERVER_CLIENT_ID=<web-client-id> [DEVICE_ID=<device-id>] ./scripts/run-local.sh ios
  GOOGLE_SERVER_CLIENT_ID=<web-client-id> [DEVICE_ID=<device-id>] ./scripts/run-local.sh android

Optional aliases:
  GOOGLE_ID=<web-client-id>
  API_URL=<backend-url>
  ADMOB_ANDROID_REWARDED_AD_UNIT_ID=<android-rewarded-ad-unit-id>
  ADMOB_IOS_REWARDED_AD_UNIT_ID=<ios-rewarded-ad-unit-id>

Defaults:
  ios     -> http://localhost:8080
  android -> http://10.0.2.2:8080
EOF
}

case "$platform" in
  ios)
    api_url="${api_url:-http://localhost:8080}"
    # CocoaPods의 Ruby 문자열 정규화가 macOS에서 지원하지 않는 C.UTF-8을 받지 않도록 UTF-8 locale을 고정한다.
    export LANG=en_US.UTF-8
    export LC_ALL=en_US.UTF-8
    ;;
  android)
    api_url="${api_url:-http://10.0.2.2:8080}"
    ;;
  *)
    usage
    exit 64
    ;;
esac

if [[ -z "$google_server_client_id" ]]; then
  echo "GOOGLE_SERVER_CLIENT_ID or GOOGLE_ID is required." >&2
  exit 64
fi

device_args=()
if [[ -n "$device_id" ]]; then
  device_args=(-d "$device_id")
fi

echo "Launching LingKo on $platform with API $api_url"
exec flutter run \
  "${device_args[@]}" \
  --dart-define="GOOGLE_SERVER_CLIENT_ID=$google_server_client_id" \
  --dart-define="LINGKO_API_BASE_URL=$api_url" \
  --dart-define="ADMOB_ANDROID_REWARDED_AD_UNIT_ID=$android_rewarded_ad_unit_id" \
  --dart-define="ADMOB_IOS_REWARDED_AD_UNIT_ID=$ios_rewarded_ad_unit_id"
