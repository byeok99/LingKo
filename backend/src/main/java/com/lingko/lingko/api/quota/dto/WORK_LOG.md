# Work Log

## 2026-08-12 - 광고 session DTO로 교체

- 변경 파일: `AdRewardClaimRequest.java` 삭제, `AdRewardSessionResponse.java`, `AdRewardSessionStatusResponse.java`, `WORK_LOG.md`
- 내용: client event claim 대신 server token·상태 응답을 제공한다.
- 검증: JSON Controller 테스트 통과
- 리스크: 없음

## 2026-08-08 - 광고 reward event 입력 계약 추가

- 변경 파일: `AdRewardClaimRequest.java`, `WORK_LOG.md`
- 내용: reward event ID를 16~80자 영숫자·밑줄·하이픈으로 제한하는 validation DTO를 추가했다.
- 검증: controller validation test 및 Backend 전체 테스트 통과
- 리스크: SSV 도입 시 Google transaction ID 계약으로 교체 또는 확장이 필요하다

## 2026-08-04 - serverTime 동봉 이유 주석

- 변경 파일: `PracticeQuotaResponse.java`
- 내용: 기기 시계 대신 서버 시각 차이로 countdown을 계산하도록 두 필드의 의미와 선택 이유를 기록했다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 동작 변경 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `PracticeQuotaResponse.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 시간 충전 응답 계약 추가

- 변경 파일: `PracticeQuotaResponse.java`
- 내용: `nextRefillAt`, `serverTime`을 추가하고 자정 `resetAt`을 제거했다.
- 검증: controller 및 service test 통과
- 리스크: 구버전 앱의 추가 필드 무시 여부 확인 필요
