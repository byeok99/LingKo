# 작업 이력

## 2026-07-30 - 추천 발음 저장 필드 제거

- 변경 파일: `RecommendedSentence.java`, `WORK_LOG.md`
- 내용: 추천 콘텐츠 엔티티에서 `standardPronunciation`을 제거해 원문 외의 발음 정답을 영속 원천으로 사용하지 못하게 했다.
- 검증: Backend 단위 190개·통합 11개 통과
- 리스크: V12 migration 선적용이 필요함

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `RecommendedSentence.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
