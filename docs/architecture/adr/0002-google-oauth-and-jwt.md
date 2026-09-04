# ADR-0002 Google OAuth와 자체 JWT

- 상태: 승인
- 날짜: 2026-07-21

## 배경

모바일 앱에서 Google 계정을 통해 간편 로그인하고, 이후 LingKo API에는 서비스가 통제하는 인증 토큰을 사용해야 합니다.

## 결정

Flutter가 Google ID Token을 획득하고 백엔드가 이를 검증합니다. 백엔드는 소셜 사용자 정보를 생성·갱신한 후 자체 Access/Refresh JWT를 발급합니다.

Access Token과 Refresh Token은 기기 세션별 `sid`를 공유하며 서버에는 현재 Refresh Token의 SHA-256 해시만 저장합니다. 보호 API는 JWT 검증 후 `sid`의 활성 상태를 확인합니다. 갱신 시 같은 세션 행을 잠그고 Access/Refresh Token을 모두 회전합니다. 이전 토큰 재사용은 탈취 신호로 처리해 해당 기기 세션을 폐기하며, 그 세션의 Access Token도 즉시 거부합니다. 절대 만료는 로그인 시점부터 계산하며 회전으로 연장하지 않습니다.

## 대안

- 모든 API 요청에 Google Token 사용
- 서버 세션과 쿠키 사용
- Firebase Authentication 전면 도입

## 결과

- 외부 신원 확인과 내부 권한·만료 정책을 분리할 수 있습니다.
- JWT 키 회전과 모바일 보안 저장소 관리가 필요합니다.
- 로그인마다 독립 세션이 생성되어 현재 기기 로그아웃이 다른 기기에 영향을 주지 않습니다.
- 전체 기기 로그아웃은 사용자 계정의 모든 활성 세션을 폐기하는 별도 기능으로 남습니다.
