# 작업 이력

## 2026-07-30 - 변환·준비 API 정규화 테스트

- 변경 파일: `EvaluationControllerPrepareTest.java`, `WORK_LOG.md`
- 내용: 문장부호·기호가 포함된 요청도 정규화한 원문과 표준 발음으로 응답하는 계약을 검증했다.
- 검증: Backend 단위 테스트 전체 190개 통과
- 리스크: 없음

## 2026-07-27 - 평가 작업 API 계약 테스트

- 변경 파일: `EvaluationJobControllerTest.java`, `EvaluationResultControllerTest.java`, `WORK_LOG.md`
- 내용: 업로드·작업 생성·조회 인증 계약을 추가하고 legacy multipart 활성 조건을 테스트에 명시했다.
- 검증: 대상 Controller 테스트 통과
- 리스크: 없음

## 2026-07-24 - 인증 평가 생성 Controller 테스트

- 변경 파일: `EvaluationResultControllerTest.java`, `WORK_LOG.md`
- 내용: 활성 세션 사용자 ID가 통합 유스케이스로 전달되고 인증 누락이 401로 거부되는 계약을 추가했다.
- 검증: `EvaluationResultControllerTest` 통과
- 리스크: 없음

## 2026-07-24 - MockBean 제거 예정 API 교체

- 변경 파일: `EvaluationControllerPrepareTest.java`, `EvaluationHistoryControllerTest.java`, `EvaluationResultControllerTest.java`, `GuideGenerationJobControllerTest.java`, `WORK_LOG.md`
- 내용: Spring Boot 4에서 제거 예정인 `@MockBean`을 Spring Framework의 `@MockitoBean`으로 교체해 평가 Controller slice test의 mock Bean 재정의 동작을 유지했다.
- 검증: 영향받은 Controller 테스트, `./gradlew cleanTest test integrationTest`, deprecated `MockBean` 검색 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationControllerPrepareTest.java`, `EvaluationHistoryControllerTest.java`, `EvaluationResultControllerTest.java`, `GuideGenerationJobControllerTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 학습 기록 인증 테스트 목적 주석 보완

- 변경 파일: `EvaluationHistoryControllerTest.java`, `WORK_LOG.md`
- 내용: 활성 세션 기반 기록 소유권 검증 목적을 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - 활성 세션 기반 학습 기록 인증 테스트

- 변경 파일: `EvaluationHistoryControllerTest.java`, `WORK_LOG.md`
- 내용: 학습 기록 API가 공통 활성 세션 인증 결과를 사용하고 누락·무효 토큰에 401을 반환하는지 검증했다.
- 검증: `EvaluationHistoryControllerTest`
- 리스크: 없음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
