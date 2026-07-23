# 작업 이력

## 2026-07-23 - Refresh Token 세션 잠금 저장소

- 변경 파일: `RefreshTokenSessionRepository.java`, `WORK_LOG.md`
- 내용: 같은 세션의 동시 회전을 직렬화하는 비관적 쓰기 잠금 조회를 추가했다.
- 검증: AuthService 회전·재사용 테스트와 JPA 전체 테스트
- 리스크: 운영 부하에서 잠금 대기 시간 관측 필요
