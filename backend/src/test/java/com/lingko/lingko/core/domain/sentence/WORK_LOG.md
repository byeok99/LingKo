# 작업 이력

## 2026-08-05 - 추천 로마자 파생 테스트

- 변경 파일: `SentenceServiceTest.java`, `WORK_LOG.md`
- 내용: 추천 문장 응답이 표준 발음에서 로마자 가이드를 파생하는 계약을 검증했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-07-30 - 추천 발음 비영속 계약 테스트

- 변경 파일: `RecommendedSentenceMigrationTest.java`, `SentenceServiceTest.java`, `WORK_LOG.md`
- 내용: V12 후 추천 발음 컬럼이 없고 추천 응답은 원문을 현재 규칙으로 변환해 `마싣껟따`를 반환하는지 검증했다.
- 검증: Backend 단위 테스트 전체 190개 통과
- 리스크: 실제 MySQL migration은 미검증

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `RecommendedSentenceMigrationTest.java`, `SentenceServiceTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
