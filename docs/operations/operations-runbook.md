# 운영 Runbook

현재 저장소에는 완성된 운영 배포 파이프라인과 모니터링 구성이 없습니다. 이 문서는 현재 구조에서 필요한 운영 절차와 점검 기준을 정의합니다.

## 배포 전 체크

- `./gradlew test integrationTest`
- `flutter analyze && flutter test`
- 네이티브 설정 변경 시 Android/iOS 빌드
- Flyway 신규 마이그레이션 검토
- 환경변수와 비밀값 존재 확인
- 외부 서비스 쿼터와 권한 확인
- S3 버킷 비공개 차단, Presigned PUT·삭제 권한과 Lifecycle 확인
- SQS Standard Queue URL, DLQ와 redrive 정책 확인
- `.env`, 토큰, 키가 Git diff에 포함되지 않았는지 확인
- DB 백업 또는 복구 지점 확인

## 백엔드 Docker 배포

```bash
cd backend
docker build -t lingko-backend:<version> .
```

이미지는 Java 21 JRE와 FFmpeg를 포함합니다. 운영에서는 MySQL을 같은 Compose 안에 두기보다 관리형 DB 또는 별도 백업 정책이 있는 DB를 권장합니다.

### SQS 기반 독립 평가 Worker

API와 Worker에 같은 `EVALUATION_QUEUE_URL`·AWS region·credential을 주입합니다. API는 dispatcher만 실행하고 Worker는 HTTP server와 dispatcher를 끕니다.

```bash
cd backend
EVALUATION_WORKER_MODE=sqs \
EVALUATION_API_WORKER_ENABLED=false \
docker compose --profile queue up --build --scale evaluation-worker=4
```

- API: `EVALUATION_WORKER_MODE=sqs`, `EVALUATION_API_WORKER_ENABLED=false`, `EVALUATION_QUEUE_DISPATCHER_ENABLED=true`
- Worker: `SPRING_MAIN_WEB_APPLICATION_TYPE=none`, `EVALUATION_WORKER_MODE=sqs`, `EVALUATION_WORKER_ENABLED=true`, dispatcher·cleanup 비활성
- 작은 개발 환경 또는 Queue 장애 fallback: `EVALUATION_WORKER_MODE=database`, API 내부 Worker 활성
- Queue에는 `jobId`만 저장하고 작업 상태·결과·Idempotency는 MySQL에서 확인
- Queue visibility timeout은 정상 평가 처리시간보다 길게 설정하고 DLQ redrive 횟수는 애플리케이션 최대 재시도와 혼동하지 않도록 별도로 기록

## 헬스 점검

현재 명시적인 애플리케이션 헬스 엔드포인트가 없으므로 운영 전 Spring Boot Actuator 도입을 권장합니다.

최소 확인 항목:

- 프로세스와 포트 8080
- DB 연결과 Flyway 적용 상태
- 추천 문장 API 응답
- 인증 실패·성공 경로
- 외부 평가 서비스 연결
- 디스크·메모리·CPU
- 외부 API 오류율과 지연시간

## 장애 대응 순서

```text
1. 사용자 영향과 시작 시각 확인
2. 최근 배포·설정·마이그레이션 변경 확인
3. 요청 ID 기준 로그 확인
4. DB·Azure·Replicate·S3·FFmpeg를 분리 점검
5. 기능 차단 또는 이전 버전 롤백 결정
6. 복구 확인
7. 원인·재발 방지 기록
```

## 대표 장애

### 백엔드 시작 실패

- 환경변수 누락 확인
- MySQL 접근 가능 여부 확인
- Flyway checksum 또는 SQL 오류 확인
- `docker logs lingko-backend` 또는 애플리케이션 로그 확인

### DB 연결 실패

- 호스트·포트·계정·비밀번호 확인
- DB 최대 연결 수와 저장 공간 확인
- 네트워크·보안 그룹 확인
- 장애 중 쓰기 요청을 반복 재시도하지 않도록 주의

### 평가 요청 급증 또는 Azure 장애

- `evaluation_jobs`의 `PENDING`·`PROCESSING` 수와 가장 오래된 작업 확인
- Azure 상태·쿼터·키 유효성 확인
- Worker를 중지하려면 `EVALUATION_WORKER_ENABLED=false`로 배포하되 PENDING 작업은 DB에 보존
- 무제한 즉시 재시도 금지

### 평가 작업 정체

- Worker 활성화 여부와 `EVALUATION_WORKER_*` 설정 확인
- SQS mode에서는 Queue URL, depth, oldest message age, DLQ와 Worker replica 수 확인
- `PENDING`의 `enqueued_at`이 재발행 기준보다 오래됐는데 Queue depth가 늘지 않으면 API dispatcher와 AWS 권한 확인
- `lease_expires_at`이 지난 `PROCESSING` 작업이 다시 claim되는지 확인
- Azure 호출이 종료되지 않는 경우 Worker 프로세스를 재시작하고 #44 timeout 적용 상태 확인
- 실패 작업의 `attempt_count`, `error_code`와 쿼터 복구 여부 확인
- DB 상태를 수동 수정하기 전에 S3 object와 예약 쿼터를 함께 확인

### Queue 장애 또는 중복 전달

- SQS Standard Queue의 중복 메시지는 정상 가능성이 있으므로 수동으로 작업을 복제하지 않음
- 완료 작업 메시지는 Worker가 DB 상태 확인 후 ACK하며 결과·쿼터를 다시 확정하지 않음
- Queue 발행 실패 후 `PENDING` 작업은 `EVALUATION_QUEUE_REDISPATCH_SECONDS`가 지나면 재발행
- Worker 종료 시 visibility timeout과 DB lease가 모두 만료된 뒤 다른 Worker가 재처리
- DLQ 이동 작업은 DB 상태, `attempt_count`, S3 object와 예약 쿼터를 함께 확인한 후 redrive
- Queue 장애 중에는 API가 작업을 DB에 보존하므로 신규 작업 중복 생성 대신 dispatcher 복구를 우선

### 평가 작업 Idempotency 보존·정리

- 기본값은 완료 후 7일 보존, 1시간 간격, 실행당 최대 1,000건 삭제
- `EVALUATION_CLEANUP_ENABLED`, `EVALUATION_CLEANUP_RETENTION_DAYS`, `EVALUATION_CLEANUP_INTERVAL_MS`, `EVALUATION_CLEANUP_BATCH_SIZE`로 조정
- 정리 대상은 `SUCCEEDED`·`FAILED`이면서 `completed_at`이 보존 기준보다 오래된 작업으로 제한
- `PENDING`·`PROCESSING` 증가는 정리 설정이 아니라 Worker 정체 원인을 먼저 확인
- 삭제량이 batch 상한에 계속 도달하면 실행 간격 또는 batch 크기를 점진적으로 조정하고 DB 부하를 확인

### 평가 음성 S3 설정

- 버킷 Public Access Block을 활성화하고 object ACL을 공개하지 않음
- Backend 자격 증명에는 해당 bucket prefix의 PUT, HEAD, GET, DELETE만 허용
- `evaluation-audio/` prefix에 보존 정책에서 정한 짧은 Lifecycle 만료 규칙 적용
- 앱 로그, Backend 로그와 analytics에 Presigned URL 전체를 기록하지 않음
- 성공·최종 실패 후 object 삭제와 Lifecycle 만료를 운영 환경에서 표본 검증

### 가이드 작업이 사라짐

현재 작업 상태는 메모리 기반이므로 서버 재시작 시 정상적으로 소실됩니다. 사용자는 기존 작업 ID로 404를 받을 수 있습니다. 현재 대응은 재생성뿐이며, 운영 전 DB 또는 작업 큐로 이전해야 합니다.

### S3 또는 FFmpeg 실패

- AWS 권한·버킷·리전 확인
- FFmpeg 실행 경로와 exit code 확인
- 임시 디스크 용량 확인
- 외부 URL 접근과 형식 확인

## 롤백

### 애플리케이션

- 직전 검증 이미지 또는 JAR로 교체
- 스키마가 하위 호환인지 확인
- 롤백 후 주요 API와 로그 점검

### 데이터베이스

- 기존 마이그레이션 파일을 수정하지 않음
- 호환 가능한 경우 앱만 롤백
- 불가능하면 보정 마이그레이션 또는 백업 복구
- 데이터 손실 가능성이 있으면 쓰기 중단 후 복구

## 백업

운영 전 다음 정책을 확정해야 합니다.

- MySQL 자동 백업 주기와 보존 기간
- 복구 훈련 주기
- S3 versioning·lifecycle·삭제 정책
- 사용자 탈퇴 시 DB와 S3 동시 삭제
- 로그 보존 기간

## 관측성 권장 지표

- 요청 수, 4xx/5xx 비율, p50/p95/p99 지연시간
- 로그인 성공·실패율
- 평가 성공·실패·타임아웃 비율
- 외부 서비스별 지연시간
- DB connection pool 사용량
- JVM heap·GC·CPU
- 가이드 작업 상태별 수와 처리시간
- 평가 작업 상태별 수, oldest pending age, 시도 횟수와 lease 만료 재claim 수
- SQS queue depth, oldest message age, receive/delete 오류, DLQ message 수
- dispatcher 발행·실패·재발행 수와 Worker 완료·재시도 처리량
- 음성 업로드 크기 분포

## 사고 기록 템플릿

```markdown
# 사고 제목
- 시작/종료 시각
- 사용자 영향
- 탐지 경로
- 직접 원인
- 근본 원인
- 복구 조치
- 재발 방지 작업
- 담당자와 완료 기한
```
