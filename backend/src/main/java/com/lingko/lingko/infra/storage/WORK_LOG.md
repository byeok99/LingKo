# 작업 이력

## 2026-08-09 - 외부 미디어 URL 정책 경계 연결

- 변경 파일: `ExternalMediaUrlValidator.java`, `S3Uploader.java`, `WORK_LOG.md`
- 내용: validator를 가이드 job 사전 검증 정책으로 연결하고 SSRF·redirect·25MiB 경계를 유지하면서 URL 원문을 로그·예외에서 제거했다.
- 검증: `ExternalMediaUrlValidatorTest`, Backend 전체 단위·통합 테스트 통과; validator line coverage 83.33%
- 리스크: DNS 검증과 실제 연결 사이의 공급자 DNS 변경은 허용 host 신뢰에 의존

## 2026-08-07 - 가이드 캐시 조회 실패가 생성을 막지 않게 변경

- 변경 파일: `S3Uploader.java`
- 내용: `findPublicUrl`이 404만 캐시 미스로 보고 나머지는 예외를 던지고 있었다. IAM 정책에 `s3:ListBucket`이 없으면 S3는 없는 key에 404가 아니라 403을 주는데, 그러면 호출자가 이를 생성 실패로 처리해 정적 이미지로 강등한다. 캐시가 빈 순간부터 영상이 영영 만들어지지 않으면서 오류도 드러나지 않는다. 이 호출은 생성을 건너뛸 수 있는지 묻는 최적화일 뿐이라, 어떤 실패든 "캐시 없음"으로 답하고 원인은 WARN으로 남긴다. 잘못 판단해도 대가는 재생성 비용뿐이다.
- 검증: `./gradlew test`, `./gradlew integrationTest` 통과
- 리스크: 자격증명 오류처럼 지속되는 문제일 때 매번 재생성을 시도해 외부 호출 비용이 는다. WARN 로그로 드러나므로 감지 가능

## 2026-07-30 - S3 가이드 영상 cache 조회

- 변경 파일: `S3Uploader.java`, `WORK_LOG.md`
- 내용: 결정적 key의 object 존재 여부를 HEAD로 확인하고 404만 cache miss로 처리하며 다른 S3 장애는 생성 오류로 전파한다.
- 검증: cache hit·404·503 단위 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 실제 IAM HeadObject 권한은 운영 환경 검증 필요

## 2026-07-29 - 탈퇴 사용자 S3 prefix 완전 삭제

- 변경 파일: `S3EvaluationAudioStorage.java`, `WORK_LOG.md`
- 내용: 사용자 prefix의 current object를 batch 삭제하고 Versioning 버킷의 과거 version·delete marker까지 반복 삭제한다.
- 검증: current·version·장애 단위 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 AWS IAM ListBucketVersions·DeleteObjectVersion 권한과 대량 object 처리시간은 미검증

## 2026-07-27 - 평가 음성 S3 저장 경계 구현

- 변경 파일: `S3EvaluationAudioStorage.java`, `WORK_LOG.md`
- 내용: 사용자별 presigned PUT, HEAD 메타데이터 검증, Worker 다운로드와 성공·실패 후 삭제를 구현했다.
- 검증: S3 경계 단위 테스트 및 Backend 전체 테스트 통과
- 리스크: Bucket CORS·Lifecycle은 AWS 운영 설정 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `ExternalMediaUrlValidator.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
