# ADR-0009 Queue 없는 독립 DB 평가 Worker

- 상태: 승인
- 날짜: 2026-07-29
- 관련 Issue: [#47](https://github.com/byeok99/LingKo/issues/47)
- 후속 검증 Issue: [#69](https://github.com/byeok99/LingKo/issues/69)
- 관련 PR: [#70](https://github.com/byeok99/LingKo/pull/70)
- 대체 대상: [ADR-0008](0008-sqs-independent-evaluation-workers.md)

## 배경

발음 평가는 S3 음성 다운로드, 로컬 임시 파일과 Azure 외부 호출을 사용합니다. 이 작업을 API 프로세스에서 실행하면 HTTP 요청 thread와 분리돼 있어도 CPU, 메모리, 디스크와 장애 범위를 공유합니다.

반면 초기 운영에서 SQS를 사용하면 Queue, IAM, DLQ, visibility timeout과 모니터링을 추가로 운영해야 합니다. 현재 예상 트래픽은 독립 Worker 한 개로 처리 가능한지 먼저 검증할 단계이며 SQS나 Redis Queue를 즉시 도입할 근거가 없습니다.

## 결정

- MySQL `evaluation_jobs`를 영속 작업 저장소이자 현재의 작업 대기열로 사용합니다.
- API 컨테이너는 `EVALUATION_WORKER_ENABLED=false`로 평가 Worker를 실행하지 않습니다.
- 같은 Docker image를 web server 없이 실행하는 `evaluation-worker` 컨테이너가 DB를 polling합니다.
- 초기 운영 Worker replica는 한 개로 고정합니다.
- Worker는 비관적 lock으로 실행 가능한 작업 한 건을 claim하고 lease를 기록한 뒤 외부 평가를 수행합니다.
- 성공·재시도·최종 실패와 쿼터 보상 정책은 기존 DB 작업 상태를 유지합니다.
- 로컬에서 Spring Boot만 직접 실행할 때는 기본값으로 내부 Worker를 실행할 수 있지만 Compose 운영 경계는 API와 Worker를 분리합니다.
- SQS와 Redis Queue 선택은 실제 backlog, DB lock, 처리량과 운영 인프라를 측정한 뒤 다시 결정합니다.

## 대안

### API 내부 DB Worker

배포는 가장 단순하지만 평가 다운로드·외부 호출·임시 파일 부하와 장애가 API 프로세스에 남아 운영 격리 목적에 맞지 않습니다.

### SQS 독립 Worker

다중 Worker와 관리형 메시지 전달에는 적합하지만 초기 운영에 필요한 인프라보다 복잡합니다. 구현 시도는 ADR-0008에 기록하고 현재 코드에서는 제거합니다.

### Redis Streams

Redis가 캐시나 Rate Limit 용도로 이미 운영될 때 재검토할 수 있습니다. 지금은 Redis 자체를 Queue 때문에 새로 운영하지 않습니다.

## 결과

- API와 평가 Worker의 프로세스 장애·재시작 경계가 분리됩니다.
- 같은 Docker 호스트에서는 CPU, 메모리, 디스크와 네트워크를 공유하므로 완전한 성능 격리는 아닙니다.
- Queue 서비스 없이도 DB 작업과 lease로 Worker 재시작 후 처리를 복구할 수 있습니다.
- 단일 Worker가 40개 작업을 모두 성공 처리하고 결과·쿼터를 한 번씩 확정하는 Spring/JPA 통합 테스트를 유지합니다.
- Worker replica를 늘리면 DB claim lock 경합이 증가할 수 있으므로 측정 없이 확장하지 않습니다.

## 후속 작업

- 운영 backlog와 oldest pending age 측정
- Worker CPU, 메모리, 임시 디스크, 처리시간과 Azure 오류율 관측
- 실제 MySQL에서 Worker 강제 종료·lease 만료 복구 검증
- 단일 Worker 용량이 부족할 때 Redis Streams, SQS 또는 DB 다중 Worker 비교
