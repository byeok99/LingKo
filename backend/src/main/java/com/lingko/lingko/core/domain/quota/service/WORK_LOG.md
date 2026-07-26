# 작업 이력

## 2026-07-26 - 쿼터 동시 요청 원자성 보장

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 예약·확정·복구 결과를 조건부 UPDATE 영향 행으로 판정하고, 당일 행 최초 생성만 사용자 부모 lock으로 직렬화했다.
- 검증: H2 MySQL mode와 실제 MySQL 8에서 동시 요청 10개 반복 테스트 통과
- 리스크: 비정상 종료로 남은 예약의 만료·회수 정책은 후속 작업

## 2026-07-24 - 쿼터 예약·확정·복구 서비스 추가

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 원래 날짜와 무료·보상 출처를 보존하는 예약 token으로 평가 성공 시 확정하고 실패 시 정확히 복구하도록 했다.
- 검증: `PracticeQuotaServiceTest`, `EvaluationApplicationFlowIntegrationTest`
- 리스크: 당시 남은 동시 요청 원자성은 2026-07-26 작업에서 보완됨

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `PracticeQuotaService.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
