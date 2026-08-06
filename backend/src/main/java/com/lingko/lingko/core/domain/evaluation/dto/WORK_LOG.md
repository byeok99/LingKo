# 작업 이력

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `WeakWordAggregate.java` → `WordScoreAggregate.java`
- 내용: 이름이 곧 결과였던 record가 이제는 음절 집계의 *입력*이라 의미가 달라져 이름을 바꿨다. DB는 어절까지만 집계하고 음절 분해는 service가 한다는 경계를 주석에 남겼다.
- 검증: `./gradlew test` 통과
- 리스크: 없음

## 2026-08-04 - WordScore position 계약 주석

- 변경 파일: `AssessmentResult.java`
- 내용: position이 공백 기준 0-based 순서이며 도메인이 이 값으로 재검증한다는 계약을 남겼다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - 공급자 독립 단어 점수 모델

- 변경 파일: `AssessmentResult.java`, `WORK_LOG.md`
- 내용: 기준 문장과 검증된 공급자 단어 점수를 내부 평가 결과에 추가했다.
- 검증: `EvaluationServiceResultTest`, parser test 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AssessmentResult.java`, `GuideGenerationJobStatus.java`, `VideoType.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
