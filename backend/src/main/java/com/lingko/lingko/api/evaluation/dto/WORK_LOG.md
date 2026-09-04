# 작업 이력

## 2026-09-02 - 평가 작업 phase API 계약 추가

- 변경 파일: `EvaluationJobResponse.java`, `WORK_LOG.md`
- 내용: polling 응답에 실제 Worker phase를 포함해 앱이 처리 단계를 추측하지 않도록 했다.
- 검증: `EvaluationJobControllerTest`, Backend 전체 단위·통합 테스트 통과
- 리스크: 구버전 앱은 추가 JSON 필드를 무시하므로 없음

## 2026-08-09 - 가이드 생성 입력 상한 강화

- 변경 파일: `GuideGenerationJobRequest.java`, `WORK_LOG.md`
- 내용: 음절을 한글 1자로 제한하고 각 외부 URL 길이를 최대 2,048자로 제한했다.
- 검증: `GuideGenerationJobControllerTest`, Backend 전체 단위·통합 테스트 통과
- 리스크: 없음

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `WeakWordResponse.java` → `WeakSoundResponse.java`, `WeakWordListResponse.java` → `WeakSoundListResponse.java`, `WordDetailResponse.java` → `SoundDetailResponse.java`
- 내용: 응답 단위를 음절로 바꿨다. `averageScore`가 측정값이 아니라 어절 점수를 귀속시킨 추정이라는 점, `practiced[].score`가 음절이 아니라 어절 점수이고 null이 0점과 다르다는 점을 주석으로 명시했다.
- 검증: `./gradlew test` 통과
- 리스크: 없음

## 2026-08-06 - 로마자와 취약 어절 응답 추가

- 변경 파일: `GuideCharacterResponse.java`, `PracticeWordResultResponse.java`, `PracticeHistoryWordResponse.java`, `PracticeHistoryCharacterResponse.java`, `WeakWordResponse.java`, `WeakWordListResponse.java`, `WordDetailResponse.java`
- 내용: 어절·음절 응답에 로마자를 넣고, 취약 어절 목록과 어절 상세 화면용 응답을 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 취약 목록은 어절 단위다. 디자인의 음절 단위 표기는 화면에서 함께 조정해야 함

## 2026-08-05 - 평가 응답 로마자 계약 추가

- 변경 파일: `PronunciationPrepareResponse.java`, `PracticeHistoryItemResponse.java`, `WORK_LOG.md`
- 내용: 준비 문장과 평가 기록에 `romanizedPronunciation` 필드를 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 구버전 앱은 추가 JSON 필드를 무시하므로 없음

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
