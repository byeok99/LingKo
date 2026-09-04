# 작업 이력

## 2026-08-09 - 가이드 작업 접근·용량 예외 추가

- 변경 파일: `GuideJobAccessDeniedException.java`, `GuideJobRateLimitExceededException.java`, `GuideJobCapacityExceededException.java`, `WORK_LOG.md`
- 내용: 인증된 일반 사용자 권한 부족과 요청량·동시 실행 초과를 HTTP 계층이 구분할 수 있는 도메인 예외로 분리했다.
- 검증: Controller·서비스 타깃 테스트와 Backend 전체 단위·통합 테스트 통과
- 리스크: 없음

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
