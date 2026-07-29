# 작업 이력

## 2026-07-29 - 회원 계정 bulk 삭제

- 변경 파일: `UserRepository.java`, `WORK_LOG.md`
- 내용: child 데이터 정리 후 사용자 행을 확인 가능한 건수로 삭제하고 persistence context를 정리하는 query를 추가했다.
- 검증: 계정 삭제 JPA 테스트와 Backend 전체 테스트 통과
- 리스크: 없음

## 2026-07-26 - 사용자별 쿼터 최초 생성 lock 추가

- 변경 파일: `UserRepository.java`, `WORK_LOG.md`
- 내용: 아직 child 쿼터 행이 없는 생성 경쟁을 직렬화할 수 있도록 사용자 행 비관적 lock 조회를 추가했다.
- 검증: H2 MySQL mode와 실제 MySQL 8에서 신규 쿼터 동시 생성 테스트 통과
- 리스크: 사용자 행을 잠그는 다른 기능 추가 시 lock 획득 순서 검토 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `UserRepository.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
