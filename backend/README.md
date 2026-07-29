# LingKo Backend

LingKo의 Spring Boot REST API입니다. 추천 문장, 표준 발음, 음성 평가, 사용자 인증, 학습 기록, 설정, 일일 쿼터, 가이드 생성 작업을 담당합니다.

## 기술 스택

- Java 21
- Spring Boot 3.4.1
- Spring MVC / Validation / Data JPA / WebFlux
- MySQL 8 / Flyway
- Azure Speech SDK
- AWS SDK for S3 / SQS
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
docker compose up --build
```

SQS 기반 독립 평가 Worker를 사용할 때는 Queue URL을 설정하고 API 내부 Worker를 끈 뒤 Worker 수를 별도로 지정합니다.

```bash
EVALUATION_WORKER_MODE=sqs \
EVALUATION_API_WORKER_ENABLED=false \
docker compose --profile queue up --build --scale evaluation-worker=4
```

API는 DB의 `PENDING` 작업을 SQS에 전달하고 `evaluation-worker`만 메시지를 소비합니다. SQS 메시지는 `jobId`만 포함하며 작업 상태·결과·Idempotency는 MySQL을 원본으로 유지합니다.

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

`externalIntegrationTest`는 Azure, Replicate, S3, FFmpeg 관련 환경변수가 필요합니다.

## API 그룹

- `/api/auth`
- `/api/sentences`
- `/api/pronunciation`
- `/api/evaluations`
- `/api/quota`
- `/api/users/me/preferences`

자세한 계약은 [API 레퍼런스](../docs/api/api-reference.md)를 참고합니다.

## 환경변수

전체 목록은 `.env.example`과 [로컬 개발 가이드](../docs/development/local-development.md)를 기준으로 합니다. 실제 비밀값은 커밋하지 않습니다.

## 현재 주의사항

- 실제 AWS SQS Queue에는 visibility timeout보다 긴 redrive 정책과 DLQ를 설정해야 합니다.
- 가이드 작업 상태는 서버 메모리에 저장됩니다.
- Refresh Token 갱신·폐기 API는 구현됐으며 운영 전 실제 동시 갱신 부하를 확인해야 합니다.
- 운영 전 Actuator, 관측성, 외부 호출 복원력, 백업 정책이 필요합니다.
