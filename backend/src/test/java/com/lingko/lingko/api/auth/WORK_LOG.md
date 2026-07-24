# 작업 이력

## 2026-07-24 - MockBean 제거 예정 API 교체

- 변경 파일: `AuthControllerTest.java`, `WORK_LOG.md`
- 내용: Spring Boot 4에서 제거 예정인 `@MockBean`을 Spring Framework의 `@MockitoBean`으로 교체해 Controller slice test의 mock Bean 재정의 동작을 유지했다.
- 검증: 영향받은 Controller 테스트, `./gradlew cleanTest test integrationTest`, deprecated `MockBean` 검색 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AuthControllerTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 API 테스트 목적 주석 보완

- 변경 파일: `AuthControllerTest.java`, `WORK_LOG.md`
- 내용: 로그인·회전·로그아웃 HTTP 계약 검증 목적을 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - Refresh Token API 계약 테스트

- 변경 파일: `AuthControllerTest.java`, `WORK_LOG.md`
- 내용: refresh 성공, logout 204, 빈 토큰 400, 폐기 토큰 401 응답을 검증했다.
- 검증: AuthController 단위 테스트와 Backend 전체 테스트
- 리스크: 없음

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
