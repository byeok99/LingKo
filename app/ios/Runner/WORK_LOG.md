# 작업 이력

## 2026-07-20 - iOS Google OAuth 설정 이력 이전

- 변경 파일: `Info.plist`, `WORK_LOG.md`
- 내용: iOS Google OAuth Client ID를 `GIDClientID`에 설정하고 callback URL scheme을 등록한 뒤 새 발급 값으로 교체했다. 비밀값은 기록하지 않았다.
- 검증: `cd app && plutil -lint ios/Runner/Info.plist`
- 리스크: 실제 iOS Google Sign-In callback은 simulator/device에서 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
