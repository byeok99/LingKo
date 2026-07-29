# 작업 이력

## 2026-07-29 - 평가 Idempotency 저장소 수명 정책 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: 동시 요청 단일화와 완료 작업 7일 보존·제한 batch 정리 정책을 확장 계획에 반영했다.
- 검증: 동시성·정리 통합 테스트와 구현 대조
- 리스크: 실제 처리량과 정리 query 비용 미측정

## 2026-07-27 - 음성 평가 부하 격리 단계 갱신

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: API 서버 multipart 처리 제거와 단일 DB Worker 도입 상태, Queue 전환 조건을 반영했다.
- 검증: ADR·구현 구조와 대조
- 리스크: 실제 처리량과 작업 대기시간 측정 필요

## 2026-07-26 - 쿼터 동시성 설계 구현 상태 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: 쿼터 예약 기준을 예약량 포함 조건부 UPDATE로 구체화하고 최초 생성 부모 lock, 무재시도 429, 외부 호출 중 lock 미유지 정책을 반영했다.
- 검증: `PracticeQuotaService`, repository 원자 SQL, ADR-0006과 대조
- 리스크: 운영 부하에서 응답시간과 lock wait 측정 필요
