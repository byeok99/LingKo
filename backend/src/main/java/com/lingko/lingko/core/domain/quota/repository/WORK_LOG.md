# Work Log

## 2026-08-12 - 광고 session lock·transaction 조회 추가

- 변경 파일: `AdRewardSessionRepository.java`, `AdRewardReceiptRepository.java`, `WORK_LOG.md`
- 내용: session row lock과 전역 provider transaction 중복 조회를 추가했다.
- 검증: AdRewardService 테스트 통과
- 리스크: 없음

## 2026-08-08 - 광고 보상 receipt 저장소 추가

- 변경 파일: `AdRewardReceiptRepository.java`, `WORK_LOG.md`
- 내용: 사용자·event 중복 조회와 계정 삭제용 일괄 삭제 계약을 추가했다.
- 검증: quota 및 account deletion 관련 Backend 전체 테스트 통과
- 리스크: 없음

## 2026-08-04 - 현재 에너지 조회의 write lock 명시

- 변경 파일: `DailyPracticeQuotaRepository.java`
- 내용: 단순 조회 API도 자연 충전 계산 때문에 FOR UPDATE로 행을 잠근다는 사실을 query 주석에 남겼다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 조회 경로의 lock 경합은 부하 시험으로 별도 확인 필요

## 2026-07-29 - 탈퇴 사용자 쿼터 삭제

- 변경 파일: `DailyPracticeQuotaRepository.java`, `WORK_LOG.md`
- 내용: 회원 탈퇴 transaction에서 사용자별 일일 쿼터를 bulk 삭제하는 query를 추가했다.
- 검증: 계정 삭제 JPA 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 MySQL 대량 행 삭제 시간은 미측정

## 2026-07-26 - 쿼터 상태 전이 원자 UPDATE 추가

- 변경 파일: `DailyPracticeQuotaRepository.java`, `WORK_LOG.md`
- 내용: 무료·보상 예약과 확정·복구를 업무 조건이 포함된 원자 UPDATE로 처리하고 최초 행 생성 재확인용 locking read를 추가했다.
- 검증: H2 MySQL mode와 실제 MySQL 8에서 `PracticeQuotaConcurrencyTest` 통과
- 리스크: native SQL 컬럼 변경 시 동시성 테스트로 회귀를 확인해야 함

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `DailyPracticeQuotaRepository.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 최신 에너지 행 잠금 조회

- 변경 파일: `DailyPracticeQuotaRepository.java`
- 내용: 날짜별 신규 행 대신 사용자 최신 행을 비관적 잠금으로 조회하도록 변경했다.
- 검증: 동시성 service test 통과
- 리스크: 기존 중복 행은 최신 행만 사용
