# 작업 이력

## 2026-07-29 - 자유 문장 특수 기호 정규화 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 문장부호·통화기호·이모지가 포함된 입력에서 한글·영문·숫자·공백만 화면과 자유 문장 준비 API에 전달되는 회귀 조건을 추가했다.
- 검증: 대상 widget test 통과
- 리스크: 실제 한국어 IME 붙여넣기·조합 입력은 실기기 확인 필요

## 2026-07-29 - 회원 탈퇴 앱 회귀 테스트

- 변경 파일: `auth_api_test.dart`, `app_auth_service_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 두 token 전송, 성공 시 세션 삭제, 실패 시 세션 보존과 확인 dialog 흐름을 검증했다.
- 검증: `flutter test` 전체 통과
- 리스크: 실제 S3·Backend·앱 통합 E2E는 미실행

## 2026-07-28 - UI 최종 검토 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 평가 중 탭 왕복 상태 보존, 점수 4단계와 데이터 없음 문구, 작은 화면·큰 글자 Result 및 4탭 smoke 테스트를 추가했다.
- 검증: `flutter test`
- 리스크: golden 기반 픽셀 비교는 현재 테스트 체계에 없음

## 2026-07-28 - 4탭·자동 평가·전체 음절 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 추천 문장 이동, quota 소진, 녹음·평가 진행, 전체 음절 표시, 전체 문장 재연습, Review 기록, 권한·실패·점수 없음 상태를 검증하도록 테스트를 갱신했다.
- 검증: `flutter test`
- 리스크: 실제 기기 마이크 권한과 네트워크 중단 E2E는 수동 확인 필요

## 2026-07-27 - 직접 업로드·폴링 회귀 테스트

- 변경 파일: `evaluation_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: S3 PUT 헤더와 작업 API 직렬화, 앱의 새 평가 호출 계약을 검증하도록 테스트를 갱신했다.
- 검증: 대상 Flutter 테스트 통과
- 리스크: 실제 S3·백엔드 연계 E2E는 별도 환경 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `app_auth_service_test.dart`, `auth_api_test.dart`, `auth_session_store_test.dart`, `evaluation_api_test.dart`, `practice_quota_api_test.dart`, `pronunciation_api_test.dart`, `sentence_api_test.dart`, `user_preferences_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 테스트 목적 주석 보완

- 변경 파일: `app_auth_service_test.dart`, `auth_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 각 테스트 파일이 보장하는 인증 계약과 경쟁 조건 범위를 명시했다.
- 검증: `flutter test`
- 리스크: 동작 변경 없음

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
