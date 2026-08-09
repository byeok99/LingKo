# 작업 이력

## 2026-08-08 - 광고 energy 지급 규칙 테스트 추가

- 변경 파일: `PracticeQuotaServiceTest.java`, `WORK_LOG.md`
- 내용: +1 지급, 자연 충전 timer 유지, 동일 event 멱등성, 5개 cap을 고정했다.
- 검증: Backend 전체 test·integrationTest 266개 통과
- 리스크: 없음

## 2026-07-30 - 이미 복구된 예약 판별 테스트

- 변경 파일: `PracticeQuotaServiceTest.java`, `WORK_LOG.md`
- 내용: 같은 예약의 후속 복구 시 예외 대신 false를 반환하고 잔여 쿼터를 변경하지 않는 최종 실패 전용 계약을 검증했다.
- 검증: RED compile 실패 확인 후 Backend 단위 201개·통합 11개 통과
- 리스크: 없음

## 2026-07-26 - 쿼터 동시성 회귀 테스트 추가

- 변경 파일: `PracticeQuotaConcurrencyTest.java`, `WORK_LOG.md`
- 내용: 남은 1회 예약, 신규 행 생성, 동일 예약 확정·복구에 동시 요청 10개를 실행하고 H2와 실제 MySQL datasource를 선택할 수 있게 했다.
- 검증: H2 MySQL mode와 MySQL 8에서 예약·생성 반복 테스트 및 상태 전이 테스트 통과
- 리스크: CI 기본 경로는 H2이며 실제 MySQL 검증에는 별도 datasource 환경변수가 필요

## 2026-07-24 - 쿼터 예약 lifecycle·migration 테스트

- 변경 파일: `PracticeQuotaServiceTest.java`, `PracticeQuotaMigrationTest.java`, `WORK_LOG.md`
- 내용: 무료·보상 예약의 확정·복구와 V7 migration의 예약 컬럼 생성을 검증했다.
- 검증: quota 관련 단위·migration 테스트 통과
- 리스크: 당시 남은 동시성 테스트는 2026-07-26 작업에서 추가됨

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `PracticeQuotaServiceTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 시간 충전 및 migration 테스트

- 변경 파일: `PracticeQuotaMigrationTest.java`, `PracticeQuotaServiceTest.java`
- 내용: 1시간 충전, 최대 5회, 자정 무초기화, timer 보존과 새 컬럼을 검증한다.
- 검증: backend test 및 integrationTest 통과
- 리스크: 없음
