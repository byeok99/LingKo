# Work Log

## 2026-08-12 - SSV session·provider transaction 모델 추가

- 변경 파일: `AdRewardSession.java`, `AdRewardSessionStatus.java`, `AdRewardReceipt.java`, `WORK_LOG.md`
- 내용: token hash, 만료·완료 상태와 Google transaction을 저장한다.
- 검증: JPA·migration 테스트 통과
- 리스크: 없음

## 2026-08-08 - 광고 보상 중복 방지 receipt entity 추가

- 변경 파일: `AdRewardReceipt.java`, `WORK_LOG.md`
- 내용: 사용자별 reward event 처리 이력을 저장해 동일 보상을 멱등 처리한다.
- 검증: quota service test 및 Backend 전체 테스트 통과
- 리스크: 운영 SSV 전에는 event ID가 클라이언트 생성 값이다

## 2026-08-04 - next_refill_at nullable 의미 주석

- 변경 파일: `DailyPracticeQuota.java`
- 내용: null이 '충전 대기 없음'을 뜻한다는 상태값 계약을 필드 주석으로 명시했다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 동작 변경 없음

## 2026-07-26 - 동시 상태 전이 책임을 저장소로 이동

- 변경 파일: `DailyPracticeQuota.java`, `WORK_LOG.md`
- 내용: 동시 예약에 안전하지 않은 엔티티 상태 전이 메서드를 제거하고 원자 DB UPDATE가 동시성 invariant를 소유하도록 책임을 명확히 했다.
- 검증: `PracticeQuotaServiceTest`, `PracticeQuotaConcurrencyTest`
- 리스크: 관리·테스트용 단일 transaction 계산 메서드는 동시 요청 경로에서 사용하지 않아야 함

## 2026-07-24 - 평가 쿼터 예약 상태 추가

- 변경 파일: `DailyPracticeQuota.java`, `WORK_LOG.md`
- 내용: 무료·보상 예약량을 사용량과 분리하고 예약 확정·복구 상태 전이를 추가했다.
- 검증: `PracticeQuotaServiceTest`, `EvaluationApplicationFlowIntegrationTest`
- 리스크: 당시 남은 동시 예약 원자성은 2026-07-26 작업에서 보완됨

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `DailyPracticeQuota.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 다음 자연 충전 시각 저장

- 변경 파일: `DailyPracticeQuota.java`
- 내용: 기존 테이블 호환성을 유지하며 nullable `nextRefillAt` 상태를 추가했다.
- 검증: migration 및 service test 통과
- 리스크: 실제 MySQL migration 적용 확인 필요
