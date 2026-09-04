# 작업 이력

## 2026-09-04 - AdMob test device 로컬 설정 전달

- 변경 파일: `run-local.sh`, `run-local_test.sh`, `WORK_LOG.md`
- 내용: Git에서 제외된 `.env.local`의 `ADMOB_TEST_DEVICE_ID`를 명시적 `--dart-define`으로 전달하고 shell 환경 override 우선순위를 유지했다.
- 검증: 구현 전 인자 누락 RED 확인, `bash scripts/run-local_test.sh`·`bash -n` 통과
- 리스크: 실제 test device ID는 개발자의 `.env.local`에 별도 입력 필요

## 2026-08-11 - 로컬 실행 설정 자동 로딩

- 변경 파일: `run-local.sh`, `run-local_test.sh`, `WORK_LOG.md`
- 내용: `app/.env.local`을 별도 `source` 없이 자동 로딩하고 플랫폼별 Device ID를 선택한다. 꺼진 iOS Simulator와 연결되지 않은 Android AVD를 자동 부팅하며, 명령에서 직접 전달한 환경변수의 우선순위를 보장했다.
- 검증: 자동 부팅 shell 테스트, Bash 구문 검사, 종료된 `iPhone 15 LingKo`의 실제 부팅·Xcode build·앱 실행 성공
- 리스크: Android는 AVD 실행 후 설정한 `ANDROID_DEVICE_ID`가 30초 안에 나타나지 않으면 종료한다

## 2026-08-08 - 로컬 실행에 광고 단위 ID 전달 추가

- 변경 파일: `run-local.sh`, `WORK_LOG.md`
- 내용: Android/iOS Rewarded Ad Unit ID 환경변수를 `--dart-define`으로 전달한다.
- 검증: `bash -n run-local.sh`
- 리스크: 환경변수가 비어 있으면 광고 버튼이 숨겨진다

## 2026-07-27 - iOS CocoaPods UTF-8 실행 환경 고정

- 변경 파일: `run-local.sh`, `WORK_LOG.md`
- 내용: macOS에서 `C.UTF-8`로 Flutter를 실행할 때 CocoaPods가 `ASCII-8BIT` 정규화 오류로 중단되지 않도록 iOS 실행 경로의 locale을 `en_US.UTF-8`로 고정했다.
- 검증: `bash -n scripts/run-local.sh`, `flutter build ios --debug --no-codesign` 통과
- 리스크: 스크립트를 거치지 않고 직접 `flutter run`할 때는 셸 locale 설정이 별도로 필요함

## 2026-07-27 - iOS·Android 로컬 실행 스크립트 추가

- 변경 파일: `run-local.sh`, `WORK_LOG.md`
- 내용: Web OAuth Client ID와 선택적 device ID를 받아 플랫폼별 Backend 주소로 Flutter를 실행하는 공통 스크립트를 추가했다.
- 검증: `bash -n scripts/run-local.sh`, 잘못된 플랫폼·필수 환경변수 검증
- 리스크: Android OAuth Client의 package name·SHA-1 등록은 Google Cloud에서 별도 수행 필요
