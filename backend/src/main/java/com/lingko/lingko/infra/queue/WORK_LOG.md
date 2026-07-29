# 작업 이력

## 2026-07-29 - SQS 평가 Queue adapter

- 변경 파일: `SqsEvaluationJobQueue.java`, `WORK_LOG.md`
- 내용: jobId 전송, long polling, ACK delete와 재시도 visibility 변경을 AWS SDK SQS adapter로 구현했다.
- 검증: SQS request 변환 단위 테스트 통과
- 리스크: 실제 AWS 권한·DLQ·redrive 정책은 운영 환경에서 검증 필요
