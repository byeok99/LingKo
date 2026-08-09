# 작업 이력

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
