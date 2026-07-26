# 작업 이력

## 2026-07-26 - 쿼터 동시성 설계 구현 상태 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: 쿼터 예약 기준을 예약량 포함 조건부 UPDATE로 구체화하고 최초 생성 부모 lock, 무재시도 429, 외부 호출 중 lock 미유지 정책을 반영했다.
- 검증: `PracticeQuotaService`, repository 원자 SQL, ADR-0006과 대조
- 리스크: 운영 부하에서 응답시간과 lock wait 측정 필요
