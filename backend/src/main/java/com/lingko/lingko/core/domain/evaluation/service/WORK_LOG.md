# 작업 이력

## 2026-07-29 - 사용자별 원격 음성 삭제 계약

- 변경 파일: `EvaluationAudioStorage.java`, `WORK_LOG.md`
- 내용: 회원 탈퇴 시 제출 여부와 무관하게 사용자 prefix의 원격 음성을 일괄 삭제하는 저장소 경계를 추가했다.
- 검증: S3 구현 단위 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 AWS 권한·Versioning 동작은 운영 환경 검증 필요

## 2026-07-29 - Queue 없는 독립 DB Worker로 단순화

- 변경 파일: `EvaluationJobExecutor.java`, `EvaluationJobProcessingService.java`, `EvaluationJobQueue.java`, `EvaluationJobQueueDispatcher.java`, `EvaluationJobQueueWorker.java`, `EvaluationJobWorker.java`, `WORK_LOG.md`
- 내용: SQS 계약·dispatcher·consumer claim을 제거하고 별도 프로세스의 DB polling Worker가 공통 평가 실행기를 호출하도록 유지했다.
- 검증: Worker 단위·배포 조건·단일 Worker 40건 통합 테스트 통과
- 리스크: 단일 Worker 처리량과 강제 종료 lease 복구는 실제 MySQL에서 확인 필요

## 2026-07-29 - SQS 독립 평가 Worker와 공통 실행 경계

- 변경 파일: `EvaluationJobExecutor.java`, `EvaluationJobProcessingService.java`, `EvaluationJobQueue.java`, `EvaluationJobQueueDispatcher.java`, `EvaluationJobQueueWorker.java`, `EvaluationJobWorker.java`, `WORK_LOG.md`
- 내용: SQS jobId 전달·DB lease·ACK/visibility·재발행 복구를 추가하고 DB fallback과 Queue Worker가 같은 평가·보상 절차를 사용하게 했다. 쿼터 native UPDATE 전 작업 상태를 변경해 detached 상태 유실도 수정했다.
- 검증: Queue 단위 테스트, 4 Worker/40 작업과 중복 전달 통합 테스트 통과
- 리스크: 실제 SQS 장애·Worker 강제 종료와 Azure 장시간 호출은 운영 검증 필요

## 2026-07-29 - 평가 Idempotency 만료 정리 구현

- 변경 파일: `EvaluationJobCleanupService.java`, `EvaluationJobCleanupWorker.java`, `WORK_LOG.md`
- 내용: 완료 후 기본 7일이 지난 성공·실패 작업만 매시간 제한된 batch로 삭제하고 진행 중 작업은 보존한다.
- 검증: 정리 대상·보존 대상 통합 테스트 및 `./gradlew test integrationTest` 통과
- 리스크: 정리 속도가 완료 작업 증가량보다 느린지 운영 모니터링 필요

## 2026-07-27 - DB 기반 음성 평가 Worker 구현

- 변경 파일: `EvaluationAudioStorage.java`, `EvaluationJobCreationService.java`, `EvaluationJobProcessingService.java`, `EvaluationJobService.java`, `EvaluationJobWorker.java`, `EvaluationService.java`, `WORK_LOG.md`
- 내용: 쿼터 예약과 Idempotency 작업 생성, lease claim, S3 음성 평가·재시도·원자적 성공 저장·실패 복구를 구현했다.
- 검증: Service·Worker·전체 Backend 테스트 통과
- 리스크: Azure 호출 timeout은 후속 운영 과제

## 2026-07-24 - 평가·쿼터·결과 저장 유스케이스 통합

- 변경 파일: `EvaluationApplicationService.java`, `EvaluationCompletionService.java`, `EvaluationService.java`, `WORK_LOG.md`
- 내용: 문장 메타데이터 확정, 쿼터 예약, 외부 평가, 결과 저장·예약 확정과 실패 보상을 조율하는 application flow를 추가했다.
- 검증: 핵심 단위 테스트와 Spring/JPA 통합 테스트 통과
- 리스크: 프로세스 비정상 종료 시 장기 예약 회수 정책과 요청 멱등성은 후속 작업

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationHistoryService.java`, `EvaluationPersistenceService.java`, `EvaluationService.java`, `GuideGenerationJobService.java`, `SpeechEvaluator.java`, `VideoGenerator.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
