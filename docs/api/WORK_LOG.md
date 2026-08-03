# 작업 이력

## 2026-08-03 - Preferences API 목표 레벨 제거

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 사용자 설정 조회·변경 계약을 표시 언어와 모국어만 반환·수신하는 현재 DTO에 맞추고 목표 레벨 예시와 enum 목록을 제거했다.
- 검증: Controller test와 API 문구 대조, Backend 단위·통합 테스트 217개 통과
- 리스크: 없음

## 2026-07-30 - 점수 독립 영상 가이드 계약

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 평가 완료 모든 다중 프레임 음절은 글자 점수 제공 여부와 관계없이 MP4를 반환하는 계약으로 보완했다.
- 검증: Backend 단위 201개·통합 11개 테스트 대조
- 리스크: 실제 Replicate·S3 E2E 필요

## 2026-07-30 - 취약 음절 영상 가이드 계약

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 준비 응답의 PNG, 평가 결과의 취약 음절 MP4, 단일 프레임·생성 실패 PNG fallback과 결정적 S3 cache 재사용을 명시했다.
- 검증: Backend 단위 199개·통합 11개와 Flutter 70개 테스트 대조
- 리스크: 실제 외부 서비스 E2E는 미실행

## 2026-07-30 - 동적 표준 발음·가이드 매체 계약

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 추천 발음은 DB 정답이 아닌 현재 규칙 계산값이며 입력 정규화와 이미지·영상 URL 해석 계약을 명시했다.
- 검증: Controller·service·Flutter API 테스트와 대조
- 리스크: 현재 기본 가이드 mapping은 PNG

## 2026-07-29 - 회원 탈퇴 API·오류 계약 문서화

- 변경 파일: `api-reference.md`, `error-codes.md`, `WORK_LOG.md`
- 내용: `DELETE /api/auth/account`, 두 token 재확인, S3 우선 삭제와 재시도 가능한 503 계약을 추가했다.
- 검증: Controller·서비스·오류 handler와 대조
- 리스크: 운영 API E2E는 미실행

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
