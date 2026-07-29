# ADR-0008 SQS 기반 독립 평가 Worker

- 상태: 승인
- 날짜: 2026-07-29
- 관련 Issue: [#47](https://github.com/byeok99/LingKo/issues/47)
- 관련 PR: [#68](https://github.com/byeok99/LingKo/pull/68)

## 배경

ADR-0007은 S3 직접 업로드와 영속 DB Worker로 API 요청에서 음성 본문과 장시간 평가를 분리했습니다. 그러나 API와 Worker가 같은 Spring 프로세스에서 실행되고 DB polling Worker가 한 번에 한 작업만 처리하므로 Worker 수를 API 인스턴스와 독립적으로 조절할 수 없었습니다.

Queue를 도입하더라도 평가 상태, 결과, 쿼터 예약과 Idempotency를 메시지에 옮기면 SQS의 at-least-once 전달에서 중복 저장과 복구 복잡도가 커집니다. API transaction과 Queue 발행 사이의 원자성도 별도로 보완해야 합니다.

## 결정

- MySQL `evaluation_jobs`를 작업 상태, payload, lease, 결과와 Idempotency의 진실의 원천으로 유지합니다.
- SQS Standard Queue에는 개인정보나 평가 payload 없이 `jobId`만 발행합니다.
- API 프로세스의 dispatcher가 실행 가능한 `PENDING` 작업을 DB에서 조회해 SQS에 발행합니다.
- 발행 성공 시 `enqueued_at`을 기록하고 일정 시간이 지난 `PENDING` 작업은 다시 발행해 DB commit 이후 Queue 발행 유실을 복구합니다.
- 독립 Worker 프로세스는 SQS 메시지를 long polling하고 DB 비관적 lock과 lease를 획득한 경우에만 평가를 실행합니다.
- 완료되었거나 존재하지 않는 작업의 중복 메시지는 ACK하고, 다른 Worker의 lease가 유효하거나 재시도 시각 전이면 visibility를 연장합니다.
- 기존 DB polling Worker는 Queue 장애 또는 작은 개발 환경을 위한 fallback mode로 유지합니다.
- API와 Worker는 같은 Docker image를 사용하되 Worker는 web server 없이 실행하고 replica 수를 독립 조절합니다.

## 대안

### Queue 메시지를 작업 원본으로 사용

상태 조회와 모바일 Polling, 쿼터 보상, Idempotency를 위해 DB가 이미 필요합니다. 메시지에 전체 작업을 복제하면 두 원본의 정합성을 관리해야 하므로 선택하지 않았습니다.

### Transactional Outbox 테이블 추가

엄격한 단일 발행 보장은 제공하지만 현재 `evaluation_jobs`의 `PENDING` 상태 자체가 복구 가능한 outbox 역할을 할 수 있습니다. 중복 전달을 DB lease가 흡수하므로 별도 테이블을 추가하지 않았습니다.

### Redis Queue

현재 필수 Redis 인프라가 없고 persistence, pending message 회수와 failover 정책을 새로 운영해야 합니다. AWS S3를 이미 사용하므로 관리형 SQS를 선택했습니다.

### SQS FIFO

평가 작업은 사용자별 순서 보장이 필요하지 않고 DB Idempotency와 lease가 중복 실행을 방지합니다. 처리량 제한과 MessageGroup 설계를 추가하지 않기 위해 Standard Queue를 사용합니다.

## 결과

- API 수와 Worker 수를 별도로 조절할 수 있습니다.
- SQS 중복 전달과 오래된 메시지는 DB 상태를 확인해 안전하게 제거하거나 연기합니다.
- Queue 발행 실패나 메시지 유실 후에도 DB의 오래된 `PENDING` 작업을 재발행할 수 있습니다.
- 4개 논리 Worker가 40개 작업을 중복 결과·쿼터 차감 없이 완료하는 Spring/JPA 통합 테스트를 통과했습니다.
- 동일 작업을 두 번 전달하는 테스트에서 결과와 쿼터가 한 번만 확정됐습니다.
- 실제 AWS SQS, MySQL과 Azure를 함께 사용한 처리량·지연시간은 측정하지 않았습니다.

## 후속 작업

- 운영 Queue의 DLQ와 redrive 정책 설정
- Queue depth, oldest message age, 처리량과 실패율 메트릭·알림
- 실제 AWS SQS·MySQL 환경의 장시간 부하와 Worker 강제 종료 복구 검증
- Azure timeout과 Circuit Breaker 적용
