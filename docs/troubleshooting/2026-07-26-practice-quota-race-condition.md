# 동시 연습 요청에서 일일 쿼터가 초과 예약되는 경쟁 조건

- 상태: 해결
- 최초 발견일: 2026-07-26
- 영향 범위: Backend 평가 전 쿼터 예약·확정·복구와 사용자별 당일 쿼터 생성
- 심각도: SEV-3
- 영역: Backend / DB / Concurrency
- 관련 Issue: [#38](https://github.com/byeok99/LingKo/issues/38)
- 관련 PR: [#63](https://github.com/byeok99/LingKo/pull/63)

## 문제 현상

남은 무료 연습 횟수가 1회인 상태에서 같은 사용자의 요청을 동시에 실행하면 둘 이상의 transaction이 동일한 잔여량을 읽고 예약에 성공할 수 있었습니다. 당일 쿼터 행이 없는 신규 사용자의 동시 조회에서는 같은 사용자·날짜 행을 여러 transaction이 생성해 Unique 제약 예외가 발생했습니다.

## 사용자 또는 운영 영향

- 사용자가 일일 허용량보다 많은 외부 평가를 실행할 수 있습니다.
- 중복 외부 호출로 공급자 비용이 증가할 수 있습니다.
- 첫 쿼터 조회가 간헐적으로 서버 오류로 실패할 수 있습니다.
- 예약·확정·복구가 중복 반영되면 표시 잔여량과 실제 사용량이 불일치할 수 있습니다.

## 발생 조건

- 같은 사용자와 같은 한국 서비스 날짜를 사용합니다.
- 둘 이상의 요청이 쿼터 조회와 변경 사이에 겹칩니다.
- 남은 무료 또는 보상 횟수가 경쟁 요청 수보다 적습니다.
- 또는 사용자·날짜별 `daily_practice_quota` 행이 아직 없습니다.

## 재현 방법

1. 무료 한도 5회 중 4회를 사용한 쿼터 행을 준비합니다.
2. 시작 latch로 같은 사용자의 예약 요청 10개를 동시에 실행합니다.
3. 성공 수와 `free_reserved`를 확인합니다.
4. 별도 신규 사용자로 오늘 쿼터 조회 10개를 동시에 실행합니다.
5. 모든 응답과 사용자·날짜별 행 개수를 확인합니다.

## 조사 과정

1. 기존 서비스가 엔티티를 조회한 뒤 Java 메서드로 잔여량을 검사하고 예약량을 증가시키는 것을 확인했습니다.
2. 동시성 테스트에서 남은 1회보다 많은 예약 성공과 신규 행 생성 시 Unique 제약 충돌을 재현했습니다.
3. MySQL 전용 upsert를 검토했지만 H2에서 SQL 문법이 호환되지 않아 채택하지 않았습니다.
4. 사용자 부모 행에 최초 생성 lock을 적용한 뒤 실제 MySQL의 기본 `REPEATABLE READ`에서 대기 transaction이 이전 snapshot을 보는 문제를 발견했습니다.
5. 부모 lock 획득 뒤 일반 조회 대신 locking read 결과를 직접 사용하도록 바꾸어 앞선 transaction이 생성한 행을 현재 읽기로 확인했습니다.

## 확인한 증거

- 수정 전 동시 예약 테스트는 성공 수가 1개라는 assertion을 통과하지 못했습니다.
- 수정 전 신규 행 동시 생성은 `DataIntegrityViolationException`을 발생시켰습니다.
- 중간 MySQL 검증에서 최초 생성 대기 요청이 `daily practice quota does not exist`로 실패해 Repeatable Read snapshot 문제를 확인했습니다.
- 최종 H2 MySQL mode와 실제 MySQL 8 모두에서 동시성 테스트가 통과했습니다.
- 응답시간, query 시간, CPU와 메모리는 이번 기능 검증에서 측정하지 않았습니다.

## 근본 원인

잔여량 확인과 예약량 변경이 하나의 원자 DB 연산이 아니라 조회 후 엔티티 변경으로 분리되어 있었습니다. 최초 생성은 존재하지 않는 쿼터 행을 lock할 수 없었고 Unique 제약은 중복 저장을 막았지만 경쟁 요청을 정상 응답으로 수렴시키지는 못했습니다.

## 해결 방법

- 무료·보상 예약 조건과 예약량 증가를 각각 하나의 조건부 native `UPDATE`로 처리했습니다.
- 확정·복구도 예약량 존재 조건을 포함한 원자 `UPDATE`로 한 번만 반영했습니다.
- 사용자·날짜별 최초 생성은 항상 존재하는 사용자 부모 행의 짧은 비관적 lock으로 직렬화했습니다.
- 부모 lock 후 쿼터 locking read를 사용해 MySQL Repeatable Read에서도 앞선 transaction의 생성 결과를 확인했습니다.
- 조건부 `UPDATE` 영향 행이 0인 예약 충돌은 재시도하지 않고 HTTP 429 쿼터 소진 계약으로 반환합니다.
- 외부 평가 중에는 어떤 DB lock도 유지하지 않습니다.

## 선택하지 않은 대안

### 전체 쿼터 행 비관적 lock

일반 예약까지 직렬화하고 외부 호출 경계가 확장될 위험이 있어 최초 생성에만 lock을 제한했습니다.

### 낙관적 lock과 재시도

충돌 예외 처리와 재시도 정책이 추가되며 마지막 횟수 경쟁은 재시도해도 소진 결과가 대부분이므로 조건부 `UPDATE`보다 복잡합니다.

### MySQL 전용 upsert

최초 생성은 간결하지만 H2 테스트 호환성과 SQL 이식성이 낮아 부모 행 lock을 선택했습니다.

## 검증 방법

- H2 MySQL mode: `./gradlew test --tests '*PracticeQuotaConcurrencyTest' --tests '*PracticeQuotaServiceTest' --rerun-tasks`
- 실제 MySQL 8 Docker: `QUOTA_TEST_DB_URL`, `QUOTA_TEST_DB_USER`, `QUOTA_TEST_DB_PASSWORD`를 지정하고 `./gradlew test --tests '*PracticeQuotaConcurrencyTest' --rerun-tasks`
- 각 환경에서 남은 1회 예약과 신규 행 생성 테스트를 각각 3회 반복했습니다.
- 동일 예약의 동시 확정과 복구도 각각 성공 1건만 허용하는지 확인했습니다.

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| 남은 1회, 동시 예약 10개 | 성공 1개 초과 재현 | 성공 1개, 429 대상 9개 |
| 신규 사용자 동시 조회 10개 | Unique 제약 예외 재현 | 정상 응답 10개, 쿼터 행 1개 |
| 동일 예약 동시 확정·복구 10개 | 원자성 보장 테스트 없음 | 상태 전이 성공 각 1개 |
| 응답시간 | 미측정 | 미측정 |
| query 실행시간 | 미측정 | 미측정 |

## 롤백 방법

원자 `UPDATE` repository 메서드와 최초 생성용 lock 조회를 제거하고 이전 엔티티 상태 전이 구현으로 되돌립니다. 스키마 migration은 포함하지 않아 DB 롤백은 필요하지 않습니다. 단, 롤백하면 동시 초과 예약 위험이 다시 발생하므로 운영 트래픽을 차단하거나 사용자별 요청을 외부에서 직렬화해야 합니다.

## 재발 방지

- `PracticeQuotaConcurrencyTest`에서 10개 동시 요청과 3회 반복 검증을 유지합니다.
- 쿼터 상태 전이는 엔티티 조회 후 변경이 아니라 조건부 원자 저장소 연산을 우선합니다.
- 행이 없는 resource의 생성 경쟁은 Unique 제약만으로 정상 흐름을 구성하지 않습니다.
- 실제 운영 DB 격리 수준에서 동시성 테스트를 실행할 수 있도록 datasource 환경변수를 유지합니다.

## 모니터링 및 알림

- 쿼터 예약 429 비율과 사용자별 급증
- 예약·확정·복구에서 영향 행 0인 상태 전이 오류
- DB lock wait time과 deadlock 수
- 외부 평가 성공 건수와 쿼터 확정 건수의 차이

메트릭과 경보 임계값 구현은 관측성 작업 [#48](https://github.com/byeok99/LingKo/issues/48)의 후속 범위입니다.

## 남은 위험

- 서버 비정상 종료로 확정 또는 복구되지 않은 예약의 만료·회수 정책은 아직 없습니다.
- 실제 운영 부하에서 lock wait과 응답시간은 측정하지 않았습니다.
- 사용자 행을 잠그는 다른 기능이 추가되면 lock 획득 순서를 함께 검토해야 합니다.

## 관련 코드와 문서

- `backend/src/main/java/com/lingko/lingko/core/domain/quota/repository/DailyPracticeQuotaRepository.java`
- `backend/src/main/java/com/lingko/lingko/core/domain/quota/service/PracticeQuotaService.java`
- `backend/src/main/java/com/lingko/lingko/core/domain/user/repository/UserRepository.java`
- `backend/src/test/java/com/lingko/lingko/core/domain/quota/PracticeQuotaConcurrencyTest.java`
- [`ADR-0006`](../architecture/adr/0006-atomic-practice-quota-transitions.md)
- [`성능·확장성 계획`](../performance/scalability-plan.md)
