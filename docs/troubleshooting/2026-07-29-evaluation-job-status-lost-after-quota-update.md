# 평가 결과는 저장됐지만 작업 상태가 PROCESSING에 남는 문제

- 상태: 해결
- 최초 발견일: 2026-07-29
- 영향 범위: 평가 Worker 완료 처리, 평가 작업 Polling, 일일 쿼터
- 심각도: SEV-3
- 영역: Backend / DB / Transaction
- 관련 Issue: [#47](https://github.com/byeok99/LingKo/issues/47)
- 관련 PR: [#68](https://github.com/byeok99/LingKo/pull/68)

## 문제 현상

독립 Worker 확장 통합 테스트에서 평가 결과와 쿼터 사용량은 모두 저장됐지만 `evaluation_jobs.status` 40건이 `SUCCEEDED`가 아니라 `PROCESSING`에 남았습니다.

## 사용자 또는 운영 영향

- 앱 Polling이 완료 결과 대신 진행 중 상태를 계속 표시할 수 있습니다.
- lease 만료 후 같은 작업이 다시 처리되어 외부 평가 비용과 중복 결과 위험이 생깁니다.
- 결과 수와 성공 작업 수가 달라 운영 지표와 장애 판단이 왜곡됩니다.

## 발생 조건

- 평가 성공 또는 최종 실패 transaction에서 작업 상태와 쿼터 상태를 함께 변경합니다.
- 쿼터 repository의 native UPDATE가 `clearAutomatically=true`로 persistence context를 비웁니다.
- 작업 엔티티 상태를 쿼터 UPDATE 뒤에 변경합니다.

## 재현 방법

1. 평가 작업 40건을 생성합니다.
2. 독립 DB Worker 한 개로 모든 작업을 처리합니다.
3. 평가 결과 수, 쿼터 사용량과 작업 상태를 각각 조회합니다.
4. 수정 전에는 결과·쿼터는 40건 완료되지만 작업 상태는 `PROCESSING`에 남습니다.

## 조사 과정

1. Worker의 DB claim과 완료 transaction 경계를 확인했습니다.
2. 결과 저장 수와 쿼터 확정 수가 작업 수와 일치해 외부 평가·저장 실패를 제외했습니다.
3. 완료 transaction의 호출 순서와 쿼터 native UPDATE 설정을 비교했습니다.
4. `clearAutomatically=true` 실행 뒤 작업 엔티티가 detached되어 이후 `succeed` 또는 `fail` 변경이 flush되지 않음을 확인했습니다.

## 확인한 증거

- 수정 전 통합 테스트: 평가 결과 40건, 사용자별 사용량 5회, 작업 상태 `PROCESSING` 40건
- 수정 후 통합 테스트: 평가 결과 40건, 작업 상태 `SUCCEEDED` 40건, 사용자별 사용량 5회
- 응답시간, CPU, 메모리와 실제 MySQL 실행시간은 측정하지 않았습니다.

## 근본 원인

쿼터 상태 전이는 조건부 native UPDATE 후 persistence context를 자동으로 clear합니다. 작업 상태 변경을 그 뒤에 수행해 detached 엔티티의 변경 감지가 일어나지 않았습니다.

## 해결 방법

- 성공 처리에서 결과 저장 후 `job.succeed`를 먼저 호출하고 쿼터 확정을 마지막에 수행합니다.
- 최종 실패 처리에서 `job.fail`을 먼저 호출하고 쿼터 예약 복구를 마지막에 수행합니다.
- native UPDATE가 flush를 먼저 수행하므로 작업 상태와 쿼터 변경은 같은 transaction에서 함께 commit됩니다.

## 선택하지 않은 대안

### 작업 엔티티를 다시 save

호출 순서의 잘못된 transaction 경계를 숨기고 불필요한 merge를 추가하므로 선택하지 않았습니다.

### 쿼터 UPDATE의 clearAutomatically 제거

쿼터 엔티티의 stale 상태가 같은 transaction에 남아 다른 정합성 문제를 만들 수 있어 현재 원자 UPDATE 정책을 유지했습니다.

## 검증 방법

- `./gradlew integrationTest --tests '*IndependentEvaluationWorkerIntegrationTest' --rerun-tasks`
- `./gradlew test integrationTest`

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| 처리 작업 | 40건 | 40건 |
| 저장 결과 | 40건 | 40건 |
| `SUCCEEDED` 작업 | 0건 | 40건 |
| 사용자별 쿼터 사용 | 5회 | 5회 |

## 롤백 방법

호출 순서를 되돌리면 상태 유실이 재발하므로 코드 롤백 대상이 아닙니다. 긴급 복구 시 결과가 존재하고 쿼터가 확정된 작업만 선별해 작업 상태를 보정해야 하며, 원본 음성과 중복 결과 여부를 함께 확인합니다.

## 재발 방지

- 결과 수, 작업 상태와 쿼터 확정을 함께 검증하는 통합 테스트를 유지합니다.
- `clearAutomatically=true` native UPDATE와 다른 엔티티 변경을 같은 transaction에서 사용할 때 호출 순서를 리뷰합니다.
- 완료 상태 변경은 persistence context를 clear하는 연산보다 먼저 수행합니다.

## 모니터링 및 알림

- `PROCESSING` 작업의 oldest lease age
- 평가 결과 수와 `SUCCEEDED` 작업 수의 차이
- 사용자별 쿼터 확정 수와 성공 작업 수의 차이

구체적인 metric과 경보 임계값은 [#48](https://github.com/byeok99/LingKo/issues/48)에서 확정합니다.

## 남은 위험

- 실제 MySQL에서 transaction flush 순서와 부하 상황을 별도로 검증해야 합니다.
- Worker 강제 종료 시 DB lease 만료 후 재claim은 실제 MySQL 환경에서 확인해야 합니다.

## 관련 코드와 문서

- `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/EvaluationJobProcessingService.java`
- `backend/src/integrationTest/java/com/lingko/lingko/core/domain/evaluation/IndependentEvaluationWorkerIntegrationTest.java`
- [`ADR-0009`](../architecture/adr/0009-independent-db-evaluation-worker.md)
- [`평가 흐름`](../architecture/evaluation-flow.md)
- [`운영 Runbook`](../operations/operations-runbook.md)
