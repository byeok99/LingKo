# 작업 이력

## 2026-09-02 - 평가 작업 phase 영속화

- 변경 파일: `EvaluationJob.java`, `WORK_LOG.md`
- 내용: 실제 Worker 경계를 나타내는 phase를 추가하고 claim·retry·단조 증가 규칙을 entity invariant로 보호했다.
- 검증: `EvaluationJobTest`, Backend 전체 단위·통합 테스트 통과
- 리스크: 운영 배포 전 V21 migration 선적용 필요

## 2026-08-18 - 평가 entity 상태 enum 주석 보강

- 변경 파일: `EvaluationJob.java`, `EvaluationLog.java`, `WORK_LOG.md`
- 내용: 비동기 작업 영속 상태와 추천·직접 입력 문장 출처 enum의 업무 의미를 Javadoc에 명시했다.
- 검증: Backend 단위 테스트 293개·통합 테스트 16개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - 음절·단어 점수 nullable 의미 주석

- 변경 파일: `EvaluationSyllable.java`, `EvaluationWord.java`
- 내용: score의 null이 0점이 아니라 '신뢰 불가'를 뜻하며 음절은 V15 이후 guide-only 단위임을 주석으로 고정했다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - 단어 점수 snapshot 영속 모델

- 변경 파일: `EvaluationLog.java`, `EvaluationWord.java`, `EvaluationSyllable.java`, `WORK_LOG.md`
- 내용: 평가 log에 단어 점수를 한 번 저장하고 음절은 nullable `word_position`으로 연결하도록 확장했다.
- 검증: persistence·history·migration·회원 탈퇴 테스트 통과
- 리스크: 운영 DB에 V15 migration 적용 필요

## 2026-07-29 - Queue 발행 상태 제거

- 변경 파일: `EvaluationJob.java`, `WORK_LOG.md`
- 내용: SQS 발행 복구에만 사용하던 `enqueuedAt` 상태와 재시도 연동을 제거했다.
- 검증: 상태 전이·V11 마이그레이션 테스트 통과
- 리스크: 없음

## 2026-07-29 - Queue 발행 복구 시각 추가

- 변경 파일: `EvaluationJob.java`, `WORK_LOG.md`
- 내용: SQS 발행 성공 시각을 저장하고 재시도 작업의 즉시 중복 발행을 억제하는 `enqueuedAt` 상태를 추가했다.
- 검증: 상태 전이·마이그레이션 테스트 통과
- 리스크: 재발행 간격은 실제 Queue 지연에 맞춰 운영 조정 필요

## 2026-07-27 - 영속 평가 작업 상태 모델 추가

- 변경 파일: `EvaluationJob.java`, `WORK_LOG.md`
- 내용: PENDING·PROCESSING·SUCCEEDED·FAILED 전이와 lease, retry, 결과 payload를 저장하는 엔티티를 추가했다.
- 검증: 상태 전이 단위 테스트 통과
- 리스크: 장시간 외부 호출 시 운영 lease 값 조정 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationLog.java`, `EvaluationSyllable.java`, `Syllable.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
