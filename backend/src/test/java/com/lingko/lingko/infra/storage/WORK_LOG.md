# 작업 이력

## 2026-08-09 - 외부 미디어 URL 비노출·응답 경계 테스트

- 변경 파일: `ExternalMediaUrlValidatorTest.java`, `WORK_LOG.md`
- 내용: malformed·비허용 URL의 민감 query 비노출과 정상/공급자 오류 연결 경계를 추가 검증했다.
- 검증: 타깃 테스트와 Backend 전체 단위·통합 테스트 통과
- 리스크: 없음

## 2026-08-07 - 캐시 조회 실패 계약을 반대로 고정

- 변경 파일: `S3UploaderTest.java`
- 내용: 기존 `propagatesStorageFailure`("S3 장애는 캐시 미스로 숨기지 않는다")를 없애고 403·503·SdkException이 모두 빈 결과가 되는 것으로 바꿨다. 원래 의도(장애를 숨기지 않는다)는 타당했지만, 이 지점에서 예외를 올리면 캐시 조회 하나가 실제 생성을 막는 대가가 더 크다. 장애는 WARN 로그로 드러난다. 403 케이스는 `s3:ListBucket` 권한이 없는 환경에서 실제로 발생하는 조건이라 별도 테스트로 뒀다.
- 검증: `./gradlew test` 통과
- 리스크: 없음

## 2026-07-30 - S3 영상 cache 조회 회귀 테스트

- 변경 파일: `S3UploaderTest.java`, `WORK_LOG.md`
- 내용: 기존 object URL 반환, 404 cache miss, 503 장애 전파를 검증했다.
- 검증: 대상 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 실제 S3 통신은 포함하지 않음

## 2026-07-29 - S3 탈퇴 삭제 회귀 테스트

- 변경 파일: `S3EvaluationAudioStorageTest.java`, `WORK_LOG.md`
- 내용: 사용자 prefix batch 삭제, Versioning 과거 원본·delete marker 삭제와 장애 전파를 검증했다.
- 검증: 대상 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 AWS E2E는 미실행

## 2026-07-27 - S3 업로드 검증 오류 테스트

- 변경 파일: `S3EvaluationAudioStorageTest.java`, `WORK_LOG.md`
- 내용: 존재하지 않는 업로드 객체가 내부 오류가 아닌 입력 오류로 변환되는 계약을 검증했다.
- 검증: 대상 단위 테스트 통과
- 리스크: 실제 AWS 권한·CORS는 운영 환경 검증 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `ExternalMediaUrlValidatorTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
