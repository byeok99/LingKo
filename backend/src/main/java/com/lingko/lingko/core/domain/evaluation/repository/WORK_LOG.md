# 작업 이력

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
