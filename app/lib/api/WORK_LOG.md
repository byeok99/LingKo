# 작업 이력

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
