# Work Log

## 2026-08-12 - SSV 보상 실패 예외 분리

- 변경 파일: `AdMobSsvVerificationException.java`, `AdRewardSessionNotFoundException.java`, `AdRewardUnavailableException.java`, `WORK_LOG.md`
- 내용: 위조 callback, session 소유권, 일시 설정·공급자 실패를 구분한다.
- 검증: Controller·handler 테스트 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `QuotaExceededException.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
