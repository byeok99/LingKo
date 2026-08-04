# 작업 이력

## 2026-08-04 - 단어 점수 평가·영속 회귀 테스트

- 변경 파일: `EvaluationServiceResultTest.java`, `EvaluationPersistenceServiceTest.java`, `EvaluationHistoryServiceTest.java`, `EvaluationPersistenceMigrationTest.java`, `WORK_LOG.md`
- 내용: 단어 grouping·점수 저장·Review 복원·guide-only 음절·V15 schema 계약을 RED→GREEN으로 검증했다.
- 검증: 백엔드 전체 단위 테스트 통과
- 리스크: 없음

## 2026-08-03 - 가이드 seed migration 회귀 테스트

- 변경 파일: `SyllableGuideSeedMigrationTest.java`, `WORK_LOG.md`
- 내용: repeatable seed가 새 테이블 없이 기존 `syllables`의 입·혀 URL을 MP4 초기값으로 채우는 계약을 검증했다.
- 검증: 대상 migration 테스트와 Backend 전체 단위 테스트 통과
- 리스크: seed 목록 누락은 출시 전 S3 산출물과 대조 필요

## 2026-07-30 - 점수 없는 `김` 영상·terminal 실패 회귀

- 변경 파일: `EvaluationServiceResultTest.java`, `EvaluationJobProcessingServiceTest.java`, `WORK_LOG.md`
- 내용: 글자 점수가 없어도 `김`은 MP4를 사용하고, 쿼터 예약이 이미 없어도 최종 실패 작업은 `FAILED`로 수렴하는 계약을 검증했다.
- 검증: RED compile 실패 확인 후 Backend 단위 201개·통합 11개 통과
- 리스크: 실제 외부 영상 content는 운영 검증 필요

## 2026-07-30 - `김` 평가 결과 MP4 회귀 검증

- 변경 파일: `EvaluationServiceResultTest.java`, `WORK_LOG.md`
- 내용: 취약 점수의 `김`이 입·혀 전환 가이드를 MP4 URL로 반환하는 평가 결과 계약을 검증했다.
- 검증: 대상 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 실제 외부 영상 content 검증은 포함하지 않음

## 2026-07-30 - 평가 경로 동적 발음 회귀 검증

- 변경 파일: `EvaluationApplicationServiceTest.java`, `EvaluationJobServiceTest.java`, `EvaluationServicePrepareTest.java`, `EvaluationServiceResultTest.java`, `EvaluationServiceTest.java`, `WORK_LOG.md`
- 내용: 추천·자유·legacy·비동기 작업이 저장된 추천 발음 없이 현재 변환 규칙과 정규화 원문을 사용하는 계약을 검증했다.
- 검증: Backend 단위 테스트 전체 190개 통과
- 리스크: 없음

## 2026-07-29 - 독립 DB Worker 계약으로 테스트 정리

- 변경 파일: `EvaluationJobMigrationTest.java`, `EvaluationJobQueueDispatcherTest.java`, `EvaluationJobQueueWorkerTest.java`, `EvaluationJobTest.java`, `EvaluationJobWorkerTest.java`, `EvaluationWorkerDeploymentConditionTest.java`, `WORK_LOG.md`
- 내용: SQS 전용 테스트를 제거하고 API 비활성·독립 Worker 활성, 처리 여부와 V11 스키마 제거 계약을 검증했다.
- 검증: 대상 단위 테스트 통과
- 리스크: Docker 프로세스 강제 종료 E2E는 미실행

## 2026-07-29 - Queue Worker·dispatcher 계약 테스트

- 변경 파일: `EvaluationJobExecutorTest.java`, `EvaluationJobMigrationTest.java`, `EvaluationJobQueueDispatcherTest.java`, `EvaluationJobQueueWorkerTest.java`, `EvaluationJobTest.java`, `EvaluationJobWorkerTest.java`, `EvaluationWorkerModeConditionTest.java`, `WORK_LOG.md`
- 내용: jobId 발행·발행 실패 복구, DB claim별 ACK/visibility, API/Worker bean 분리, 공통 실행 성공·재시도·최종 실패와 V10 스키마를 검증했다.
- 검증: 대상 단위 테스트 통과
- 리스크: 실제 AWS SQS 통신은 포함하지 않음

## 2026-07-29 - 평가 작업 정리 인덱스 회귀 검증

- 변경 파일: `EvaluationJobMigrationTest.java`, `WORK_LOG.md`
- 내용: V9 적용 후 완료 작업 정리 인덱스가 생성되는 계약을 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 실제 MySQL migration 검증 필요

## 2026-07-27 - 평가 작업 생명주기 회귀 테스트

- 변경 파일: `EvaluationJobMigrationTest.java`, `EvaluationJobServiceTest.java`, `EvaluationJobTest.java`, `EvaluationJobWorkerTest.java`, `EvaluationApplicationServiceTest.java`, `WORK_LOG.md`
- 내용: 상태 전이, Idempotency, JSON 결과 복원, Worker 성공·재시도·최종 실패, 스키마 제약을 검증했다.
- 검증: 대상 테스트 및 Backend 전체 테스트 통과
- 리스크: 실제 Azure·S3 E2E는 별도 환경 필요

## 2026-07-24 - 평가 통합 유스케이스 단위 테스트

- 변경 파일: `EvaluationApplicationServiceTest.java`, `WORK_LOG.md`
- 내용: 추천·자유 문장 저장 metadata, 평가 실패와 저장 실패의 쿼터 보상을 검증했다.
- 검증: `EvaluationApplicationServiceTest` 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationHistoryServiceTest.java`, `EvaluationPersistenceMigrationTest.java`, `EvaluationPersistenceRepositoryTest.java`, `EvaluationPersistenceServiceTest.java`, `EvaluationServicePrepareTest.java`, `EvaluationServiceResultTest.java`, `EvaluationServiceTest.java`, `GuideGenerationJobServiceTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
