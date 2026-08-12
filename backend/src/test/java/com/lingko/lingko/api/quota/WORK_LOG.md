# Work Log

## 2026-08-12 - 광고 session·SSV Controller 테스트

- 변경 파일: `PracticeQuotaControllerTest.java`, `AdMobSsvControllerTest.java`, `WORK_LOG.md`
- 내용: 인증 token 생성·조회, raw callback과 legacy 410을 검증한다.
- 검증: 대상 테스트 통과
- 리스크: 없음

## 2026-08-08 - 광고 보상 endpoint 인증·validation 테스트 추가

- 변경 파일: `PracticeQuotaControllerTest.java`, `WORK_LOG.md`
- 내용: 인증 누락, 유효하지 않은 event ID, 정상 quota 응답 계약을 검증한다.
- 검증: Backend 전체 test·integrationTest 266개 통과
- 리스크: 실제 Google SSV callback integration test는 후속 작업이다

## 2026-07-24 - MockBean 제거 예정 API 교체

- 변경 파일: `PracticeQuotaControllerTest.java`, `WORK_LOG.md`
- 내용: Spring Boot 4에서 제거 예정인 `@MockBean`을 Spring Framework의 `@MockitoBean`으로 교체해 quota Controller slice test의 mock Bean 재정의 동작을 유지했다.
- 검증: 영향받은 Controller 테스트, `./gradlew cleanTest test integrationTest`, deprecated `MockBean` 검색 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `PracticeQuotaControllerTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - quota 인증 테스트 목적 주석 보완

- 변경 파일: `PracticeQuotaControllerTest.java`, `WORK_LOG.md`
- 내용: 공통 활성 세션 인증 경계 검증 목적을 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - 활성 세션 기반 quota 인증 테스트

- 변경 파일: `PracticeQuotaControllerTest.java`, `WORK_LOG.md`
- 내용: quota API가 공통 활성 세션 인증 결과를 사용하고 누락·무효 토큰에 401을 반환하는지 검증했다.
- 검증: `PracticeQuotaControllerTest`
- 리스크: 없음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 에너지 API 계약 테스트

- 변경 파일: `PracticeQuotaControllerTest.java`
- 내용: 다음 충전 시각과 서버 시각 응답을 검증한다.
- 검증: backend test 통과
- 리스크: 없음
