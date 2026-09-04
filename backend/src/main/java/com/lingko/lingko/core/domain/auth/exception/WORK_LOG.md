# 작업 이력

## 2026-08-19 - Review 접근 Rate Limit 예외

- 변경 파일: `ReviewAccessRateLimitExceededException.java`, `WORK_LOG.md`
- 내용: 제한 초과의 남은 대기 시간을 HTTP 계층에 전달하는 전용 예외를 추가했다.
- 검증: guard·controller 타깃 테스트와 Backend 전체 테스트 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AuthException.java`, `RefreshTokenReuseException.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 재사용 예외 목적 주석 보완

- 변경 파일: `RefreshTokenReuseException.java`, `WORK_LOG.md`
- 내용: 실패 응답과 세션 폐기 commit을 함께 보장하기 위한 전용 예외 목적을 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - Refresh Token 재사용 탐지 예외

- 변경 파일: `RefreshTokenReuseException.java`, `WORK_LOG.md`
- 내용: 회전 전 토큰 재사용 시 세션 폐기 트랜잭션을 커밋하면서 401을 반환할 전용 예외를 추가했다.
- 검증: 재사용 후 현재 토큰까지 거부되는 서비스 테스트
- 리스크: 없음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
