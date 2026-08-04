# 작업 이력

## 2026-08-04 - 점수·가이드 상태 enum 도입

- 변경 파일: `ScoreStatus.java`, `GuideStatus.java`, `EvaluationJobRequest.java`, `GuideCharacterResponse.java`, `PracticeResultResponse.java`, `PracticeWordResultResponse.java`, `PracticeHistoryWordResponse.java`
- 내용: 상태 문자열을 enum으로 승격해 오타와 누락 분기를 컴파일 시점에 막고, 자유 문장 text 상한을 준비 endpoint와 같은 100자로 맞췄다. Jackson 기본 직렬화를 사용해 JSON 계약은 그대로다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: guideStatus의 NONE 값은 현재 코드에서 생성되지 않아 enum에 포함하지 않음

## 2026-08-04 - scoreStatus 허용 값 문서화

- 변경 파일: `PracticeWordResultResponse.java`, `PracticeHistoryWordResponse.java`
- 내용: AVAILABLE/UNAVAILABLE 두 값만 가지며 UNAVAILABLE일 때 score가 null인 이유와 클라이언트 처리 규칙을 명시했다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 상태 문자열이 아직 공유 상수가 아니라 양쪽에 literal로 흩어져 있음

## 2026-08-04 - 평가 단어 응답 계약

- 변경 파일: `PracticeResultResponse.java`, `PracticeHistoryItemResponse.java`, `PracticeWordResultResponse.java`, `PracticeHistoryWordResponse.java`, `WORK_LOG.md`
- 내용: 단어 점수와 하위 guide-only 음절을 Practice 결과와 Review 기록에 제공하는 DTO 계층을 추가했다.
- 검증: 백엔드 전체 단위 테스트 통과
- 리스크: 기존 client는 추가 JSON field를 무시해야 함

## 2026-07-27 - 평가 업로드·작업 DTO 추가

- 변경 파일: `EvaluationUploadRequest.java`, `EvaluationUploadResponse.java`, `EvaluationJobRequest.java`, `EvaluationJobResponse.java`, `WORK_LOG.md`
- 내용: S3 업로드 발급과 비동기 평가 작업 요청·상태 응답 계약을 정의했다.
- 검증: Controller·JSON 왕복 테스트 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `GuideCharacterResponse.java`, `GuideGenerationJobRequest.java`, `GuideGenerationJobResponse.java`, `PracticeHistoryCharacterResponse.java`, `PracticeHistoryItemResponse.java`, `PracticeHistoryResponse.java`, `PracticeResultResponse.java`, `PronunciationPrepareRequest.java`, `PronunciationPrepareResponse.java`, `StandardPronunciationRequest.java`, `StandardPronunciationResponse.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
