# LingKo Backend

Java 21 / Spring Boot 기반 API와 독립 평가 Worker입니다. API는 사용자 요청을 검증하고 작업을 등록하며, Worker는 DB에 저장된 작업을 받아 외부 음성 평가와 가이드 처리를 수행합니다.

## 구조

- `api`: Controller, 요청·응답 DTO, 입력 검증
- `core/domain`: 사용자·세션·동의·문장·평가·쿼터의 업무 규칙과 트랜잭션
- `infra`: Azure Speech, S3, Replicate 등 외부 연동
- `src/main/resources/db/migration`: Flyway 스키마와 기준 데이터
- `src/test`, `src/integrationTest`: 단위·통합 테스트

API와 Worker는 같은 코드베이스를 사용하지만 별도 프로세스로 실행합니다. 기본 구성은 MySQL DB polling Worker 한 개이며, 별도의 메시지 브로커는 사용하지 않습니다.

## 핵심 계약

- 인증 사용자는 업로드 티켓을 발급받아 비공개 S3로 WAV를 업로드합니다.
- 작업 생성 시 소유권과 Idempotency 키를 검사하고 평가 기회를 예약합니다.
- 작업 성공 시 결과 저장과 예약 확정, 실패 시 예약 복구를 처리합니다.
- 추천 문장은 기본 48개이며 표준 발음과 로마자는 현재 변환 규칙으로 계산합니다.
- 평가 기회는 최대 5회, 서버 시각 기준 1시간마다 1회 충전합니다. 날짜 변경으로 초기화하지 않습니다.
- 약관 동의는 서버의 현재 문서 버전을 기준으로 기록합니다.
- `/app-ads.txt`는 인증 없이 AdMob 승인 판매자 레코드를 `text/plain`으로 제공합니다.

## 실행과 검증

환경변수는 [예시 설정](.env.example)을 기준으로 준비합니다. 비밀값은 저장소에 저장하지 않습니다.

```bash
./gradlew bootRun
./gradlew test
./gradlew integrationTest
```

외부 서비스 호출 테스트는 별도 작업이며 자격증명과 비용이 필요할 수 있습니다. 상세 설정과 실행 범위는 [로컬 개발](../docs/development/local-development.md), [테스트 구성](../docs/development/testing-and-troubleshooting.md)을 참고합니다.

### 가이드 미디어 사전 생성

현대 한글 11,172자를 실제 입·혀 프레임 시퀀스로 변환한 뒤 중복을 제거해 cache를 미리 채울 수 있습니다. 인자 없이 실행하면 외부 서비스를 호출하지 않는 dry-run이며, 현재 `phonology-v3` 기준 결과 영상 693개와 고유 전이 clip 94개를 계획합니다.

```bash
./gradlew guidePrewarm
./gradlew guidePrewarm --args='--prewarm-type=MOUTH --prewarm-offset=0 --prewarm-limit=25'
```

실제 생성은 AWS·Replicate·FFmpeg 설정을 환경변수로 전달하고 명시적으로 활성화합니다. 완료된 최종 영상과 전이 clip은 내용 기반 S3 key로 재사용하므로 같은 명령을 다시 실행해도 cache hit 항목은 외부 AI를 호출하지 않습니다.

```bash
./gradlew guidePrewarm --args='--prewarm-dry-run=false --prewarm-type=MOUTH --prewarm-offset=0 --prewarm-limit=25'
```

`.env`를 shell에서 직접 `source`하지 않고 Compose의 env-file parser로 전달하려면 운영 profile의 일회성 container를 사용합니다.

```bash
docker compose --profile operations run --rm --build guide-prewarm
docker compose --profile operations run --rm --build guide-prewarm \
  --prewarm-dry-run=false --prewarm-type=MOUTH --prewarm-offset=0 --prewarm-limit=25
```

`--prewarm-type`은 `MOUTH` 또는 `TONGUE`, `--prewarm-offset`과 `--prewarm-limit`은 재개 범위를 지정합니다. 기본값은 항목별 실패를 기록하고 다음 항목을 계속 처리하며, 즉시 중단하려면 `--prewarm-continue-on-error=false`를 사용합니다. 이 작업은 DB와 API 서버를 시작하지 않지만 실제 생성 시 외부 서비스 비용과 실행 시간이 발생합니다.

[API 레퍼런스](../docs/api/api-reference.md) · [평가 흐름](../docs/architecture/evaluation-flow.md) · [데이터 모델](../docs/data/data-model.md)
