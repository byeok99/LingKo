# 작업 이력

## 2026-07-23 - Refresh Token 갱신·로그아웃 엔드포인트

- 변경 파일: `AuthController.java`, `WORK_LOG.md`
- 내용: `POST /api/auth/token/refresh`와 `POST /api/auth/logout` 계약을 추가했다.
- 검증: AuthController 단위 테스트와 Backend 전체 테스트
- 리스크: 인증 rate limit은 별도 운영 보안 작업으로 남음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
