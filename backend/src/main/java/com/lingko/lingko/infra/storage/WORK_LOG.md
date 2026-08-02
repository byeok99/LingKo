# 작업 이력

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
