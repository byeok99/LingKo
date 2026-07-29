# 평가 재시도에서 작업과 쿼터가 중복 생성될 위험

- 상태: 해결
- 최초 발견일: 2026-07-27
- 영향 범위: 평가 작업 생성, 일일 쿼터 예약, 외부 평가 비용, 평가 결과 저장
- 심각도: SEV-3
- 영역: Backend / DB / Concurrency
- 관련 Issue: [#39](https://github.com/byeok99/LingKo/issues/39)
- 관련 PR: 미생성 — 로컬 변경 검토 후 실제 PR 링크로 교체

## 문제 현상

모바일 네트워크 재시도나 중복 탭으로 같은 평가 요청이 동시에 도착하면 별도 작업과 쿼터 예약이 생성될 수 있었습니다. 완료된 요청을 무기한 보존하면 중복 방지는 되지만 `evaluation_jobs`와 결과 payload가 계속 증가하는 운영 문제가 남습니다.

## 사용자 또는 운영 영향

- 같은 녹음이 여러 번 평가되어 일일 횟수가 중복 예약·차감될 수 있습니다.
- Azure 평가 호출과 결과 저장이 중복되어 비용과 데이터가 증가할 수 있습니다.
- 완료 작업을 무기한 보존하면 DB 저장공간과 인덱스 크기가 계속 증가합니다.

## 발생 조건

- 같은 사용자와 동일한 `Idempotency-Key`·payload를 사용합니다.
- 둘 이상의 작업 생성 transaction이 겹치거나 완료 요청을 반복 전송합니다.
- 완료 작업 정리 정책이 없고 서비스가 장기간 운영됩니다.

## 재현 방법

1. 사용자와 평가 대상 정보를 준비합니다.
2. 시작 latch로 같은 Key·hash·object key의 생성 요청 10개를 동시에 실행합니다.
3. 반환된 작업 ID, `evaluation_jobs` 행 수와 `free_reserved`를 확인합니다.
4. 완료 시각이 7일보다 오래된 성공·실패 작업과 최근·진행 중 작업을 함께 저장합니다.
5. 정리 서비스를 실행하고 삭제 대상을 확인합니다.

## 조사 과정

1. 사용자·Key Unique 제약과 기존 결과 반환 계약이 이미 존재함을 확인했습니다.
2. 작업 생성 transaction이 사용자 행을 비관적으로 잠근 뒤 기존 작업을 다시 조회하는 구조를 확인했습니다.
3. 동시 통합 테스트로 이 transaction 경계가 실제 JPA와 DB에서 작업과 예약을 한 건으로 수렴시키는지 검증했습니다.
4. 완료 작업의 `completed_at`이 명시적으로 기록되므로 별도 만료 컬럼 대신 완료 시점과 설정된 보존 기간으로 만료를 계산했습니다.
5. 진행 중 작업 삭제는 Worker 복구와 예약 쿼터를 훼손하므로 제외했습니다.

## 확인한 증거

- H2 MySQL mode의 Spring/JPA 통합 테스트에서 동일 요청 10개가 같은 작업 ID를 반환했습니다.
- 테스트 종료 시 평가 작업은 1건, 무료 예약은 1건, 사용량은 0건이었습니다.
- 완료 후 8일 지난 성공·실패 작업만 삭제되고 6일 작업과 `PENDING` 작업은 유지됐습니다.
- 실제 MySQL 동시 부하, 응답시간, lock wait과 정리 query 시간은 측정하지 않았습니다.

## 근본 원인

Idempotency는 Unique 제약만으로 완성되지 않습니다. 경쟁 요청이 제약 충돌로 실패하지 않고 같은 결과로 수렴하려면 기존 작업 확인과 쿼터 예약·생성을 직렬화한 transaction 경계가 필요합니다. 또한 완료 작업의 재사용 기간을 정하지 않으면 멱등 저장소가 무기한 증가합니다.

## 해결 방법

- 동일 사용자의 생성 요청은 사용자 행의 짧은 비관적 lock 뒤 기존 작업을 재확인합니다.
- 기존 작업이 없을 때만 쿼터를 예약하고 `PENDING` 작업을 생성합니다.
- 성공·최종 실패 작업은 완료 시점부터 기본 7일 보존합니다.
- `(status, completed_at)` 인덱스로 오래된 완료 작업 ID를 조회하고 기본 1시간마다 최대 1,000건을 삭제합니다.
- `PENDING`·`PROCESSING`은 정리하지 않아 재처리와 예약 쿼터를 보호합니다.

## 선택하지 않은 대안

### Unique 제약 예외 후 재조회

같은 transaction에서 flush 예외가 발생하면 rollback-only 상태가 될 수 있고 쿼터 예약도 이미 시도될 수 있어 현재 사용자 lock보다 복잡합니다.

### 모든 상태의 시간 기반 삭제

정체된 작업을 제거할 수 있지만 처리 가능한 작업과 미확정 예약을 유실할 수 있어 선택하지 않았습니다.

### 완료 작업 영구 보존

오래된 재호출도 응답할 수 있지만 결과 payload와 인덱스가 무제한 증가하므로 운영 정책으로 적합하지 않습니다.

## 검증 방법

- `./gradlew integrationTest --tests '*EvaluationJobIdempotencyIntegrationTest' --rerun-tasks`
- 실제 MySQL: `IDEMPOTENCY_TEST_DB_URL`, `IDEMPOTENCY_TEST_DB_USER`, `IDEMPOTENCY_TEST_DB_PASSWORD`를 지정하고 같은 통합 테스트 실행
- `./gradlew test --tests '*EvaluationJobMigrationTest' --rerun-tasks`
- 전체 회귀: `./gradlew test integrationTest`

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| 동일 요청 동시 실행 | 통합 검증 없음 | 10개 요청이 작업 1건·예약 1건으로 수렴 |
| 완료 작업 보존 | 무기한 | 기본 7일 |
| 실행당 정리량 | 정책 없음 | 기본 최대 1,000건 |
| 실제 MySQL lock wait | 미측정 | 미측정 |
| 정리 query 실행시간 | 미측정 | 미측정 |

## 롤백 방법

정리 Worker와 Service, 설정을 제거하고 V9 인덱스 migration은 필요하면 후속 migration에서 삭제합니다. 기존 완료 작업은 삭제하지 않으면 계속 조회 가능하지만 저장량 증가를 운영자가 관리해야 합니다. 동시 생성 lock은 기존 멱등성 보장이므로 롤백 대상이 아닙니다.

## 재발 방지

- 동일 요청 10개 동시 실행 통합 테스트를 유지합니다.
- 쿼터 예약과 작업 생성은 같은 transaction에서만 수행합니다.
- Idempotency 보존 기간을 변경할 때 모바일 재시도 기간과 개인정보 보존 정책을 함께 검토합니다.
- 진행 중 작업을 정리 대상에 추가하지 않습니다.

## 모니터링 및 알림

- Idempotency hit·conflict 수
- 사용자 행 lock wait과 deadlock
- 상태별 평가 작업 수와 가장 오래된 완료 작업 시각
- 정리 실행당 삭제 수와 batch 상한 도달 횟수
- 평가 성공 수와 쿼터 확정 수의 차이

구체적인 metric과 경보 임계값은 관측성 작업 [#48](https://github.com/byeok99/LingKo/issues/48)에서 확정합니다.

## 남은 위험

- 실제 MySQL 격리 수준과 운영 수준 동시 부하에서 lock wait을 검증하지 않았습니다.
- 정리 속도가 신규 완료 작업 증가량보다 느리면 backlog가 쌓일 수 있습니다.
- PR 생성 전이므로 관련 PR 링크를 후속 갱신해야 합니다.

## 관련 코드와 문서

- `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/EvaluationJobCreationService.java`
- `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/EvaluationJobCleanupService.java`
- `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/EvaluationJobCleanupWorker.java`
- `backend/src/integrationTest/java/com/lingko/lingko/core/domain/evaluation/EvaluationJobIdempotencyIntegrationTest.java`
- [`평가 흐름`](../architecture/evaluation-flow.md)
- [`API Reference`](../api/api-reference.md)
- [`운영 Runbook`](../operations/operations-runbook.md)
