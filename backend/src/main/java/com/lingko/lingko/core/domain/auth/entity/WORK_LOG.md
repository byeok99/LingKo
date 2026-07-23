# 작업 이력

## 2026-07-23 - Refresh Token 세션 엔티티

- 변경 파일: `RefreshTokenSession.java`, `WORK_LOG.md`
- 내용: 현재 토큰 해시, 절대 만료와 폐기 상태를 캡슐화한 기기 세션 엔티티를 추가했다.
- 검증: AuthService 및 JPA 전체 테스트
- 리스크: 만료 세션 물리 삭제는 후속 운영 작업
