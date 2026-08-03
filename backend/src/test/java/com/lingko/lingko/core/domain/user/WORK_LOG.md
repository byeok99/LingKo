# 작업 이력

## 2026-08-03 - 언어 설정·목표 레벨 제거 테스트

- 변경 파일: `UserPreferencesServiceTest.java`, `UserPreferencesMigrationTest.java`, `WORK_LOG.md`
- 내용: 서비스가 표시 언어와 모국어만 저장하는 계약으로 갱신하고 V13이 언어 컬럼을 보존하면서 `target_level`을 제거하는 migration test를 추가했다.
- 검증: 구현 전 DTO 생성자 compile 실패 확인, Backend 단위·통합 테스트 217개 통과
- 리스크: 실제 MySQL migration은 테스트 범위 밖임

## 2026-07-29 - 회원 탈퇴 서비스·DB 삭제 테스트

- 변경 파일: `AccountDeletionServiceTest.java`, `AccountDeletionPersistenceServiceTest.java`, `WORK_LOG.md`
- 내용: 재확인→S3→DB 순서, S3 실패 시 DB 보존과 사용자 소유 데이터 삭제·공용 음절 보존을 검증했다.
- 검증: 대상 테스트와 Backend 전체 `test integrationTest` 통과
- 리스크: 실제 MySQL 대량 데이터 성능은 미측정

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `UserPreferencesServiceTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
