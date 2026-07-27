# 작업 이력

## 2026-07-27 - 평가 작업 도메인 예외 추가

- 변경 파일: `EvaluationJobConflictException.java`, `EvaluationJobNotFoundException.java`, `WORK_LOG.md`
- 내용: Idempotency payload 충돌과 사용자 소유 작업 미존재를 구분하는 예외를 추가했다.
- 검증: Service·Controller 테스트 통과
- 리스크: 없음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
