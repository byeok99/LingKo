# 테스트 구성과 재현 범위

## 기본 검증

백엔드 디렉터리:

```bash
./gradlew compileJava
./gradlew test
./gradlew integrationTest
```

앱 디렉터리:

```bash
flutter analyze
flutter test
```

명령 목록은 실행 결과가 아닙니다. 변경마다 실제 수행한 범위와 결과를 구분합니다.

## Backend CI

`develop` 대상 Pull Request를 열거나 새 commit을 push하면 GitHub Actions의 `Backend CI`가 실행됩니다. Java 21 환경에서 단위 test, 통합 test, 합산 line coverage 80% 기준과 `bootJar` 생성을 순서대로 검증합니다.

CI 성공은 운영 배포를 의미하지 않습니다. 현재 workflow는 검증만 수행하며, 외부 서비스 자격증명과 비용이 필요한 `externalIntegrationTest`는 실행하지 않습니다. `Backend test`는 `develop` ruleset의 required status check입니다.

## Flutter CI

`develop` 대상 Pull Request에는 `Flutter CI`도 실행됩니다. 프로젝트 생성 환경과 같은 Flutter 3.29.1을 설치하고 `pubspec.lock`을 강제한 의존성 설치, 정적 분석, 전체 test와 line coverage 80% 기준을 검증합니다.

이 workflow는 Linux에서 실행하는 Dart·Flutter test gate입니다. iOS 서명 build, App Store 배포, 실제 기기 권한과 외부 API 연결은 검증하지 않습니다. `Flutter test`는 `develop` ruleset의 required status check입니다.

## Docker CI

`develop` 대상 Pull Request에는 `Docker CI`도 실행됩니다. placeholder DB 비밀번호와 추적된 `.env.example`로 Compose 설정을 검증하고, Backend Dockerfile로 Registry에 push하지 않는 임시 이미지를 빌드합니다. API, evaluation worker와 guide prewarm은 같은 Dockerfile을 사용하므로 한 번의 image build로 공통 runtime을 검증합니다.

빌드가 끝나면 image 안의 Spring Boot JAR, Java와 FFmpeg 실행 가능 여부를 확인합니다. 이 검증은 컨테이너 기동, MySQL 연결, 운영 Secret, Registry 업로드와 EC2 배포를 포함하지 않습니다. `Docker build`는 `develop` ruleset의 required status check입니다.

## develop 병합 보호

`protect-develop` ruleset은 `develop` 변경에 Pull Request를 요구하고 `Backend test`, `Flutter test`, `Docker build`가 모두 성공해야 병합을 허용합니다. 세 이름은 GitHub required status check가 workflow가 아닌 job 이름을 사용한다는 계약에 맞춰 고유하게 유지합니다.

필수 check는 최신 `develop` 기준으로 다시 검증하며 GitHub Actions가 생성한 결과만 인정합니다. ruleset에는 bypass 사용자를 두지 않고 branch 삭제와 force push를 차단합니다. 개인 프로젝트이므로 다른 사용자의 승인은 요구하지 않지만, 작성자 자신도 PR과 CI 검증은 우회할 수 없습니다. 병합 이후 수동으로 실행하는 `Backend CD`는 required check에 포함하지 않습니다.

Required check 이름 회귀 검증:

```bash
./scripts/tests/ci-required-checks-test.sh
```

## Backend CD

`develop` 병합은 운영 배포를 자동 실행하지 않습니다. 배포할 commit을 CI에서 검증하고 `Actions` → `Backend CD` → `Run workflow`에서 `develop`을 선택한 뒤 운영 배포 확인 항목을 체크해야 `production` Environment 배포가 시작됩니다. 이는 배포 가능한 상태를 계속 유지하되 운영 반영은 사람이 결정하는 Continuous Delivery 정책입니다.

Environment의 deployment branch policy는 `develop`만 허용합니다. workflow도 선택한 ref가 `develop`인지, 명시적 승인 입력이 `true`인지 다시 검사합니다. 동시 배포는 하나로 제한하며 진행 중인 배포를 새 실행이 취소하지 않습니다.

GitHub Actions는 장기 AWS Access Key를 저장하지 않습니다. `id-token: write`로 발급받은 OIDC token을 AWS STS의 단기 자격 증명으로 교환하며, AWS role의 신뢰 조건은 `repo:byeok99/LingKo:environment:production`으로 제한합니다. Environment에는 다음 비밀값이 아닌 설정값이 필요합니다.

- `AWS_REGION`: 현재 운영 리전
- `AWS_DEPLOY_ROLE_ARN`: GitHub OIDC가 assume할 최소 권한 role
- `AWS_EC2_INSTANCE_ID`: SSM managed node로 등록된 배포 대상 하나

배포 role은 대상 instance와 AWS 관리 문서 `AWS-RunShellScript`에 대한 `ssm:SendCommand`, 대상의 Online 여부와 실행 결과를 확인할 최소 조회 권한만 가집니다. 애플리케이션 S3 key나 EC2 instance role을 GitHub에 공유하지 않습니다.

EC2 명령은 원격 `develop`이 workflow의 정확한 commit인지, 서버의 추적 파일이 깨끗한지 확인하고 fast-forward만 허용합니다. 이후 commit SHA로 공용 Backend image를 한 번 빌드해 API와 evaluation worker를 함께 교체합니다. DB indicator를 포함한 `/actuator/health` 응답, 두 container의 실행 상태와 `RestartCount=0`을 확인하고 15초 안정화 뒤 같은 계약을 다시 통과해야 성공합니다. 이중 확인은 restart policy가 시작 직후의 crash를 잠시 `Running`으로 보이게 하는 구간을 정상 배포로 오인하지 않기 위한 것입니다.

교체 전 image는 `rollback-<UTC 시각>` tag로 보존합니다. 새 image의 명령 실행 또는 health check가 실패하면 API와 Worker를 직전 image로 되돌리고 workflow를 실패 처리합니다. 이 rollback은 database migration이나 서버 Git checkout을 되돌리지 않으므로, Flyway migration은 항상 이전 애플리케이션과 호환되는 순방향 변경이어야 합니다.

배포 스크립트 회귀 검증:

```bash
./scripts/tests/deploy-backend-test.sh
```

이 테스트는 AWS나 운영 Docker를 호출하지 않고 mock command로 잘못된 SHA·dirty checkout 차단, 정확한 image tag 배포, health check 실패와 container 재시작의 rollback, 안정화 전후 재확인을 검증합니다. 실제 OIDC·SSM·운영 네트워크는 production 실행에서 별도로 검증합니다.

## CloudWatch 애플리케이션 로그

Spring Boot는 별도 파일을 만들지 않고 stdout으로만 기록합니다. 기본 `docker-compose.yml`은 로컬 개발이 AWS 자격 증명을 요구하거나 운영 로그와 섞이지 않도록 모든 서비스에 회전되는 `json-file`을 사용합니다. 운영 배포 스크립트만 `docker-compose.prod.yml`을 함께 합성해 API stdout을 `/lingko/prod/api`, 평가 Worker stdout을 `/lingko/prod/worker` Log Group으로 전송합니다. Docker가 각 container ID를 stream 이름으로 사용하므로 재배포된 container가 기존 stream을 공유하지 않습니다. 두 Group은 `us-east-1`에서 14일 보관하며 기본 애플리케이션 로그 레벨은 `INFO`입니다.

운영 API·Worker의 `awslogs`는 `mode=non-blocking`, `max-buffer-size=4m`으로 설정해 CloudWatch 전송 지연이 애플리케이션 실행을 직접 막지 않게 합니다. 이 모드는 buffer가 가득 차면 일부 로그를 버릴 수 있으므로 장애 시 애플리케이션 오류와 함께 Docker daemon 로그도 확인해야 합니다. 운영 MySQL과 수동 `guide-prewarm`, 로컬의 모든 서비스는 CloudWatch로 보내지 않고 `json-file`을 파일당 10 MiB, 최대 3개로 회전합니다.

운영 `.env`에서 목적지를 바꿔야 할 때만 다음 비밀값이 아닌 설정을 사용합니다.

- `AWS_LOGS_REGION` (기본 `us-east-1`)
- `AWS_LOGS_API_GROUP` (기본 `/lingko/prod/api`)
- `AWS_LOGS_WORKER_GROUP` (기본 `/lingko/prod/worker`)

배포 스크립트는 새 image를 만든 뒤 API와 Worker의 각 Log Group에 일회성 canary container를 실행합니다. Docker logging driver 초기화에 실패하면 실행 중인 container를 교체하기 전에 배포를 중단합니다. Log Group은 자동 생성하지 않으므로 미리 만들고 EC2 Instance Role에 `logs:CreateLogStream`과 `logs:PutLogEvents` 권한을 부여해야 합니다.

로컬 설정 회귀 검증:

```bash
./scripts/tests/cloudwatch-logs-test.sh
./scripts/tests/deploy-backend-test.sh
```

배포 후 CloudWatch Console에서 region을 `us-east-1`로 선택하고 `Logs` → `Log groups`에서 두 Group의 최신 stream을 각각 확인합니다. EC2에서는 driver와 Spring 파일 로그 제거 상태를 다음처럼 확인합니다.

```bash
docker inspect lingko-backend --format '{{.HostConfig.LogConfig.Type}}'
worker_id=$(docker compose -f /home/ubuntu/LingKo/backend/docker-compose.yml ps -q evaluation-worker)
docker inspect "$worker_id" --format '{{.HostConfig.LogConfig.Type}}'
docker exec lingko-backend sh -c 'test ! -e /app/logs/lingko.log'
```

인증 Header·token·Presigned URL·사용자 문장·음성·이메일·소셜 ID와 사용자별 S3 object key는 기록하지 않습니다. 내부 작업 ID는 장애 추적에 필요한 범위에서만 사용하며, 예측하지 못한 500은 응답에 예외 메시지를 노출하지 않고 서버에서 `ERROR`로 기록합니다.

### 요청 단위 로그 추적

API는 모든 HTTP 요청에 `X-Request-Id` 응답 Header를 반환하고 같은 값을 MDC `requestId`와 오류 응답
본문에 사용합니다. 외부 Header는 영숫자로 시작하고 영숫자·점·밑줄·하이픈만 포함한 1~64자일 때만
수용하며, 나머지는 서버 UUID로 교체해 로그 주입을 막습니다. 요청 완료 INFO 로그에는 method, status,
outcome, duration만 기록하고 URL·query·본문은 기록하지 않습니다. Servlet thread 재사용으로 다른 요청의
ID가 섞이지 않도록 요청 종료 시 MDC 값을 제거합니다.

console 로그는 `requestId=<값>`을 포함합니다. HTTP 요청을 처리하지 않는 평가 Worker는
`requestId=none`이며, 비동기 작업 로그의 `jobId=<값>`으로 한 작업을 추적합니다. CloudWatch Logs
Insights에서는 사용자로부터 받은 ID를 그대로 query 문법에 붙이지 말고 허용 형식을 확인한 뒤 다음처럼
검색합니다.

```text
fields @timestamp, @message
| filter @message like /requestId=5b8df634-fbab-44d2-9a89-8e1698570dc2/
| sort @timestamp asc
```

Worker 작업은 `/lingko/prod/worker`에서 `jobId=<작업 ID>`로 같은 방식으로 검색합니다. `requestId`와
`jobId`는 로그 검색용이며 CloudWatch Metric dimension으로 사용하지 않습니다.

## CloudWatch Agent OS 지표

운영 EC2는 Instance Profile `LingKoEc2SsmRole`의 임시 자격 증명으로 CloudWatch에 지표를 전송합니다. 장기 AWS Access Key를 Agent 설정에 저장하지 않습니다. 저장소의 `backend/aws/cloudwatch-agent.json`은 60초 간격으로 `CWAgent` namespace에 OS 지표와 Spring StatsD allowlist 지표를 전송합니다.

- `mem_used_percent`
- 루트(`/`)의 `disk_used_percent`
- 루트(`/`)의 `disk_inodes_free`, `disk_inodes_total`

모든 지표에는 `InstanceId`를 붙이고 `drop_device`로 재부팅 시 달라질 수 있는 device dimension을 제거합니다. Docker overlay와 메모리 filesystem은 수집하지 않습니다. Spring 지표에는 `environment`, `service`와 bounded outcome/provider tag만 추가합니다. 세부 지표·Alarm·비용·디스크 대응은 [운영 모니터링·장애 대응](../operations/monitoring-and-incident-response.md)을 참고합니다.

운영 적용은 `develop` 병합 후 `Actions` → `CloudWatch Agent` → `Run workflow`에서 `develop`을 선택하고 확인 항목을 체크해 시작합니다. 이 workflow는 Backend CD와 같은 production Environment·OIDC·SSM 경로를 사용하며, 정확한 `develop` commit만 EC2에 fast-forward한 뒤 설치합니다. 병합만으로 자동 실행되지는 않습니다.

EC2에서 직접 복구해야 할 때만 저장소의 최신 `develop`을 받은 뒤 다음 명령을 사용합니다.

```bash
cd /home/ubuntu/LingKo
sudo ./scripts/install-cloudwatch-agent.sh
```

설치 스크립트는 Ubuntu `amd64`·`arm64`만 허용하며 AWS 공식 GPG key fingerprint와 package signature를 검증한 후 설치합니다. 설정 schema 검증, service 시작, 부팅 시 자동 시작까지 한 번에 수행합니다. workflow는 설치 후 service를 한 번 더 재시작하고 active·enabled 상태와 최근 journal을 SSM 결과로 확인합니다.

로컬 회귀 검증:

```bash
./scripts/tests/cloudwatch-agent-test.sh
```

적용 후에는 service와 Agent 로그를 확인합니다.

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a status
sudo systemctl is-enabled amazon-cloudwatch-agent.service
sudo journalctl -u amazon-cloudwatch-agent --since '10 minutes ago' --no-pager
```

CloudWatch Console의 `Metrics` → `CWAgent`에서 현재 `InstanceId`의 OS 지표와 `service=api|worker` 애플리케이션 지표가 5분 이상 연속 수집되는지 확인합니다. 중단해야 하면 먼저 Agent를 멈추고, 다시 적용할 때 동일 설치 스크립트를 실행합니다.

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a stop
```

## 테스트 경계

- 단위 테스트: 입력 검증, 변환 규칙, 응답 파싱과 서비스 상태 전이
- Spring/JPA 테스트: 작업 생성의 멱등성, 쿼터 경쟁, 결과 저장과 상태 전이
- Flutter 테스트: 화면 동작, API 계약, 세션과 폴링 처리
- 외부 통합 테스트: 실제 외부 서비스와의 연결. 별도 `externalIntegrationTest` 작업을 사용하며 자격증명과 비용을 확인한 뒤 실행합니다.

H2와 외부 서비스 대역을 사용하는 테스트는 MySQL의 실제 잠금·실행 계획이나 Azure·S3 응답시간을 검증한 것으로 해석하지 않습니다. JaCoCo 보고서가 존재하는 것과 커버리지 임계값을 강제하는 것은 별개입니다.

법무 문서를 변경할 때 `LegalDocumentSourceSyncTest`로 문서 원본과 서버 제공 리소스의 일치를 확인합니다. 기기 녹음·Presigned PUT·로그인 capability는 해당 플랫폼에서 별도 실행 확인이 필요합니다.

## 문제를 재현할 때

실패 명령, 재현 조건, 비밀값을 제거한 핵심 오류, 실행 환경을 먼저 구분합니다. 외부 서비스 장애와 입력 오류를 섞지 않고 동일 조건의 회귀 테스트로 확인합니다.

[문제 해결 사례](../engineering/case-studies.md) · [오류 코드](../api/error-codes.md)
