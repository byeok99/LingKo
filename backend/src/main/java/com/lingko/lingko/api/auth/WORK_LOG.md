# 작업 이력

## 2026-07-29 - 회원 탈퇴 API 추가

- 변경 파일: `AuthController.java`, `WORK_LOG.md`
- 내용: Bearer Access Token과 현재 Refresh Token을 요구하는 `DELETE /api/auth/account`를 추가했다.
- 검증: Controller 대상 테스트와 Backend 전체 테스트 통과
- 리스크: endpoint별 Rate Limit은 기존 후속 보안 작업 범위

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AuthController.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 endpoint 목적 주석 보완

- 변경 파일: `AuthController.java`, `WORK_LOG.md`
- 내용: 로그인, token 회전, 현재 기기 로그아웃 endpoint의 책임을 Javadoc으로 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - Refresh Token 갱신·로그아웃 엔드포인트

- 변경 파일: `AuthController.java`, `WORK_LOG.md`
- 내용: `POST /api/auth/token/refresh`와 `POST /api/auth/logout` 계약을 추가했다.
- 검증: AuthController 단위 테스트와 Backend 전체 테스트
- 리스크: 인증 rate limit은 별도 운영 보안 작업으로 남음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
