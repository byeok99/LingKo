# 작업 이력

## 2026-07-23 - 인증 갱신·로그아웃 API 명세 추가

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: Refresh Token 갱신과 현재 기기 로그아웃 endpoint, 요청·응답·오류 계약을 추가했다.
- 검증: Backend `AuthController` 및 DTO와 계약 대조
- 리스크: 전체 기기 로그아웃 endpoint는 후속 기능
