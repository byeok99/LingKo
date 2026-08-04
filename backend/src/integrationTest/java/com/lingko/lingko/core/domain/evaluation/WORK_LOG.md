# 작업 이력

## 2026-07-30 - 평가 입력 정규화 통합 검증

- 변경 파일: `EvaluationApplicationFlowIntegrationTest.java`, `WORK_LOG.md`
- 내용: 자유 문장 평가가 기호를 제거한 원문과 현재 규칙의 표준 발음을 저장하는 흐름을 검증했다.
- 검증: Backend 통합 테스트 전체 11개 통과
- 리스크: 실제 Azure 평가 호출은 외부 통합 테스트 범위임

## 2026-07-29 - 단일 독립 DB Worker 처리 검증

- 변경 파일: `IndependentEvaluationWorkerIntegrationTest.java`, `EvaluationQueueScalingIntegrationTest.java`, `WORK_LOG.md`
- 내용: Queue·4 Worker 테스트를 단일 DB polling Worker가 40개 작업을 결과·쿼터 중복 없이 처리하는 계약으로 교체했다.
- 검증: H2 MySQL mode 대상 통합 테스트 통과
- 리스크: 실제 MySQL lock wait·처리량과 별도 프로세스 강제 종료는 미측정

## 2026-07-29 - 독립 Queue Worker 확장·중복 전달 검증

- 변경 파일: `EvaluationQueueScalingIntegrationTest.java`, `WORK_LOG.md`
- 내용: 4개 논리 Worker가 40개 작업을 중복 없이 완료하고 동일 jobId 재전달도 결과·쿼터를 한 번만 확정하는지 검증했다.
- 검증: H2 MySQL mode 대상 통합 테스트 통과
- 리스크: 실제 MySQL·SQS·Azure 처리량과 지연시간은 미측정

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
## 2026-08-03 - 평가 흐름 시간대 명칭 정리

- 변경 파일: `EvaluationApplicationFlowIntegrationTest.java`
- 내용: 자정 초기화 의미를 제거하고 서비스 시간대 명칭으로 갱신했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음
