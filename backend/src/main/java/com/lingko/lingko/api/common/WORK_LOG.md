# Work Log

## 2026-08-12 - 광고 보상 안전 오류 매핑

- 변경 파일: `GlobalExceptionHandler.java`, `WORK_LOG.md`
- 내용: SSV 위조 사유를 숨기고 session 없음·일시 실패를 안정된 오류 코드로 변환한다.
- 검증: Controller 테스트 통과
- 리스크: 없음

## 2026-08-09 - 가이드 작업 보안 오류 응답 추가

- 변경 파일: `GlobalExceptionHandler.java`, `WORK_LOG.md`
- 내용: 일반 사용자 403과 요청량·동시 실행 초과 429를 고정 오류 코드와 `Retry-After`로 변환했다.
- 검증: `GuideGenerationJobControllerTest`, Backend 전체 단위·통합 테스트 통과
- 리스크: 없음

## 2026-07-29 - 회원 탈퇴 일시 실패 오류 매핑

- 변경 파일: `GlobalExceptionHandler.java`, `WORK_LOG.md`
- 내용: S3 정리 실패를 내부 원인 없는 503 `ACCOUNT_DELETION_UNAVAILABLE`으로 반환한다.
- 검증: 안전한 오류 응답 Controller 테스트와 Backend 전체 테스트 통과
- 리스크: 오류율 alert는 관측성 작업에서 추가 필요

## 2026-07-27 - 평가 작업 오류 응답 추가

- 변경 파일: `GlobalExceptionHandler.java`, `WORK_LOG.md`
- 내용: Idempotency 충돌과 사용자 소유 평가 작업 미존재를 409·404 표준 오류로 매핑했다.
- 검증: Backend 테스트 통과
- 리스크: 없음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `ErrorResponse.java`, `GlobalExceptionHandler.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 에너지 소진 오류 문구 갱신

- 변경 파일: `GlobalExceptionHandler.java`
- 내용: 폐지된 일일 쿼터 대신 현재 에너지 소진 의미를 전달하도록 변경했다.
- 검증: backend test 및 integrationTest 통과
- 리스크: 없음
