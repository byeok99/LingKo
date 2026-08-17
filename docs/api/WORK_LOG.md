# 작업 이력

## 2026-08-12 - Apple OAuth API 계약 추가

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: `APPLE` provider의 identity token·raw nonce·최초 이름과 검증 조건을 문서화했다.
- 검증: DTO·service·controller 테스트와 대조
- 리스크: authorization code 교환 endpoint는 후속 필요

## 2026-08-12 - 광고 보상 SSV API 계약 갱신

- 변경 파일: `api-reference.md`, `error-codes.md`, `WORK_LOG.md`
- 내용: session 생성·조회, signed callback, legacy 410과 오류 코드를 문서화했다.
- 검증: Backend Controller·DTO와 대조
- 리스크: 없음

## 2026-08-12 - 저장 문장 API 문서화와 동의 오류 처리 보강

- 변경 파일: `api-reference.md`, `error-codes.md`
- 내용: 구현돼 있으나 문서에 없던 `GET /api/sentences/saved`와 `PATCH /api/sentences/saved/{sentenceId}`를 추가했다. 앱이 원하는 상태를 보내지 않고 서버가 실제 상태를 뒤집는 이유(두 기기 동시 조작 시 늦게 도착한 요청이 이전 상태를 되살리는 것 방지)를 계약으로 적었다. 오류 문서에는 동의 버전 불일치·필수 동의 누락이 `INVALID_REQUEST`로 온다는 점과, 동의 확인 실패를 통과로 해석하면 안 된다는 처리 원칙을 넣었다.
- 검증: `SavedSentenceController`·DTO·`LegalConsentService`의 예외 경로와 대조
- 리스크: 없음

## 2026-08-09 - 가이드 작업 내부 API 계약 갱신

- 변경 파일: `api-reference.md`, `error-codes.md`, `WORK_LOG.md`
- 내용: 기본 비활성화, 내부 Secret header, 입력·URL 제한, 401/403/429와 `Retry-After` 계약을 문서화했다.
- 검증: Controller·설정·예외 handler와 대조, Backend 전체 단위·통합 테스트 통과
- 리스크: job 상태는 서버 memory에만 존재

## 2026-08-08 - 광고 보상 quota API 계약 추가

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 인증, event ID validation, +1/cap 5, 멱등성, 자연 충전 timer 보존 응답을 문서화했다.
- 검증: controller/service 테스트와 예시 계약 대조
- 리스크: 현재 client event ID 계약은 테스트 단계이며 운영 전 SSV transaction 계약으로 강화해야 한다

## 2026-08-07 - 법무 동의 API 계약 문서화

- 변경 파일: `api-reference.md`
- 내용: 인증 상태 조회·제출 경로, validation, 사용자 귀속, 서버 기록 시각과 idempotency를 문서화했다.
- 검증: Controller·DTO·Flutter client와 대조
- 리스크: 없음

## 2026-08-07 - 법무 문서 공개 endpoint 문서화

- 변경 파일: `api-reference.md`
- 내용: `GET /legal/{document}` 을 추가했다. 인증이 필요 없는 이유, `lang` 파라미터가 오류 대신 기본값으로 되돌아가는 이유, 응답 헤더, 그리고 문서 원본과 리소스 사본의 관계를 적었다.
- 검증: 구현·테스트와 대조
- 리스크: 없음

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `api-reference.md`
- 내용: `GET /api/evaluations/me/weak-sounds`와 `GET /api/evaluations/me/sounds/{character}`를 문서화했다. 두 endpoint는 이전(weak-words)에도 문서에 없던 공백이었다. `averageScore`가 측정값이 아니라 어절 점수를 귀속시킨 가중 평균이라는 점, 최소 2회 조건, 한글 음절만 집계한다는 점을 계약으로 적었다.
- 검증: 코드와 대조. 실행 검증 없음(문서)
- 리스크: 저장 문장 endpoint(`/me/saved-sentences`)는 여전히 문서에 없다. 이번 변경 범위 밖이라 남겨둠

## 2026-08-05 - 로마자 발음 API 문서화

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: 추천·준비·기록 응답의 `romanizedPronunciation` 파생 규칙을 문서화했다.
- 검증: 코드 DTO·service 계약과 대조
- 리스크: 없음

## 2026-08-04 - 사용자 설정 API 문서 제거

- 변경 파일: `api-reference.md`
- 내용: 제거한 endpoint 절을 삭제했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - preferences API 계약 갱신

- 변경 파일: `api-reference.md`
- 내용: displayLanguage를 계약에서 제거하고 제거 사유를 문서에 남겼다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-04 - 상태값 계약과 text 상한 문서화

- 변경 파일: `api-reference.md`
- 내용: scoreStatus 허용 값 표와 미지 값 처리 규칙, guideStatus와의 축 분리를 명시하고 평가 작업 text 상한을 100자로 기록했다.
- 검증: 문서 내용과 구현 대조
- 리스크: 없음

## 2026-08-04 - 단어 중심 평가 API 문서

- 변경 파일: `api-reference.md`, `WORK_LOG.md`
- 내용: Result·Review의 `wordScoreStatus`, `words`, guide-only `syllables` 계약과 신뢰 조건을 문서화했다.
- 검증: 코드 DTO·Flutter parser와 field 대조
- 리스크: 없음

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
## 2026-08-03 - 발음 평가 에너지 API 문서화

- 변경 파일: `api-reference.md`, `error-codes.md`
- 내용: 시간 충전 응답과 에너지 소진 오류를 현재 구현에 맞췄다.
- 검증: 코드 응답 field 및 오류 문구 대조
- 리스크: `/today` 경로명은 호환성을 위해 유지
