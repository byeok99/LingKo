# 작업 이력

## 2026-08-04 - preferences 계약에서 displayLanguage 제거

- 변경 파일: `UserPreferencesResponse.java`, `UserPreferencesUpdateRequest.java`
- 내용: 앱이 더 이상 보내지도 쓰지도 않는 표시 언어 필드를 요청·응답 계약에서 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-03 - Preferences 목표 레벨 API 계약 제거

- 변경 파일: `UserPreferencesResponse.java`, `UserPreferencesUpdateRequest.java`, `WORK_LOG.md`
- 내용: 조회·변경 DTO에서 의미 없는 `targetLevel`을 제거하고 표시 언어와 모국어 validation만 유지했다.
- 검증: Backend 단위·통합 테스트 217개 통과
- 리스크: Backend 전체 line coverage 72.48%로 프로젝트 목표 80% 미달

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `UserPreferencesResponse.java`, `UserPreferencesUpdateRequest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
