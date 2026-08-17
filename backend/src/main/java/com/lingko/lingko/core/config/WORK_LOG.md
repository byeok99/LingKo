# Work Log

## 2026-08-12 - Apple OAuth audience 설정 추가

- 변경 파일: `AppleOAuthSettings.java`, `WORK_LOG.md`
- 내용: Apple identity token의 허용 audience를 type-safe 설정으로 binding한다.
- 검증: Spring ApplicationContext와 Apple verifier 테스트 통과
- 리스크: 배포 환경 `APPLE_CLIENT_ID` 누락 시 Apple 로그인 fail-closed

## 2026-08-12 - AdMob SSV 정책 설정 추가

- 변경 파일: `AdMobSsvSettings.java`, `WORK_LOG.md`
- 내용: 허용 광고 단위, 보상 항목·수량과 session 만료를 환경 설정으로 제한한다.
- 검증: Spring 대상 테스트 통과
- 리스크: allowlist 미설정 시 fail-closed

## 2026-08-09 - 가이드 작업 admission 설정 추가

- 변경 파일: `GuideGenerationJobSettings.java`, `WORK_LOG.md`
- 내용: API 활성화, 내부 Secret, 분당 요청, 동시 실행 한도를 type-safe 설정으로 추가하고 활성화 시 32자 미만 Secret으로는 시작하지 못하게 했다.
- 검증: `GuideGenerationJobSettingsTest`, 배포 조건 테스트, Backend 전체 단위·통합 테스트 통과
- 리스크: 없음

## 2026-08-03 - Replicate 제한 재시도 설정 추가

- 변경 파일: `ReplicateSettings.java`, `WORK_LOG.md`
- 내용: Prediction 생성·polling의 제한 재시도 횟수와 지수 backoff 초기·최대 지연을 type-safe 설정으로 추가했다.
- 검증: `ReplicateApiClientTest`와 Backend 전체 단위·내부 통합 테스트 통과
- 리스크: 공급자 `Retry-After`와 Jitter 반영은 #44 후속 범위

## 2026-07-30 - 영상 생성용 Worker lease 연장

- 변경 파일: `EvaluationJobSettings.java`, `WORK_LOG.md`
- 내용: 최초 Replicate 보간과 FFmpeg 병합 중 다른 Worker가 작업을 재선점하지 않도록 기본 lease를 600초로 늘렸다.
- 검증: Backend 단위 199개·통합 11개 통과
- 리스크: 실제 최초 생성 시간과 운영 lease 여유는 측정 필요

## 2026-07-29 - SQS 설정 제거

- 변경 파일: `EvaluationJobSettings.java`, `SqsConfig.java`, `WORK_LOG.md`
- 내용: Queue URL·dispatcher·visibility와 Worker mode를 제거하고 DB Worker 활성화·lease·retry 설정만 유지했다.
- 검증: 설정 compile과 Worker 배포 조건 테스트 통과
- 리스크: Queue 재도입 시 현재 운영 측정에 기반한 새 ADR 필요

## 2026-07-29 - SQS Worker 모드 설정

- 변경 파일: `EvaluationJobSettings.java`, `SqsConfig.java`, `WORK_LOG.md`
- 내용: database fallback과 SQS 모드, dispatcher·long polling·visibility·재발행 설정 및 조건부 SQS client를 추가했다.
- 검증: 설정 binding compile과 Queue 대상 테스트 통과
- 리스크: 운영에서는 Queue URL·region·credential 누락 시 SQS mode가 기동하지 않음

## 2026-07-29 - 평가 완료 작업 정리 설정 추가

- 변경 파일: `EvaluationJobSettings.java`, `WORK_LOG.md`
- 내용: 기본 7일 보존, 1시간 주기, 1,000건 batch와 설정값 경계 검증을 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 운영 데이터 증가량 기준의 최적 값은 미측정

## 2026-07-27 - 평가 Worker와 S3 signer 설정

- 변경 파일: `S3Config.java`, `EvaluationJobSettings.java`, `EvaluationWorkerConfig.java`, `WORK_LOG.md`
- 내용: S3Presigner bean과 Worker scheduling, 업로드 만료·lease·재시도 설정을 추가했다.
- 검증: Backend 테스트 및 integrationTest 통과
- 리스크: 다중 인스턴스 Worker 운영은 현재 지원 범위가 아님

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AwsSettings.java`, `AzureSettings.java`, `DBSettings.java`, `FfmpegSettings.java`, `GoogleOAuthSettings.java`, `GoogleSettings.java`, `JwtSettings.java`, `ReplicateSettings.java`, `WebClientConfig.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
