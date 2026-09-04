# ADR-0007 S3 직접 업로드와 DB 기반 평가 Worker

- 상태: 승인
- 날짜: 2026-07-27
- 관련 Issue: [#47](https://github.com/byeok99/LingKo/issues/47)
- 관련 PR: 현재 작업 PR 생성 시 연결

## 배경

기존 평가 API는 최대 10MiB WAV 본문을 API 서버가 받은 뒤 Azure 평가가 끝날 때까지 HTTP 연결과 서버 임시 파일을 유지했습니다. 사용자가 늘면 업로드 네트워크, 요청 thread, 임시 디스크와 외부 서비스 대기가 같은 API 인스턴스에 집중됩니다.

MVP에서는 음성 본문과 장시간 평가를 일반 API 요청에서 분리해야 하지만, 별도 Queue 서비스까지 동시에 도입하면 메시지 중복, visibility timeout, DLQ와 운영 인프라가 한 번에 늘어납니다.

## 결정

- Flutter 앱은 인증 API에서 제한 시간 Presigned PUT URL을 발급받고 비공개 S3에 WAV를 직접 업로드합니다.
- API 서버는 음성 본문을 중계하지 않고 사용자 ID prefix, object metadata와 크기를 검증합니다.
- 평가 요청은 MySQL `evaluation_jobs`에 저장하고 즉시 `202 Accepted`와 `jobId`를 반환합니다.
- DB가 작업 상태의 원본이며 단일 제한 Worker가 `PENDING` 또는 lease가 만료된 작업을 locking read로 claim합니다.
- Worker는 S3 음성을 로컬 임시 파일로 내려받아 WAV 헤더를 다시 검증하고 Azure 평가를 실행합니다.
- 결과 저장, 쿼터 확정과 작업 성공 상태는 하나의 DB transaction으로 처리합니다.
- 실패는 최대 시도 횟수까지 예약을 유지하고 재시도하며, 최종 실패에서 쿼터를 복구합니다.
- 성공·최종 실패 후 S3 음성을 삭제하고 별도 S3 Lifecycle을 최종 안전망으로 구성합니다.
- 앱은 인증 갱신이 적용되는 상태 API를 Polling해 완료 결과를 표시합니다.
- 기존 multipart 평가 endpoint는 기본 비활성화하고 명시적 환경변수로만 임시 호환할 수 있습니다.

## 대안

### 동기 API에서 S3에 재업로드

음성 본문과 Azure 대기가 계속 API 서버를 통과하므로 병목을 제거하지 못해 제외했습니다.

### SQS 즉시 도입

장기적으로 적합하지만 현재는 DB 작업 상태가 Polling과 결과 복구에 어차피 필요합니다. MVP 1단계는 DB Worker로 경계를 먼저 안정화하고, 다중 Worker 처리량이 필요할 때 claim 신호만 SQS의 `jobId` 메시지로 교체합니다.

### Redis Queue

Redis persistence, pending message 회수, eviction과 별도 failover 정책이 필요합니다. 현재 필수 Redis 인프라가 없어 Queue만을 위해 도입하지 않습니다.

### 메모리 `@Async`

서버 재시작 시 작업과 쿼터 예약의 복구 근거가 사라지므로 사용하지 않습니다.

## 결과

- API 서버는 WAV 본문을 받지 않고 Azure 완료를 기다리지 않습니다.
- 서버 재시작 후에도 lease 만료 작업을 DB에서 다시 claim할 수 있습니다.
- 현재 Worker는 한 번에 한 작업만 처리하므로 Azure 동시 호출이 제한됩니다.
- API와 Worker가 같은 Spring 배포 단위에 있지만 HTTP thread와 작업 상태는 분리됩니다.
- 운영 S3 Lifecycle, 실제 Presigned PUT E2E, Azure timeout·재시도 고도화는 배포 전 검증이 필요합니다.
- 단일 DB Worker 용량이 부족해지면 Redis Streams, SQS와 DB 다중 Worker를 측정해 비교하되 DB 상태와 Idempotency를 진실의 원천으로 유지합니다.

SQS 확장안은 [ADR-0008](0008-sqs-independent-evaluation-workers.md)에서 검토·구현했으나 초기 운영 범위에서 제외했습니다. 현재 결정은 [ADR-0009](0009-independent-db-evaluation-worker.md)의 Queue 없는 독립 DB Worker입니다.
