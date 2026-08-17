# 작업 이력

## 2026-08-12 - Apple token 보안 검증 테스트

- 변경 파일: `AppleOAuthIdentityVerifierTest.java`, `WORK_LOG.md`
- 내용: 정상 token과 nonce·audience·issuer·서명 key·만료·email 검증 실패 경로를 고정했다.
- 검증: 대상 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 Apple key rotation은 운영 통합 검증 필요
