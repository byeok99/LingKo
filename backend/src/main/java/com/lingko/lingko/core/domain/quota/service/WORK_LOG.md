# Work Log

## 2026-08-12 - 검증된 SSV만 quota 지급

- 변경 파일: `AdRewardService.java`, `VerifiedAdRewardCallback.java`, `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 1회성 session과 Google transaction을 확인한 callback만 최대 5회 정책으로 지급한다.
- 검증: 정책·중복·timer·session 테스트 통과
- 리스크: 없음

## 2026-08-08 - 광고 보상 energy 지급 규칙 추가

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 인증 사용자에게 중복 event당 최대 한 번, 총 5개 미만일 때만 +1을 지급하며 자연 충전 시각은 유지한다.
- 검증: 지급·중복·cap·timer 보존 unit test와 Backend 전체 테스트 통과
- 리스크: 운영 전 광고 완료 증명을 Google SSV로 검증해야 한다

## 2026-07-30 - 최종 실패용 비예외 예약 복구

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 이미 사라진 예약을 boolean으로 판별하는 복구 경계를 추가해 terminal 작업 transaction이 반복 롤백되지 않게 했다.
- 검증: 대상 TDD와 Backend 단위 201개·통합 11개 통과
- 리스크: 일반 평가 경로의 `releasePractice`는 기존 엄격한 예외 계약 유지

## 2026-07-26 - 쿼터 동시 요청 원자성 보장

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 예약·확정·복구 결과를 조건부 UPDATE 영향 행으로 판정하고, 당일 행 최초 생성만 사용자 부모 lock으로 직렬화했다.
- 검증: H2 MySQL mode와 실제 MySQL 8에서 동시 요청 10개 반복 테스트 통과
- 리스크: 비정상 종료로 남은 예약의 만료·회수 정책은 후속 작업

## 2026-07-24 - 쿼터 예약·확정·복구 서비스 추가

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 원래 날짜와 무료·보상 출처를 보존하는 예약 token으로 평가 성공 시 확정하고 실패 시 정확히 복구하도록 했다.
- 검증: `PracticeQuotaServiceTest`, `EvaluationApplicationFlowIntegrationTest`
- 리스크: 당시 남은 동시 요청 원자성은 2026-07-26 작업에서 보완됨

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 자정 초기화 폐지 및 시간 충전

- 변경 파일: `PracticeQuotaService.java`
- 내용: 1시간 단위 lazy refill, 자연 최대 5회, 기존 timer 보존과 즉시 상태 반환을 구현했다.
- 검증: 단위·동시성·자정 경계 test 및 integrationTest 통과
- 리스크: 사용자 요청 없이 장시간 API 호출이 없으면 다음 조회 시 일괄 반영됨
