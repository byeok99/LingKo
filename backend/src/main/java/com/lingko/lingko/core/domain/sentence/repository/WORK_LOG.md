# 작업 이력

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `RecommendedSentenceRepository.java`
- 내용: `findUnpracticedByWord`를 `findUnpracticedByCharacter`로 바꿨다. 이미 `like %:param%`이라 query 자체는 그대로 동작한다. 표준 발음이 아니라 원문으로 찾는 이유(원문에 없는 글자로 문장을 고르면 사용자가 이유를 알 수 없다)를 주석에 명시했다.
- 검증: `./gradlew test` 통과
- 리스크: 발음이 바뀌는 음절(좋 → 조)로 진입하면 후보를 놓친다

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `RecommendedSentenceRepository.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
