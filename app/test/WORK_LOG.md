# 작업 이력

## 2026-07-20 - 인증 및 quota 테스트 이력 이전

- 변경 파일: `widget_test.dart`, `practice_quota_api_test.dart`, `WORK_LOG.md`
- 내용: 로그인 필수 진입, 로고 스플래시, Google 로그인 버튼 및 quota API/UI 동작을 검증하는 테스트를 추가·수정했다.
- 검증: `cd app && flutter test --reporter compact` 통과
- 리스크: 실제 OAuth 네이티브 콜백은 emulator/device 수동 검증 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
