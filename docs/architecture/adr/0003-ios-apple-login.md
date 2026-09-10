# ADR-0003 iOS native Apple 로그인과 nonce 검증

- 상태: 승인 — native identity token 검증 방식
- 날짜: 2026-08-12

## 배경

iOS에서도 Google 로그인을 제공하므로 Apple 계정 로그인 경로가 필요합니다. Apple 이름은 최초 승인
응답에만 있고 identity token에는 포함되지 않으며, 이메일은 실제 주소 또는 private relay 주소일 수
있습니다. 모바일이 전달하는 값을 그대로 계정으로 신뢰하면 다른 앱용 token 재사용과 replay 위험이
생깁니다.

## 결정

- Flutter iOS는 `sign_in_with_apple` native 흐름을 사용하며 Android에는 아직 버튼을 노출하지 않습니다.
- 앱은 매 시도 cryptographically secure raw nonce를 만들고 SHA-256 값만 Apple 요청에 전달합니다.
- Backend 요청은 `provider`, `idToken`, `rawNonce`, 선택 `displayName`을 사용합니다.
- Backend는 Apple 공개 JWK로 RS256 서명을 검증하고 issuer, `APPLE_CLIENT_ID` audience, 만료,
  subject, nonce와 이메일 검증 상태를 모두 확인합니다.
- 계정 고유 키는 변경·가리기가 가능한 이메일이 아니라 Apple `sub`와 `APPLE` provider 조합입니다.
- 최초 이름이 이후 로그인에서 null이면 기존 이름을 보존합니다.

## 결과와 적용 범위

이 결정은 iOS native identity token 검증을 다룹니다. 플랫폼 capability와 provisioning 설정이 필요하며, authorization code 교환·서버 refresh token 보관·Apple 승인 철회까지 구현한 것으로 해석하지 않습니다. Android·Web의 Service ID 기반 흐름과도 구분합니다.
