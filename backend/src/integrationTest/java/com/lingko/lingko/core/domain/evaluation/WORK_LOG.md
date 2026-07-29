# 작업 이력

## 2026-07-29 - 평가 Idempotency 동시성·정리 통합 테스트

- 변경 파일: `EvaluationJobIdempotencyIntegrationTest.java`, `WORK_LOG.md`
- 내용: 동일 요청 10개가 작업·쿼터 예약 1건으로 수렴하고 7일이 지난 완료 작업만 삭제되는지 Spring/JPA에서 검증했다.
- 검증: H2 MySQL mode 대상 테스트와 `./gradlew test integrationTest` 통과
- 리스크: 실제 MySQL은 `IDEMPOTENCY_TEST_DB_*` 환경변수 경로만 마련하고 미실행

## 2026-07-24 - 평가 application flow 통합 테스트

- 변경 파일: `EvaluationApplicationFlowIntegrationTest.java`, `WORK_LOG.md`
- 내용: 실제 Spring transaction과 JPA에서 평가 성공 시 결과 저장·쿼터 확정, 외부 실패 시 결과 미저장·쿼터 복구를 검증했다.
- 검증: `./gradlew integrationTest --tests "*EvaluationApplicationFlowIntegrationTest"` 통과
- 리스크: MySQL 동시성은 #38에서 별도 검증 필요
