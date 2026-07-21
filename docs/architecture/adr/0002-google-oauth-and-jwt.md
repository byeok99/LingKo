# ADR-0002 Google OAuth와 자체 JWT

- 상태: 승인
- 날짜: 2026-07-21

## 배경

모바일 앱에서 Google 계정을 통해 간편 로그인하고, 이후 LingKo API에는 서비스가 통제하는 인증 토큰을 사용해야 합니다.

## 결정

Flutter가 Google ID Token을 획득하고 백엔드가 이를 검증합니다. 백엔드는 소셜 사용자 정보를 생성·갱신한 후 자체 Access/Refresh JWT를 발급합니다.

## 대안

- 모든 API 요청에 Google Token 사용
- 서버 세션과 쿠키 사용
- Firebase Authentication 전면 도입

## 결과

- 외부 신원 확인과 내부 권한·만료 정책을 분리할 수 있습니다.
- JWT 키, Refresh Token 회전·폐기, 모바일 보안 저장소 관리가 필요합니다.
- 현재 Refresh Token 갱신과 서버 측 폐기 기능은 후속 작업입니다.
