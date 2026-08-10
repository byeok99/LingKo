# LingKo Backend

LingKo의 Spring Boot REST API입니다. 추천 문장, 표준 발음, 음성 평가, 사용자 인증, 학습 기록, 설정, 일일 쿼터, 가이드 생성 작업을 담당합니다.

추천 문장에는 표준 발음 정답을 저장하지 않습니다. 추천·자유 문장 모두 정규화한 원문을 현재 `KoreanPhonemeUtil` 규칙으로 변환하며, 평가 기록과 비동기 작업에는 당시 평가 재현을 위한 snapshot만 저장합니다.

## 기술 스택

- Java 21
- Spring Boot 3.4.1
- Spring MVC / Validation / Data JPA / WebFlux
- MySQL 8 / Flyway
- Azure Speech SDK
- AWS SDK for S3
- Gradle / JUnit 5 / JaCoCo
- Docker / FFmpeg

## 주요 패키지

```text
src/main/java/com/lingko/lingko/
├── api/        # HTTP Controller와 요청·응답 DTO
├── core/       # 도메인 모델과 서비스
└── infra/      # 외부 서비스·스토리지 구현
```

## 실행

```bash
cp .env.example .env
cp application.example.yaml src/main/resources/application.yaml
docker compose up --build
```

기본 Compose 구성은 API 내부 평가 Worker를 끄고 web server가 없는 DB polling Worker 컨테이너 1개를 함께 실행합니다.

```bash
docker compose up --build
```

API는 `evaluation_jobs`에 작업을 저장하고 `evaluation-worker`가 DB lock과 lease로 한 건씩 claim합니다. Worker는 Azure 평가 후 결과 화면에서 열 수 있는 모든 음절의 다중 프레임 입·혀 가이드를 Replicate와 FFmpeg로 MP4화합니다. 생성 MP4 URL은 기존 `syllables` 테이블에 upsert하고 동일 음절·종류는 DB를 먼저, 동일 음절·종류·프레임 조합은 결정적 S3 cache를 다음으로 재사용합니다. 단일 프레임이나 외부 생성 실패는 PNG로 fallback합니다. 최초 cache miss 시간을 고려해 기본 lease는 600초이며, 초기 운영에서는 Worker 1개를 유지하고 실제 대기시간과 DB lock을 측정한 뒤에만 replica 확장을 검토합니다.

별도의 `/api/pronunciation/guide-jobs` HTTP surface는 비용 남용을 막기 위해 기본 비활성화되어 있습니다. 내부 도구에서 사용할 때만 `GUIDE_JOBS_API_ENABLED=true`와 32자 이상의 별도 `GUIDE_JOBS_INTERNAL_TOKEN` Secret을 설정합니다. 생성 요청은 기본 분당 2회, 동시 1개로 제한하며 값은 `GUIDE_JOBS_REQUESTS_PER_MINUTE`, `GUIDE_JOBS_MAX_CONCURRENT`로 낮은 범위 안에서 조정합니다.

또는 MySQL과 환경변수를 별도로 준비한 후:

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
./gradlew integrationTest
./gradlew externalIntegrationTest
```

`externalIntegrationTest`는 Azure, Replicate, S3, FFmpeg 관련 환경변수가 필요합니다. 실제 영상 생성 E2E 전에는 `REPLICATE_*`, AWS credential·bucket, FFmpeg 실행 가능 여부를 확인해야 합니다. Replicate 생성 요청은 429·5xx에 제한된 지수 backoff를 적용하고 polling 기한을 넘긴 원격 Prediction은 취소합니다.

출시 전 생성 완료된 가이드 MP4는 `src/main/resources/db/migration/R__seed_generated_syllable_guides.sql`에 누적합니다. 이 repeatable migration은 내용이 바뀌면 다시 실행되며 기존 `syllables` 행의 비어 있지 않은 입·혀 URL을 보존하면서 새 초기값을 upsert합니다.

## API 그룹

- `/api/auth`
- `/api/sentences`
- `/api/pronunciation`
- `/api/evaluations`
- `/api/quota`
- `/api/users/me/preferences`

자세한 계약은 [API 레퍼런스](../docs/api/api-reference.md)를 참고합니다.

`DELETE /api/auth/account`는 현재 Access Token과 Refresh Token을 함께 재확인한 뒤 사용자 소유 S3 음성 version과 DB 데이터를 삭제합니다. 운영 버킷에는 미제출·삭제 실패 음성을 최대 1일 뒤 만료시키는 [`aws/s3-lifecycle.json`](aws/s3-lifecycle.json)을 별도로 적용해야 하며 실제 AWS 검증은 [#71](https://github.com/byeok99/LingKo/issues/71)에서 추적합니다.

## 환경변수

전체 목록은 `.env.example`과 [로컬 개발 가이드](../docs/development/local-development.md)를 기준으로 합니다. 실제 비밀값은 커밋하지 않습니다.

## 현재 주의사항

- 현재 평가 Worker는 Queue 없이 MySQL을 polling하므로 Worker 수를 늘리기 전에 DB lock 경합을 검증해야 합니다.
- 가이드 작업 상태는 서버 메모리에 저장됩니다.
- Refresh Token 갱신·폐기 API는 구현됐으며 운영 전 실제 동시 갱신 부하를 확인해야 합니다.
- S3 Lifecycle 파일은 저장소 산출물이며 AWS 운영 버킷에는 자동 적용되지 않습니다.
- 가이드 job 기본 지표는 Micrometer에 기록되지만 운영 전 Actuator 노출 정책·alert, 외부 호출 복원력, 백업 정책이 필요합니다.
