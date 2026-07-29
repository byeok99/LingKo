# 작업 이력

## 2026-07-29 - 평가 Idempotency 보존 계약 추가

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 완료 작업 기본 7일 재사용, batch 정리와 진행 중 작업 보존 정책을 API 계약에 반영했다.
- 검증: Cleanup 구현·설정 및 통합 테스트와 대조
- 리스크: 보존 기간 변경 시 앱의 최대 재시도 기간과 함께 검토 필요

## 2026-07-27 - 비동기 평가 API와 오류 계약 문서화

- 변경 파일: `api-reference.md`, `error-codes.md`, `WORK_LOG.md`
- 내용: S3 업로드 티켓, 작업 생성·조회, Idempotency와 작업 오류 코드를 현재 구현에 맞게 반영했다.
- 검증: Controller·DTO·예외 처리 코드와 대조
- 리스크: 실제 운영 base URL과 S3 CORS는 배포 시 확인 필요

## 2026-07-24 - 인증 평가·쿼터 lifecycle 명세 반영

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 평가 API 인증, 쿼터 예약·확정·복구, 성공 결과 저장과 429 계약을 현재 구현에 맞게 반영했다.
- 검증: Controller·application service 구현과 대조
- 리스크: 동시성·멱등성은 #38·#39 후속 작업

## 2026-07-23 - 인증 갱신·로그아웃 API 명세 추가

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: Refresh Token 갱신과 현재 기기 로그아웃 endpoint, 요청·응답·오류 계약을 추가했다.
- 검증: Backend `AuthController` 및 DTO와 계약 대조
- 리스크: 전체 기기 로그아웃 endpoint는 후속 기능
