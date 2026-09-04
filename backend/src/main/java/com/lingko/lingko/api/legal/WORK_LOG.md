# 작업 이력

## 2026-08-07 - 인증 사용자 법무 동의 API 추가

- 변경 파일: `LegalConsentController.java`
- 내용: Bearer Token 사용자만 현재 동의 상태를 조회·제출하도록 `/api/legal/consent` GET·POST를 추가했다. 사용자 ID는 body에서 받지 않는다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 운영 전 인증 endpoint rate limit 공통 정책 필요

## 2026-08-07 - 법무 문서 공개 endpoint 신설

- 변경 파일: `LegalDocumentController.java`
- 내용: `GET /legal/{document}?lang=` 을 추가했다. 이 저장소에서 유일하게 인증을 요구하지 않고 JSON 대신 HTML을 반환하는 controller다. 가입 화면의 미로그인 사용자와 스토어 심사자가 같은 문서를 열어야 하고, 응답 대상이 앱이 아니라 브라우저이기 때문이다. 알 수 없는 문서는 404, 지원하지 않는 언어는 400이 아니라 기본 언어로 되돌린다.
- 검증: `./gradlew test integrationTest` 통과. `LegalDocumentControllerIntegrationTest` 5개
- 리스크: 없음
