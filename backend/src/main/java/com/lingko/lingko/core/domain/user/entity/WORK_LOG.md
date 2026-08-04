# 작업 이력

## 2026-08-04 - User에서 언어 설정 제거

- 변경 파일: `User.java`
- 내용: displayLanguage·nativeLanguage 필드와 갱신 메서드를 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - User에서 displayLanguage 참조 제거

- 변경 파일: `User.java`
- 내용: 엔티티 필드와 갱신 메서드에서 표시 언어를 제거하고 모국어만 갱신하도록 좁혔다. display_language 컬럼은 기존 데이터 보존을 위해 남겨 두고, 컬럼 삭제는 확장 후 축소 절차에 따라 별도 마이그레이션으로 다룬다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-03 - User 목표 레벨 영속 상태 제거

- 변경 파일: `User.java`, `WORK_LOG.md`
- 내용: `targetLevel` field와 `LearningLevel` enum을 제거하고 언어 설정 상태 전이를 표시 언어·모국어 두 값으로 축소했다.
- 검증: Backend 단위·통합 테스트 217개 통과
- 리스크: Backend 전체 line coverage 72.48%로 프로젝트 목표 80% 미달

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `User.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
