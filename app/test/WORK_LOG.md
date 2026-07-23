# 작업 이력

## 2026-07-23 - Refresh Token 회전·자동 갱신 테스트

- 변경 파일: `auth_api_test.dart`, `app_auth_service_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: refresh/logout 계약, 401 재시도, 동시 refresh 단일화, 실패 시 세션 삭제와 로그인 화면 전환, 로그아웃 중 늦은 refresh 응답 차단을 검증했다.
- 검증: `flutter test --reporter compact`
- 리스크: 실제 Access Token 만료 대기는 자동화하지 않고 401 응답으로 재현함

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
