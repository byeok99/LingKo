# 작업 이력

## 2026-08-19 - 기존 review 사용자 세션 발급

- 변경 파일: `AuthService.java`, `WORK_LOG.md`
- 내용: 코드 검증을 통과한 기존 사용자에게만 일반 OAuth와 동일한 회전·폐기 가능 세션을 발급하고 발급 로직을 공유한다.
- 검증: `AuthServiceTest`, Backend 전체 단위·통합 테스트 통과
- 리스크: review 사용자가 삭제되면 운영 설정의 사용자 ID 재지정 필요

## 2026-08-18 - OAuth 신원 신뢰 경계 주석 보강

- 변경 파일: `AuthService.java`, `WORK_LOG.md`
- 내용: provider verifier 결과만 신뢰하고 Apple 최초 이름만 제한적으로 보완하는 로그인 규칙을 Javadoc에 명시했다.
- 검증: Backend 단위 테스트 293개·통합 테스트 16개 통과
- 리스크: 동작 변경 없음

## 2026-08-12 - 공급자별 OAuth verifier 주입

- 변경 파일: `AuthService.java`, `OAuthIdentityVerifier.java`, `WORK_LOG.md`
- 내용: verifier 목록 생성자 주입으로 Google·Apple을 선택하고 Apple 최초 이름만 제한적으로 보완한다.
- 검증: provider 선택·신규/재로그인·client 이름 불신 테스트와 Backend 전체 테스트 통과
- 리스크: endpoint 공통 rate limit은 후속 보안 과제

## 2026-07-29 - 회원 탈퇴 현재 세션 재확인

- 변경 파일: `AuthService.java`, `WORK_LOG.md`
- 내용: 탈퇴 전 Access Token 사용자와 만료·폐기되지 않은 현재 Refresh Token 소유자·hash 일치를 검증한다.
- 검증: 현재·다른 사용자·회전 전 token 대상 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 동시 token 회전과 탈퇴 경합은 운영 부하 테스트 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `ActiveSessionAuthenticator.java`, `AuthService.java`, `JwtTokenProvider.java`, `OAuthIdentity.java`, `OAuthIdentityVerifier.java`, `RefreshTokenHasher.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 서비스 보안 목적 주석 보완

- 변경 파일: `ActiveSessionAuthenticator.java`, `AuthService.java`, `JwtTokenProvider.java`, `RefreshTokenHasher.java`, `WORK_LOG.md`
- 내용: 활성 세션 검증, 원자적 회전, 재사용 폐기, claim과 해시 비교의 보안 목적을 Javadoc과 블록 주석으로 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - Refresh Token 회전·폐기 서비스

- 변경 파일: `ActiveSessionAuthenticator.java`, `AuthService.java`, `JwtTokenProvider.java`, `RefreshTokenHasher.java`, `WORK_LOG.md`
- 내용: `sid`·`jti` claim, SHA-256 원문 비저장, 절대 만료, 비관적 잠금 회전, 재사용 탐지와 현재 기기 로그아웃을 구현했다. 보호 API는 Access Token의 활성 세션까지 확인해 폐기 즉시 접근을 차단한다.
- 검증: AuthService 회전·재사용·만료·로그아웃 테스트와 Backend 전체 테스트
- 리스크: JWT 서명 키 `kid` 회전과 전체 기기 로그아웃은 후속 작업

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
