#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
env_file="${LINGKO_ENV_FILE:-$script_dir/../.env.local}"

# 로컬 기본값을 자동으로 읽되, 명령 실행 시 명시한 환경변수는 파일 값보다 우선한다.
device_id_override_set="${DEVICE_ID+x}"
device_id_override="${DEVICE_ID-}"
ios_device_id_override_set="${IOS_DEVICE_ID+x}"
ios_device_id_override="${IOS_DEVICE_ID-}"
android_device_id_override_set="${ANDROID_DEVICE_ID+x}"
android_device_id_override="${ANDROID_DEVICE_ID-}"
android_emulator_id_override_set="${ANDROID_EMULATOR_ID+x}"
android_emulator_id_override="${ANDROID_EMULATOR_ID-}"
google_server_client_id_override_set="${GOOGLE_SERVER_CLIENT_ID+x}"
google_server_client_id_override="${GOOGLE_SERVER_CLIENT_ID-}"
google_id_override_set="${GOOGLE_ID+x}"
google_id_override="${GOOGLE_ID-}"
api_base_url_override_set="${LINGKO_API_BASE_URL+x}"
api_base_url_override="${LINGKO_API_BASE_URL-}"
api_url_override_set="${API_URL+x}"
api_url_override="${API_URL-}"
android_ad_unit_override_set="${ADMOB_ANDROID_REWARDED_AD_UNIT_ID+x}"
android_ad_unit_override="${ADMOB_ANDROID_REWARDED_AD_UNIT_ID-}"
ios_ad_unit_override_set="${ADMOB_IOS_REWARDED_AD_UNIT_ID+x}"
ios_ad_unit_override="${ADMOB_IOS_REWARDED_AD_UNIT_ID-}"

if [[ -f "$env_file" ]]; then
  set -a
  # 프로젝트 소유자가 관리하며 Git에서 제외된 로컬 shell 설정 파일만 현재 프로세스에 적용한다.
  # shellcheck source=/dev/null
  source "$env_file"
  set +a
fi

restore_override() {
  local variable_name="$1"
  local was_set="$2"
  local value="$3"

  if [[ "$was_set" == "x" ]]; then
    printf -v "$variable_name" '%s' "$value"
    export "$variable_name"
  fi
}

restore_override DEVICE_ID "$device_id_override_set" "$device_id_override"
restore_override IOS_DEVICE_ID \
  "$ios_device_id_override_set" "$ios_device_id_override"
restore_override ANDROID_DEVICE_ID \
  "$android_device_id_override_set" "$android_device_id_override"
restore_override ANDROID_EMULATOR_ID \
  "$android_emulator_id_override_set" "$android_emulator_id_override"
restore_override GOOGLE_SERVER_CLIENT_ID \
  "$google_server_client_id_override_set" "$google_server_client_id_override"
restore_override GOOGLE_ID "$google_id_override_set" "$google_id_override"
restore_override LINGKO_API_BASE_URL \
  "$api_base_url_override_set" "$api_base_url_override"
restore_override API_URL "$api_url_override_set" "$api_url_override"
restore_override ADMOB_ANDROID_REWARDED_AD_UNIT_ID \
  "$android_ad_unit_override_set" "$android_ad_unit_override"
restore_override ADMOB_IOS_REWARDED_AD_UNIT_ID \
  "$ios_ad_unit_override_set" "$ios_ad_unit_override"

platform="${1:-}"
requested_device_id="${DEVICE_ID:-${2:-}}"
google_server_client_id="${GOOGLE_SERVER_CLIENT_ID:-${GOOGLE_ID:-}}"
api_url="${LINGKO_API_BASE_URL:-${API_URL:-}}"
android_rewarded_ad_unit_id="${ADMOB_ANDROID_REWARDED_AD_UNIT_ID:-}"
ios_rewarded_ad_unit_id="${ADMOB_IOS_REWARDED_AD_UNIT_ID:-}"
android_emulator_id="${ANDROID_EMULATOR_ID:-}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/run-local.sh ios
  ./scripts/run-local.sh android

Local configuration:
  app/.env.local is loaded automatically when present.
  Explicit environment variables override values from the local file.

Optional aliases:
  GOOGLE_ID=<web-client-id>
  API_URL=<backend-url>
  IOS_DEVICE_ID=<ios-simulator-or-device-id>
  ANDROID_DEVICE_ID=<android-emulator-or-device-id>
  ANDROID_EMULATOR_ID=<flutter-avd-id>
  ADMOB_ANDROID_REWARDED_AD_UNIT_ID=<android-rewarded-ad-unit-id>
  ADMOB_IOS_REWARDED_AD_UNIT_ID=<ios-rewarded-ad-unit-id>

Defaults:
  ios     -> http://localhost:8080
  android -> http://10.0.2.2:8080
EOF
}

case "$platform" in
  ios)
    device_id="${requested_device_id:-${IOS_DEVICE_ID:-}}"
    api_url="${api_url:-http://localhost:8080}"
    # CocoaPods의 Ruby 문자열 정규화가 macOS에서 지원하지 않는 C.UTF-8을 받지 않도록 UTF-8 locale을 고정한다.
    export LANG=en_US.UTF-8
    export LC_ALL=en_US.UTF-8
    ;;
  android)
    device_id="${requested_device_id:-${ANDROID_DEVICE_ID:-}}"
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

ensure_ios_simulator_ready() {
  local simulator_id="$1"
  local simulator_line

  [[ -n "$simulator_id" ]] || return 0
  command -v xcrun >/dev/null 2>&1 || return 0

  simulator_line="$(
    xcrun simctl list devices "$simulator_id" 2>/dev/null \
      | grep -F "$simulator_id" \
      | head -n 1 \
      || true
  )"

  # simctl 목록에 없으면 실제 iPhone 같은 비-Simulator 기기일 수 있으므로 Flutter에 판단을 맡긴다.
  [[ -n "$simulator_line" ]] || return 0
  [[ "$simulator_line" != *"(Booted)"* ]] || return 0

  if [[ "$simulator_line" == *"(Shutdown)"* ]]; then
    echo "Booting iOS Simulator $simulator_id"
    xcrun simctl boot "$simulator_id"
    if command -v open >/dev/null 2>&1; then
      open -a Simulator >/dev/null 2>&1 || true
    fi
  fi

  # Flutter가 기기를 탐색하기 전에 Simulator의 boot sequence가 완료되어야 한다.
  xcrun simctl bootstatus "$simulator_id" -b
}

android_device_is_available() {
  local target_device_id="$1"

  flutter devices --machine --device-timeout 2 2>/dev/null \
    | grep -Fq "$target_device_id"
}

ensure_android_emulator_ready() {
  local target_device_id="$1"
  local target_emulator_id="$2"
  local attempt=0

  [[ "$target_device_id" == emulator-* ]] || return 0
  [[ -n "$target_emulator_id" ]] || return 0
  android_device_is_available "$target_device_id" && return 0

  echo "Booting Android emulator $target_emulator_id"
  flutter emulators --launch "$target_emulator_id"

  while (( attempt < 30 )); do
    if android_device_is_available "$target_device_id"; then
      return 0
    fi
    sleep 1
    attempt=$((attempt + 1))
  done

  echo "Android emulator did not become available: $target_device_id" >&2
  exit 69
}

case "$platform" in
  ios)
    ensure_ios_simulator_ready "$device_id"
    ;;
  android)
    ensure_android_emulator_ready "$device_id" "$android_emulator_id"
    ;;
esac

echo "Launching LingKo on $platform with API $api_url"
flutter_args=(
  --dart-define="GOOGLE_SERVER_CLIENT_ID=$google_server_client_id" \
  --dart-define="LINGKO_API_BASE_URL=$api_url" \
  --dart-define="ADMOB_ANDROID_REWARDED_AD_UNIT_ID=$android_rewarded_ad_unit_id" \
  --dart-define="ADMOB_IOS_REWARDED_AD_UNIT_ID=$ios_rewarded_ad_unit_id"
)

if [[ -n "$device_id" ]]; then
  flutter_args=(-d "$device_id" "${flutter_args[@]}")
fi

exec flutter run "${flutter_args[@]}"
