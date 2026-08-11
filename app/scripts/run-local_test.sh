#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script_under_test="$script_dir/run-local.sh"
temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT

mock_bin="$temp_dir/bin"
mkdir -p "$mock_bin"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'if [[ "${1:-}" == "devices" ]]; then' \
  '  if [[ -n "${ANDROID_BOOT_MARKER:-}" && -f "$ANDROID_BOOT_MARKER" ]]; then' \
  '    printf '\''[{"id":"%s"}]\n'\'' "$ANDROID_DEVICE_ID_FOR_TEST"' \
  '  else' \
  '    printf '\''[]\n'\''' \
  '  fi' \
  '  exit 0' \
  'fi' \
  'if [[ "${1:-}" == "emulators" ]]; then' \
  '  printf '\''flutter %s\n'\'' "$*" >> "$DEVICE_COMMANDS_OUTPUT"' \
  '  touch "$ANDROID_BOOT_MARKER"' \
  '  exit 0' \
  'fi' \
  'printf '\''%s\n'\'' "$@" > "$FLUTTER_ARGS_OUTPUT"' \
  > "$mock_bin/flutter"
chmod +x "$mock_bin/flutter"

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'printf '\''xcrun %s\n'\'' "$*" >> "$DEVICE_COMMANDS_OUTPUT"' \
  'if [[ "$*" == "simctl list devices "* ]]; then' \
  '  printf '\''    Test Simulator (%s) (%s)\n'\'' "$4" "${SIMULATOR_STATE:-Booted}"' \
  'fi' \
  > "$mock_bin/xcrun"
chmod +x "$mock_bin/xcrun"

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'printf '\''open %s\n'\'' "$*" >> "$DEVICE_COMMANDS_OUTPUT"' \
  > "$mock_bin/open"
chmod +x "$mock_bin/open"

assert_argument() {
  local output_file="$1"
  local expected_argument="$2"

  if ! grep -Fqx -- "$expected_argument" "$output_file"; then
    echo "Expected Flutter argument was not found: $expected_argument" >&2
    echo "Actual arguments:" >&2
    sed 's/^/  /' "$output_file" >&2
    exit 1
  fi
}

assert_command() {
  local output_file="$1"
  local expected_command="$2"

  if ! grep -Fqx -- "$expected_command" "$output_file"; then
    echo "Expected device command was not found: $expected_command" >&2
    exit 1
  fi
}

assert_command_absent() {
  local output_file="$1"
  local unexpected_command="$2"

  if grep -Fqx -- "$unexpected_command" "$output_file"; then
    echo "Unexpected device command was executed: $unexpected_command" >&2
    exit 1
  fi
}

run_with_clean_environment() {
  local env_file="$1"
  local output_file="$2"
  shift 2

  (
    unset DEVICE_ID IOS_DEVICE_ID ANDROID_DEVICE_ID ANDROID_EMULATOR_ID
    unset GOOGLE_SERVER_CLIENT_ID GOOGLE_ID LINGKO_API_BASE_URL API_URL
    unset ADMOB_ANDROID_REWARDED_AD_UNIT_ID ADMOB_IOS_REWARDED_AD_UNIT_ID
    unset SIMULATOR_STATE ANDROID_BOOT_MARKER ANDROID_DEVICE_ID_FOR_TEST
    export PATH="$mock_bin:$PATH"
    export LINGKO_ENV_FILE="$env_file"
    export FLUTTER_ARGS_OUTPUT="$output_file"
    export DEVICE_COMMANDS_OUTPUT="$temp_dir/default-device-commands.log"
    "$@"
  )
}

# 사용자는 별도 source 없이 프로젝트 로컬 설정만으로 앱을 실행할 수 있어야 한다.
auto_env_file="$temp_dir/auto.env"
auto_output_file="$temp_dir/auto.args"
printf '%s\n' \
  "GOOGLE_SERVER_CLIENT_ID='google-from-file'" \
  "LINGKO_API_BASE_URL='http://127.0.0.1:18080'" \
  "ADMOB_IOS_REWARDED_AD_UNIT_ID='ios-ad-from-file'" \
  "IOS_DEVICE_ID='ios-device-from-file'" \
  > "$auto_env_file"

run_with_clean_environment \
  "$auto_env_file" \
  "$auto_output_file" \
  "$script_under_test" ios

assert_argument "$auto_output_file" \
  '--dart-define=GOOGLE_SERVER_CLIENT_ID=google-from-file'
assert_argument "$auto_output_file" \
  '--dart-define=LINGKO_API_BASE_URL=http://127.0.0.1:18080'
assert_argument "$auto_output_file" \
  '--dart-define=ADMOB_IOS_REWARDED_AD_UNIT_ID=ios-ad-from-file'
assert_argument "$auto_output_file" '-d'
assert_argument "$auto_output_file" 'ios-device-from-file'

# 꺼진 iOS Simulator는 부팅하고 Simulator 앱을 연 뒤 완전히 준비될 때까지 기다려야 한다.
ios_boot_output_file="$temp_dir/ios-boot.args"
ios_boot_commands_file="$temp_dir/ios-boot.commands"
run_with_clean_environment \
  "$auto_env_file" \
  "$ios_boot_output_file" \
  env \
    SIMULATOR_STATE='Shutdown' \
    DEVICE_COMMANDS_OUTPUT="$ios_boot_commands_file" \
    "$script_under_test" ios

assert_command "$ios_boot_commands_file" \
  'xcrun simctl boot ios-device-from-file'
assert_command "$ios_boot_commands_file" 'open -a Simulator'
assert_command "$ios_boot_commands_file" \
  'xcrun simctl bootstatus ios-device-from-file -b'

# 이미 부팅된 iOS Simulator에는 중복 boot 명령을 보내지 않아야 한다.
ios_ready_output_file="$temp_dir/ios-ready.args"
ios_ready_commands_file="$temp_dir/ios-ready.commands"
run_with_clean_environment \
  "$auto_env_file" \
  "$ios_ready_output_file" \
  env \
    SIMULATOR_STATE='Booted' \
    DEVICE_COMMANDS_OUTPUT="$ios_ready_commands_file" \
    "$script_under_test" ios

assert_command_absent "$ios_ready_commands_file" \
  'xcrun simctl boot ios-device-from-file'

# Android도 같은 로컬 파일에서 플랫폼 전용 기기와 광고 설정을 선택해야 한다.
android_env_file="$temp_dir/android.env"
android_output_file="$temp_dir/android.args"
printf '%s\n' \
  "GOOGLE_SERVER_CLIENT_ID='google-from-file'" \
  "ANDROID_DEVICE_ID='android-device-from-file'" \
  "ADMOB_ANDROID_REWARDED_AD_UNIT_ID='android-ad-from-file'" \
  > "$android_env_file"

run_with_clean_environment \
  "$android_env_file" \
  "$android_output_file" \
  "$script_under_test" android

assert_argument "$android_output_file" 'android-device-from-file'
assert_argument "$android_output_file" \
  '--dart-define=LINGKO_API_BASE_URL=http://10.0.2.2:8080'
assert_argument "$android_output_file" \
  '--dart-define=ADMOB_ANDROID_REWARDED_AD_UNIT_ID=android-ad-from-file'

# 꺼진 Android emulator는 AVD 이름으로 실행한 뒤 지정한 Device ID가 나타날 때까지 기다려야 한다.
android_boot_env_file="$temp_dir/android-boot.env"
android_boot_output_file="$temp_dir/android-boot.args"
android_boot_commands_file="$temp_dir/android-boot.commands"
android_boot_marker="$temp_dir/android-booted"
printf '%s\n' \
  "GOOGLE_SERVER_CLIENT_ID='google-from-file'" \
  "ANDROID_DEVICE_ID='emulator-5554'" \
  "ANDROID_EMULATOR_ID='Galaxy_Test_API_36'" \
  > "$android_boot_env_file"

run_with_clean_environment \
  "$android_boot_env_file" \
  "$android_boot_output_file" \
  env \
    ANDROID_BOOT_MARKER="$android_boot_marker" \
    ANDROID_DEVICE_ID_FOR_TEST='emulator-5554' \
    DEVICE_COMMANDS_OUTPUT="$android_boot_commands_file" \
    "$script_under_test" android

assert_command "$android_boot_commands_file" \
  'flutter emulators --launch Galaxy_Test_API_36'
assert_argument "$android_boot_output_file" 'emulator-5554'

# 일회성 환경변수는 로컬 파일보다 우선해 실행 환경을 안전하게 덮어쓸 수 있어야 한다.
override_env_file="$temp_dir/override.env"
override_output_file="$temp_dir/override.args"
printf '%s\n' \
  "GOOGLE_SERVER_CLIENT_ID='google-from-file'" \
  "ADMOB_IOS_REWARDED_AD_UNIT_ID='ios-ad-from-file'" \
  > "$override_env_file"

run_with_clean_environment \
  "$override_env_file" \
  "$override_output_file" \
  env \
    GOOGLE_SERVER_CLIENT_ID='google-from-shell' \
    ADMOB_IOS_REWARDED_AD_UNIT_ID='ios-ad-from-shell' \
    "$script_under_test" ios

assert_argument "$override_output_file" \
  '--dart-define=GOOGLE_SERVER_CLIENT_ID=google-from-shell'
assert_argument "$override_output_file" \
  '--dart-define=ADMOB_IOS_REWARDED_AD_UNIT_ID=ios-ad-from-shell'

echo 'run-local.sh tests passed'
