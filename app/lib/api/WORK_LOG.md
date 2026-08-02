# 작업 이력

## 2026-07-30 - Practice API 입력 정규화

- 변경 파일: `evaluation_api.dart`, `pronunciation_api.dart`, `WORK_LOG.md`
- 내용: 자유 문장 준비·평가 작업 요청 전에 Unicode 문장부호·기호와 연속 공백을 동일 규칙으로 정규화했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 없음

## 2026-07-29 - 회원 탈퇴 DELETE API 연동

- 변경 파일: `api_client.dart`, `auth_api.dart`, `WORK_LOG.md`
- 내용: Access Token header와 현재 Refresh Token body를 함께 전송하는 계정 삭제 요청을 추가했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 실제 운영 API 연계 E2E는 미실행

## 2026-07-27 - S3 직접 업로드와 평가 작업 API 연동

- 변경 파일: `api_client.dart`, `evaluation_api.dart`, `WORK_LOG.md`
- 내용: 인증 JSON POST, presigned URL PUT, 평가 작업 생성·조회 API를 추가하고 기존 multipart 평가 호출을 대체했다.
- 검증: `flutter analyze`, 대상 API·위젯 테스트 통과
- 리스크: 실제 S3 CORS·서명 정책은 배포 환경 검증 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `api_client.dart`, `auth_api.dart`, `evaluation_api.dart`, `practice_quota_api.dart`, `pronunciation_api.dart`, `sentence_api.dart`, `user_preferences_api.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 API 코드 목적 주석 보완

- 변경 파일: `api_client.dart`, `auth_api.dart`, `WORK_LOG.md`
- 내용: 204 응답 처리와 로그인·회전·폐기 API의 책임을 Dartdoc으로 명시했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 동작 변경 없음

## 2026-07-23 - Refresh Token API client 추가

- 변경 파일: `api_client.dart`, `auth_api.dart`, `WORK_LOG.md`
- 내용: token refresh와 logout API를 추가하고 204 응답을 본문 파싱 없이 처리하도록 공통 client를 확장했다.
- 검증: Flutter API·인증 서비스 테스트 및 전체 테스트
- 리스크: 운영 API 주소는 HTTPS를 사용해야 함

## 2026-07-20 - quota API 이력 이전

- 변경 파일: `practice_quota_api.dart`, `WORK_LOG.md`
- 내용: bearer token으로 `GET /api/quota/today`를 호출하고 응답 및 오류를 처리하는 API client를 추가했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: Render 배포 환경의 실제 API 응답과 추가 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
