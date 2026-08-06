# 작업 이력

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `EvaluationWordRepository.java`
- 내용: `findWeakWords`를 `findScoredWordAggregates`로 바꿔 최소 시도 필터를 걷어냈다. 필터가 이제 음절 단위라 어절 단계에서 거르면 안 된다. `findPracticedByWord`는 `findPracticedByCharacter`로 바꿔 부분 일치로 찾는다.
- 검증: `./gradlew test` 통과
- 리스크: 음절 조회가 한 글자 like 앞뒤 wildcard라 index를 타지 못한다. 사용자 한 명의 어절 snapshot으로 범위가 좁아 현재는 감당 가능하나, 이력이 매우 긴 사용자에서 재확인 필요

## 2026-08-04 - 평가 단어 저장소

- 변경 파일: `EvaluationWordRepository.java`, `WORK_LOG.md`
- 내용: 단어 snapshot 조회·저장과 회원 탈퇴 시 사용자 소유 단어 삭제 연산을 추가했다.
- 검증: `AccountDeletionPersistenceServiceTest` 통과
- 리스크: 없음

## 2026-07-29 - 탈퇴 사용자 평가 데이터 bulk 삭제

- 변경 파일: `EvaluationJobRepository.java`, `EvaluationLogRepository.java`, `EvaluationSyllableRepository.java`, `WORK_LOG.md`
- 내용: 사용자 평가 작업과 결과·음절 점수를 foreign key 순서로 삭제하는 query를 추가했다.
- 검증: 계정 삭제 JPA 테스트와 Backend 전체 `test integrationTest` 통과
- 리스크: 실제 운영 MySQL 대량 이력 삭제 시간은 미측정

## 2026-07-29 - SQS dispatcher 쿼리 제거

- 변경 파일: `EvaluationJobRepository.java`, `WORK_LOG.md`
- 내용: `enqueued_at` 기반 발행 후보 조회·갱신을 제거하고 DB polling claim 쿼리만 유지했다.
- 검증: 독립 DB Worker 40건 통합 테스트 통과
- 리스크: Worker 다중화 전 실제 MySQL lock 경합 검증 필요

## 2026-07-29 - Queue 발행 대상과 전송 시각 갱신 쿼리

- 변경 파일: `EvaluationJobRepository.java`, `WORK_LOG.md`
- 내용: 실행 가능한 오래된 PENDING 작업 조회와 상태 변경 경쟁을 방어하는 조건부 `enqueued_at` 갱신을 추가했다.
- 검증: dispatcher 단위 테스트와 4 Worker 통합 테스트 통과
- 리스크: 실제 MySQL dispatch 인덱스 실행 계획은 미측정

## 2026-07-29 - 만료 완료 작업 batch 조회 추가

- 변경 파일: `EvaluationJobRepository.java`, `WORK_LOG.md`
- 내용: 성공·최종 실패 작업 중 완료 시점이 보존 기준보다 오래된 ID만 제한 조회하도록 추가했다.
- 검증: Idempotency 정리 통합 테스트 및 전체 Backend 테스트 통과
- 리스크: 운영 DB 정리 query 시간은 미측정

## 2026-07-27 - 평가 작업 claim 조회 추가

- 변경 파일: `EvaluationJobRepository.java`, `WORK_LOG.md`
- 내용: 사용자·Idempotency 조회와 단일 Worker의 비관적 잠금 기반 다음 작업 claim 쿼리를 추가했다.
- 검증: Worker·마이그레이션 테스트 통과
- 리스크: 다중 Worker 확장 시 SKIP LOCKED 또는 Queue 전환 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationLogRepository.java`, `EvaluationSyllableRepository.java`, `SyllableRepository.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
