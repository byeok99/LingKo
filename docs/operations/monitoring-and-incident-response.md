# 운영 모니터링과 장애 대응

LingKo 운영 환경은 EC2 한 대의 Docker Compose에서 API, 평가 Worker, MySQL을 실행합니다. CloudWatch는 운영 기반선이며 Prometheus/Grafana는 현재 범위에 포함하지 않습니다.

## 수집 경로

```text
EC2 CPU·Network·Status ──────────────────────┐
CloudWatch Agent → Memory·Disk              ├→ CloudWatch → Alarm → SNS email
Spring Micrometer → StatsD UDP → Agent      │
Spring stdout → Docker awslogs              ┘
```

- namespace: `CWAgent`(Agent와 Spring custom metric), `AWS/EC2`(기본 지표)
- Log Group: `/lingko/prod/api`, `/lingko/prod/worker`, 보존 14일
- Dashboard: `LingKo-Prod`
- 공통 dimension: `InstanceId`, `environment=prod`, `service=api|worker`
- `userId`, `requestId`, `jobId`, 문장과 S3 object key는 Metric dimension으로 사용하지 않습니다.
- `/actuator/health`만 노출하며 정상은 `200 {"status":"UP"}`, DB 장애는 상세 정보 없이 `503 {"status":"DOWN"}`입니다.

StatsD는 기존 CloudWatch Agent를 재사용합니다. 애플리케이션이 CloudWatch API로 직접 push하지 않으므로 CloudWatch 장애나 UDP 유실이 요청 처리를 막지 않으며, 같은 지표를 두 경로로 중복 전송하지 않습니다. export allowlist는 JVM heap, GC pause, live thread, HTTP, HikariCP와 `lingko.*`만 허용합니다.

## 배포와 검증

Agent 설정을 먼저 배포한 뒤 API/Worker를 배포합니다. EC2에서 다음을 확인합니다.

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a status
sudo ss -lunp | grep ':8125'

cd /home/ubuntu/LingKo/backend
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

CloudWatch의 `CWAgent`에서 `environment=prod`, `service=api|worker` dimension과 다음 지표가 5분 이상 연속으로 들어오는지 확인합니다. CloudWatch Agent의 StatsD parser가 점을 밑줄로 정규화하므로 CloudWatch에서 조회하는 이름은 Micrometer 이름과 다릅니다.

- `jvm_memory_used`, `jvm_memory_max`, `statsd_jvm_gc_pause_count`, `jvm_threads_live`
- `statsd_http_server_requests_percentile_95`, `hikaricp_connections_active|idle|pending`
- `lingko_evaluation_pending`, `lingko_evaluation_oldest_pending_age_seconds`
- `lingko_evaluation_expired_leases`, `lingko_evaluation_worker_heartbeat_age_seconds`
- `lingko_evaluation_executions`, `statsd_lingko_evaluation_duration_percentile_95`

Dashboard 적용 script는 Memory·Disk, API·Worker JVM과 Worker heartbeat가 최근 3시간 내 실제로 수집됐는지 확인한 후에만 Dashboard와 Alarm을 생성합니다. Gauge·counter Alarm에는 Agent가 추가하는 `metric_type` dimension까지 포함합니다.

```bash
aws cloudwatch list-metrics --region us-east-1 --namespace CWAgent \
  --query 'Metrics[?contains(MetricName, `lingko`) || contains(MetricName, `http`)].MetricName' \
  --output text
```

Dashboard와 핵심 Alarm은 CloudShell에서 저장소를 받은 뒤 다음 환경값으로 적용합니다.

```bash
export AWS_REGION=us-east-1
export EC2_INSTANCE_ID=i-xxxxxxxxxxxxxxxxx
export ALERT_TOPIC_ARN=arn:aws:sns:us-east-1:<account-id>:lingko-prod-alerts
export EXPECTED_AWS_ACCOUNT_ID=<account-id>
export CONFIRM_PRODUCTION=true
bash scripts/apply-cloudwatch-observability.sh
```

애플리케이션 S3 권한은 `evaluation-audio/`, `guide-segments/`, `images/`, `videos/` prefix만 허용하는 `backend/aws/s3-instance-role-policy.json.template`로 관리합니다. 정적 원본 `guides/`는 애플리케이션의 쓰기 범위에 포함하지 않습니다.

장기 Access Key 제거는 다음 순서를 지킵니다.

1. CloudShell에서 `AWS_S3_BUCKET`, `EC2_INSTANCE_ID`, `EXPECTED_AWS_ACCOUNT_ID`, `CONFIRM_PRODUCTION=true`를 설정하고 `scripts/apply-ec2-s3-policy.sh`를 실행합니다. 이 script는 실제 Instance Profile role을 확인한 뒤 S3 policy를 적용하고, container용 hop limit 2와 IMDSv2-only를 강제합니다.
2. EC2에서 같은 commit으로 `CONFIRM_PRODUCTION=true scripts/verify-s3-instance-role.sh`를 실행합니다. 이 검증 container에는 `.env`나 AWS Access Key를 전달하지 않으므로 presigned PUT·download·delete 성공이 Instance Profile 사용을 증명합니다.
3. `backend/.env`에서 `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`와 표준 AWS credential 변수를 제거하고 Backend를 배포합니다. 배포 script는 이 값이 하나라도 남으면 image build 전에 실패합니다.
4. API·Worker health와 S3 기능을 다시 확인한 뒤에만 기존 IAM Access Key를 비활성화하고, 관찰 기간 후 삭제합니다.

SDK에는 `AWS_EC2_METADATA_V1_DISABLED=true`를 전달합니다. 따라서 Instance Profile 조회는 IMDSv2만 허용하고, 장기 credential이 SDK의 기본 provider chain에서 우선 선택되는 상황을 막습니다.

## Alarm 판단 기준

| Alarm | 기준 | 의도 |
|---|---:|---|
| Memory warning | 80%, 3/5분 | swap/OOM 이전 대응 |
| Agent metrics missing | 3분 연속 누락 | Agent·권한·전송 경로 장애 |
| Root disk warning / critical | 80% / 90% | 정리 여유 확보 / 즉시 대응 |
| EC2 status critical | 1, 2/2분 | host·instance 장애 |
| Worker heartbeat critical | 15분 | 최대 10분 lease 작업을 정상 정지로 오인하지 않음 |
| Oldest pending warning | 15분 | lease와 정상 처리 시간을 넘는 적체 탐지 |
| Evaluation terminal failure | 5분 합계 1 이상 | 사용자 평가의 영구 실패 즉시 확인 |

Disk 90% critical이 울리면 같은 원인의 80% warning은 별도 장애로 대응하지 않고 critical 한 건에 연결합니다. Alarm 이름에 `warning`·`critical`을 포함해 이메일 제목에서도 심각도를 구분합니다.

HTTP 5xx와 p95 Alarm은 초기 트래픽 기준선을 7일 이상 확보한 후 설정합니다. 표본이 적을 때 고정 p95 임계값은 오탐이 크므로, 우선 Dashboard로 관찰하고 `5xx >= 5/5분`, `p95 >= 5초 3/5분`을 시작 후보로 검토합니다.

## 비용 통제

CloudWatch 비용은 실제 리전 가격표가 기준입니다. 배포 전후 Billing의 CloudWatch usage를 비교합니다.

- 수집 주기는 60초이고 고해상도 지표는 사용하지 않습니다.
- 로그는 14일 후 만료하며 `INFO`가 기본입니다.
- 지표 allowlist와 bounded tag만 사용합니다.
- 월 예상액은 `유료 custom metric 수 × 단가 + alarm 수 × 단가 + 로그 수집 GB × 단가 + dashboard`로 계산합니다.
- 매월 첫 영업일에 `CWAgent` metric 수, Log Group `IncomingBytes`, Alarm 수를 기록합니다.

최근 24시간 로그 수집량은 CloudShell에서 `AWS/Logs`의 `IncomingBytes`를 합산해 확인합니다.

```bash
start_time=$(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%SZ)
end_time=$(date -u +%Y-%m-%dT%H:%M:%SZ)
for group in /lingko/prod/api /lingko/prod/worker; do
  aws cloudwatch get-metric-statistics \
    --region us-east-1 \
    --namespace AWS/Logs \
    --metric-name IncomingBytes \
    --dimensions Name=LogGroupName,Value="$group" \
    --start-time "$start_time" \
    --end-time "$end_time" \
    --period 86400 \
    --statistics Sum \
    --query 'Datapoints[0].Sum' \
    --output text
done
```

현재 가격과 Free Tier는 [CloudWatch 공식 가격](https://aws.amazon.com/cloudwatch/pricing/)에서 확인합니다. 무료 한도를 영구 운영 예산으로 가정하지 않습니다.

## 디스크 80% 대응 runbook

먼저 원인을 확인하고 DB volume은 직접 삭제하지 않습니다.

```bash
df -h /
df -i /
docker system df
sudo du -xhd1 /var/lib/docker /var/log 2>/dev/null | sort -h
```

안전한 순서로 다음만 정리합니다.

1. 7일 이상 된 Docker build cache: `docker builder prune --filter until=168h`
2. dangling image: `docker image prune`
3. 사용하지 않는 중지 container: 목록을 확인한 뒤 `docker container prune`
4. systemd journal: 사용량 확인 후 `sudo journalctl --vacuum-time=14d`

현재 실행 image와 최신 `rollback-*` image 하나는 유지합니다. `docker system prune -a`, MySQL volume 삭제, 경로가 확인되지 않은 재귀 삭제는 사용하지 않습니다. 정리 후 `docker compose ps`, MySQL health, `/actuator/health`와 디스크 사용률을 다시 확인합니다.

## 소유권과 장애 연습

Alarm 소유자는 `lingko-prod-alerts`의 확인 완료된 구독자이며 개인 이메일은 공개 문서에 기록하지 않습니다. 알림을 받으면 CloudWatch Dashboard → API/Worker Log Group → SSM 순서로 확인합니다.

분기마다 운영자 승인 아래 다음 연습을 한 번 수행합니다.

1. CloudWatch Alarm을 임시 `ALARM` 상태로 전환해 SNS 수신과 OK 복구를 확인합니다.
2. maintenance 시간에 Worker만 중지해 heartbeat missing-data Alarm과 재시작 후 복구를 확인합니다.
3. DB 중단은 데이터 백업과 복구 창이 확보된 경우에만 수행하고 health 503과 CD rollback 계약을 확인합니다.
4. 시작·감지·복구 시각과 문제를 비공개 운영 기록에 남깁니다.

설정 rollback은 직전 `cloudwatch-agent.json` 재적용, Dashboard 삭제가 아니라 이전 body 재적용, 애플리케이션은 보존된 `rollback-*` image 재배포 순서로 수행합니다.
