# 작업 이력

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
