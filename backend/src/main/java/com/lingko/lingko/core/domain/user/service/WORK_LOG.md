# 작업 이력

## 2026-08-03 - 언어 Preferences 조율로 단순화

- 변경 파일: `UserPreferencesService.java`, `WORK_LOG.md`
- 내용: Target level 전달·응답 mapping을 제거하고 검증된 표시 언어와 모국어만 사용자에게 적용하도록 서비스 계약을 맞췄다.
- 검증: Backend 단위·통합 테스트 217개 통과
- 리스크: Backend 전체 line coverage 72.48%로 프로젝트 목표 80% 미달

## 2026-07-29 - S3 우선 회원 탈퇴 조율

- 변경 파일: `AccountDeletionService.java`, `AccountDeletionPersistenceService.java`, `AccountDeletionUnavailableException.java`, `WORK_LOG.md`
- 내용: 현재 token 재확인 후 S3를 먼저 정리하고 성공 시 사용자 소유 DB 데이터를 하나의 transaction으로 삭제한다.
- 검증: 순서·실패 보존 단위 테스트, JPA 삭제 테스트와 Backend 전체 테스트 통과
- 리스크: S3 성공 뒤 DB 장애 시 음성만 먼저 삭제될 수 있으며 동일 세션으로 DB 삭제를 재시도해야 함

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `UserPreferencesService.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
