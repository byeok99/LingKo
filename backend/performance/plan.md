# LingKo 부하 테스트 계획

API를 **많이 불리는 API / 무거운 API / 실패하면 안 되는 API** 세 묶음으로 나누고, 묶음마다 아래 6단계 중 필요한 단계를 순서대로 실행한다. 실행 도구와 안전장치는 [README](README.md)를 따른다.

앞 단계를 통과하지 못하면 다음 단계로 넘어가지 않는다. 기능 오류가 있는 endpoint에 부하를 더 넣지 않는다.

1~6절은 HTTP API의 응답 시간과 처리량을 다룬다. 실제로 가장 비싸고 사용자를 오래 기다리게 할 수 있는 부분은 **평가 Worker(음성 평가와 가이드 영상 생성)**다. 이 부분은 측정 지표와 방법이 달라서 7절에서 따로 다룬다.

## 1. API 분류

분류 근거는 앱 호출 코드(`app/lib/api`, `app/lib/app/lingko_app.dart`)와 서버 구현이다. 한 API가 여러 묶음에 속할 수 있다.

| API | 빈도 | 무거움 | 실패 불가 | 근거 |
|---|:---:|:---:|:---:|---|
| `GET /api/evaluations/jobs/{jobId}` | ● | | ● | 앱이 평가가 끝날 때까지 **1초 간격으로 polling**한다. 결과 유실은 차감된 기회의 유실이다 |
| `POST /api/pronunciation/prepare` | ● | | | 직접 입력 문장에서 **입력이 멈춘 뒤 700ms**마다 호출된다. 음운 규칙 계산이 CPU를 쓴다 |
| `GET /api/quota/today` | ● | | ● | 앱 시작, 평가 전후, 광고 보상 후 다시 조회한다. 값이 틀리면 평가를 막거나 과다 허용한다 |
| `GET /api/sentences/recommended` | ● | | | 연습 화면 진입마다 호출된다 |
| `GET /api/sentences/{sentenceId}` | ● | | | 추천 문장을 선택할 때마다 호출된다 |
| `GET /api/evaluations/me` | | ● | | 기록 수에 비례하는 `count(*)`, `max(score)`, page size에 비례하는 N+1(`size=20`이면 query 43개) |
| `GET /api/evaluations/me/weak-sounds` | | ● | | 사용자 전체 음절 집계 |
| `GET /api/evaluations/me/sounds/{character}` | | ● | | `LIKE '%글자%'` 조회 |
| `POST /api/evaluations/jobs` | | ● | ● | Idempotency 확인, 기회 차감, 작업 생성을 한 transaction에서 처리하고 S3 객체를 `headObject`로 확인한다. 중복 차감과 중복 작업이 생기면 안 된다 |
| `POST /api/evaluations/uploads` | | | ● | 업로드 티켓(Presigned URL)을 발급한다. 실패하면 평가를 시작할 수 없다 |
| `POST /api/auth/token/refresh` | | | ● | Refresh Token을 회전한다. 동시 요청 중 하나만 유효해야 하고, 실패하면 강제 로그아웃된다 |
| `POST /api/auth/oauth/login` | | | ● | 외부 공급자 검증. 실패하면 서비스 진입 자체가 불가능하다 |
| `POST /api/quota/ad-reward-sessions`, `GET .../{sessionToken}` | | | ● | 광고 보상 상태를 1초 간격으로 polling한다(최대 10초) |
| `GET /api/quota/ad-rewards/ssv` | | | ● | Google 서명 검증 후 보상을 지급한다. 중복 지급과 보유 한도(5회) 초과가 없어야 한다 |
| `POST /api/legal/consent`, `POST /api/legal/ai-consent` | | | ● | 동의가 없으면 평가를 진행할 수 없다 |
| `GET /api/sentences/saved`, `PATCH /api/sentences/saved/{id}` | | | | 보통 수준. 평소 부하 혼합에만 포함한다 |
| `GET /api/legal/consent`, `/policy`, `/api/legal/ai-consent` | | | | 앱 시작 시 1회. 평소 부하 혼합에만 포함한다 |
| 평가 Worker (Azure Speech + 가이드 영상) | | ● | ● | Worker 하나가 한 번에 작업 하나만 처리하고, 캐시 미스면 영상 생성까지 같은 작업 안에서 기다린다. **7절 Worker 용량 테스트로 측정한다** |
| `POST /api/pronunciation/convert`, `/api/pronunciation/guide-jobs` | | | | 앱이 호출하지 않는다. 측정 대상에서 제외한다 |
| `DELETE /api/auth/account`, `POST /api/auth/logout` | | | | 파괴적이거나 호출 빈도가 낮다. 부하 대상이 아니라 기능 테스트 대상이다 |

### 측정 전에 풀어야 할 제약

- **`POST /jobs`는 실제 S3 객체가 있어야 성공한다.** 로컬에서는 S3 대체 수단(MinIO나 LocalStack)이나 test profile용 storage stub이 필요하다. 이것이 준비되기 전까지 이 API는 smoke 단계에서 멈춘다.
- **평가 기회는 사용자당 최대 5회**이고 1시간에 1회 충전된다. 차감이 일어나는 부하를 주려면 perf 사용자 pool이 필요하다. 예를 들어 1 RPS로 10분이면 600회 차감이 생기므로 최소 120명이 필요하다. 사용자 pool은 `scripts/create-perf-tokens.sh`로 만든다.
- **OAuth 로그인과 SSV는 외부 서명이 필요하다.** 로컬에서는 test key를 쓰는 검증기 stub이 있어야 측정할 수 있다. 준비되기 전까지는 제외한다.
- **API 측정(1~6절)에서는 평가 Worker를 띄우지 않는다.** 이 상태에서 작업은 `QUEUED`에 머문다. 그래서 polling 측정은 "미완료 작업 조회" 비용을 재는 것이다. Worker는 7절에서 외부 호출을 stub으로 바꾼 상태로만 띄운다.

## 2. 평소 부하의 요청 혼합

**아래 비율은 앱 흐름으로 추정한 가정값이다.** 운영에서 Prometheus 지표 `sum by (method, uri) (rate(http_server_requests_seconds_count[1d]))`로 실제 비율을 구해 교체한다. 요청 로그에는 URL을 남기지 않으므로 로그로는 이 비율을 구할 수 없다.

추천 문장 1개로 평가를 1회 하는 흐름을 기준으로 한다. 평가 시간은 약 10초로 가정한다.

| API | 1회 흐름당 호출 | 비율 |
|---|---:|---:|
| `GET /jobs/{jobId}` | 10 | 50% |
| `GET /quota/today` | 2 | 10% |
| `POST /prepare` | 2 | 10% |
| `GET /sentences/recommended` | 1 | 5% |
| `GET /sentences/{id}` | 1 | 5% |
| `POST /uploads` | 1 | 5% |
| `POST /jobs` | 1 | 5% |
| `GET /evaluations/me` | 1 | 5% |
| `GET /me/weak-sounds`, `/me/sounds/{c}` | 0.5 | 2.5% |
| `GET /sentences/saved`, `POST /token/refresh` 등 | 0.5 | 2.5% |

**목표 처리량은 20 RPS(NFR-PERF-006)다.** 2026-11-01 기준선에서 전체 API 혼합의 처리량은 closed model 기준 약 1,175 RPS에서 포화했다. 그래서 20 RPS 통과는 확인 절차에 가깝고, 스트레스 이후 단계는 측정한 **안전 처리량**을 기준으로 잡는다.

## 3. 테스트 순서

부하 크기는 VU가 아니라 **RPS**로 정한다. 2단계부터는 `arrival-rate` executor를 쓰고, VU는 목표 RPS를 만들기 위한 여유분으로만 둔다. 필요한 VU는 `목표 RPS × 응답 시간(초)`로 계산한다. 예를 들어 20 RPS × 0.03초이면 VU 1개면 된다.

RPS 값은 2026-11-01 기준선(`all-apis` closed model 1회 측정, 약 1,175 RPS에서 포화)을 기준으로 잡았다. open model로 안전 처리량을 다시 측정하면 3~6단계 값을 그 값에 맞게 바꾼다.

| 순서 | 종류 | RPS | VU (preAllocated / max) | 시간 | 확인할 것 | 적용 묶음 |
|---:|---|---|---|---|---|---|
| 1 | 스모크 | iteration 1회 또는 VU 1~2 | 1~2 | 1분 | 스크립트가 맞는지, check 100%, 오류 0 | 전체 |
| 2 | 평소 부하 | 20 (3분 동안 0→20으로 올린 뒤 유지) | 10 / 50 | 10~30분 | 합격 기준 통과, `dropped_iterations` 0 | 빈도, 무거움, 실패 불가 |
| 3 | 스트레스 | 40, 이어서 안전 처리량 × 1.5 | 50 / 200 | 단계당 5분 | 무엇이 먼저 포화되는지(CPU, Hikari, MySQL, k6) | 빈도, 무거움 |
| 4 | 한계 찾기 | 100 → 1,500, 2분마다 +100 | 100 / 400 | 오류나 지연 급증이 나올 때까지 | p95가 급증하거나 오류가 나기 시작하는 지점. 마지막 정상 단계가 안전 처리량이다 | 빈도, 무거움 |
| 5 | 스파이크 | 20 → 300을 10초 안에, 1분 유지 후 복귀 | 50 / 300 | 약 5분 | 오류 없이 버티는지, 복귀 후 지연이 회복되는지 | 빈도, 실패 불가 |
| 6 | 장시간 | 안전 처리량의 60~70% (기준선 기준 약 700) | 50 / 200 | 2~4시간 | heap 증가, Hikari active 누적, thread·file descriptor 증가 | 빈도, 실패 불가 |

- **스모크는 부하를 주는 단계가 아니다.** VU를 늘리면 오류가 났을 때 스크립트 문제인지 부하 문제인지 구분할 수 없다.
- **`dropped_iterations`가 나오면 maxVU를 먼저 늘린다.** 그래도 계속 나오면 서버가 아니라 k6가 병목이다.
- **장시간 단계를 20 RPS로 돌리면 부하가 너무 작다.** 그 정도로는 메모리 누수나 커넥션 누수가 몇 시간 안에 드러나지 않을 수 있다.
- **이 Mac(Docker 4 CPU)에서는 약 1,000 RPS부터 k6와 Docker가 CPU를 두고 경쟁한다.** 4단계에서 이 구간을 넘기면 실행 중 CPU 사용률을 반드시 함께 기록한다.
- **20 RPS는 동시 접속자 약 40~100명에 해당한다.** 평가를 기다리는 동안은 1초에 1번, 다른 화면에서는 10~20초에 1번 요청하므로 평균을 사용자당 0.2~0.5 요청/초로 잡았다. 실제 동시 접속자 수가 이보다 많으면 2단계 목표 RPS부터 올린다.

### 묶음별 실행 방식

- **많이 불리는 API**: 2절의 혼합 비율로 한 scenario에서 실행한다. endpoint마다 tag를 달아 따로 판정한다.
- **무거운 API**: 혼합에 섞지 않고 단독 scenario로 실행한다. 3~4단계는 seed 코호트(`perf-seed-1k`, `100k`, `long`)별로 반복해 데이터 크기에 따른 변화를 본다.
- **실패하면 안 되는 API**: 모든 단계에서 정합성 invariant를 함께 검사한다. 2단계와 5단계에서는 같은 키로 동시에 요청을 보내는 동시성 scenario를 추가로 실행한다.
  - 같은 Idempotency 키로 `POST /jobs`를 N개 동시에 보내면 작업 1건, 차감 1회여야 한다.
  - 같은 Refresh Token으로 `POST /token/refresh`를 N개 동시에 보내면 유효한 새 토큰 쌍은 1개여야 한다.
  - 같은 보상 세션으로 SSV를 N번 보내면 지급 1회여야 하고 보유량은 5회를 넘으면 안 된다.

## 4. 합격 기준

| 대상 | 기준 |
|---|---|
| 일반 읽기 (빈도 묶음) | p95 300ms 이하 |
| 평가 기록 첫 페이지 (`size=20`) | p95 500ms 이하 |
| 쓰기와 평가 접수 | p95 500ms 이하 |
| 오류율 | 1% 미만. 예상된 4xx는 별도 scenario에서 판정한다 |
| check | 100% |
| `dropped_iterations` | 0 |
| 중복 작업, 중복 차감, 중복 지급, 데이터 누락 | **0건. 성능보다 우선한다** |
| DB pool timeout, container 재시작, OOM | 0건 |

### 즉시 중단 조건

- 정합성 invariant가 한 번이라도 깨진다.
- 5xx 비율 1% 초과가 1분 넘게 계속된다.
- p95가 기준의 2배를 넘는 상태가 2분 넘게 계속된다.
- Hikari pending이나 timeout이 계속된다. MySQL deadlock이나 lock wait timeout이 발생한다.
- 부하 발생기(k6)의 CPU가 포화되거나 `dropped_iterations`가 생긴다.

중단한 결과도 버리지 않는다. 마지막 정상 단계와 첫 실패 단계가 안전 처리량을 판단하는 근거다.

## 5. 실행마다 기록할 것

- commit hash. 측정은 커밋된 상태에서만 한다(디렉터리 이름에 `dirty`가 붙지 않게).
- Docker 할당 자원, MySQL buffer pool 크기, seed 코호트
- k6 summary(`--summary-export`)와 endpoint별 수치
- 실행 중 `docker stats` 기록, Grafana의 JVM, Hikari, HTTP 패널
- 같은 조건 3회 실행의 중앙값과 편차

## 6. 현재 준비 상태

| 항목 | 상태 |
|---|---|
| host guard, `public_read`, `all-apis`, `history-depth` scenario | 있음 |
| endpoint별 threshold (summary에 endpoint별 수치가 나오게) | `config.js`의 `endpointThresholds`로 추가됨 |
| open model(`arrival-rate`) scenario | 많이 불리는 API의 1~2단계만 있음(`frequent-read.js`). `GET /jobs/{id}` polling은 제외 |
| 혼합 비율 scenario, 스파이크 scenario, 장시간 scenario | 없음 |
| Idempotency, Refresh Token, SSV 동시성 scenario | 없음 |
| 로컬 S3 대체 수단, OAuth·SSV 검증기 stub | 없음 (`POST /jobs`, 로그인, SSV 측정의 전제 조건) |
| 실제 운영 요청 비율 | 미측정 (2절 비율은 가정값) |
| Worker 외부 연동 stub (`SpeechEvaluator`, `EvaluationAudioStorage`, `VideoGenerator`) | 없음 (7절의 전제 조건) |
| Worker 용량 scenario (`POST /uploads` → `POST /jobs` → 1초 polling) | 없음 |

## 7. 평가 Worker 용량 테스트

### 왜 따로 측정하는가

`POST /jobs`는 작업을 DB에 넣고 바로 응답하므로 가볍다. 무거운 처리는 Worker가 따로 한다. 그래서 RPS와 응답 p95로는 사용자가 결과를 받기까지 얼마나 기다리는지 알 수 없다.

| 항목 | 현재 설정 | 의미 |
|---|---|---|
| 처리 방식 | `@Scheduled(fixedDelay=1s)`, Worker 하나가 **한 번에 작업 하나** | 처리량 ≈ Worker 수 × 60 ÷ 평균 처리 시간(초) (분당) |
| 가이드 영상 생성 | 평가 작업 안에서 **이어서 실행**. 캐시 미스면 Replicate를 최대 300초까지 polling | 영상 하나가 오래 걸리면 뒤에 있는 모든 작업이 기다린다 |
| Azure 평가 timeout | 30초 | 응답이 없어도 최대 30초 동안 Worker를 붙잡는다 |
| lease | 600초, 최대 3회 시도 | Worker를 여러 개 띄워 나눠 처리할 수 있는 구조다 |

**접수율이 처리량보다 크면 대기열이 끝없이 늘어난다.** 예를 들어 평균 처리 시간이 5초면 Worker 1개의 처리량은 분당 12건이다. 2절 가정 비율(`POST /jobs` 5%)을 20 RPS에 적용하면 접수는 분당 60건이 되는데, 이는 Worker 1개로는 감당할 수 없다. 이 숫자들은 아직 가정값이므로, 실제 처리 시간과 접수율을 재는 것이 이 절의 첫 목표다.

평가 기록 기준 가이드 영상 재사용률은 90.6%다(2026-09-04~11-10). 나머지 미스가 처리 시간의 꼬리를 결정하므로, 캐시 미스 비율을 실험 조건에 넣는다.

### 측정 지표

| 지표 | 출처 | 판단 |
|---|---|---|
| 대기열 길이 | `lingko.evaluation.pending` | 일정하게 유지되면 안정, 계속 늘면 처리량 초과 |
| 가장 오래 기다린 작업의 대기 시간 | `lingko.evaluation.oldest.pending.age.seconds` | 계속 커지면 대기열이 발산하는 것이다 |
| 작업 처리 시간 | `lingko.evaluation.duration` (outcome별) | 처리량 계산의 기준 |
| 접수 → 완료 시간 | DB `evaluation_jobs.completed_at - created_at` | 사용자가 실제로 기다린 시간. 처리 시작 시각 컬럼은 없으므로 대기 시간은 이 값에서 처리 시간을 빼서 추정한다 |
| 결과·재시도 | `lingko.evaluation.executions` (outcome별), `attempt_count` | 재시도와 최종 실패 비율 |
| lease 만료 | `lingko.evaluation.expired.leases` | 0이 아니면 처리 시간이 lease보다 길었거나 Worker가 멈춘 것이다 |
| 외부 호출 시간 | `lingko.external.requests` (provider별) | staging에서 실제 Azure·Replicate 지연 분포를 얻는 데 쓴다 |
| polling 부하 | k6 `GET /jobs/{id}` 요청률 | 대기 시간이 늘면 polling도 함께 늘어나는지 확인한다 |

### 정합성 invariant (모든 단계에서 0건이어야 함)

- 같은 Idempotency 키로 작업이 2건 이상 생긴다.
- 기회 차감 횟수와 생성된 작업 수가 다르다.
- 같은 작업이 두 Worker에서 동시에 완료 처리된다.
- 종료된 작업의 원본 음성 삭제가 호출되지 않는다.
- 완료된 작업에 결과(`result_payload`)가 없다.

### 1차: 로컬, 외부 호출을 stub으로 바꿔 측정

외부 비용 없이 **우리 코드(Worker 스케줄링, DB lease, transaction, 대기열)**의 한계를 찾는다.

| 바꿀 것 | stub 동작 |
|---|---|
| `SpeechEvaluator` (Azure) | 설정한 지연 시간 뒤 고정 평가 결과를 반환한다. 일정 비율로 timeout을 흉내 낼 수 있게 한다 |
| `EvaluationAudioStorage` (S3) | 업로드 티켓 발급, 업로드 확인, 다운로드, 삭제를 로컬에서 처리한다. 삭제 호출 횟수를 센다 |
| `VideoGenerator` (Replicate + FFmpeg) | 캐시 미스 비율만큼 설정한 지연 시간 동안 기다린 뒤 URL을 반환한다 |

- stub은 test profile에서만 켜지게 한다. 운영 설정에 섞이지 않도록 한다.
- 이 방식이면 MinIO 없이도 측정할 수 있다. 대신 FFmpeg의 CPU·메모리 비용은 빠진다. FFmpeg는 2차에서 MinIO와 Replicate stub을 붙여 따로 측정한다.
- k6 scenario는 앱 흐름을 따른다: `POST /uploads` → `POST /jobs`(Idempotency 키) → 완료될 때까지 1초 간격 `GET /jobs/{id}`
- 사용자당 기회가 5회이므로 perf 사용자 pool을 접수 건수에 맞게 준비한다.

**실험 조건**

| 변수 | 값 |
|---|---|
| 접수율 (작업/분) | 2, 5, 10, 20 |
| stub 평가 지연 | staging 실측값. 실측 전에는 3초 |
| stub 영상 생성 지연 (미스일 때) | staging 실측값. 실측 전에는 30초 |
| 캐시 미스 비율 | 0%, 10%, 30% |
| Worker 수 | 1, 2 |

**6단계 대응**

| 순서 | 종류 | 설정 | 확인할 것 |
|---:|---|---|---|
| 1 | 스모크 | 작업 1건을 접수부터 완료까지 | 상태가 끝까지 바뀌는지, 결과와 원본 삭제가 정상인지 |
| 2 | 평소 부하 | 목표 접수율로 15분 | 대기열이 일정하게 유지되는지, 접수 → 완료 p95 |
| 3 | 스트레스 | 목표 접수율 × 1.5~2 | 대기열이 늘기 시작하는 지점, 재시도·lease 만료 발생 여부 |
| 4 | 한계 찾기 | 접수율을 5분마다 올림 | `oldest.pending.age`가 계속 커지기 시작하는 접수율 = Worker 처리량 한계 |
| 5 | 스파이크 | 작업 30건을 10초 안에 접수 | 몰린 작업이 모두 끝나는 데 걸리는 시간, 정합성 invariant |
| 6 | 장시간 | 한계의 60~70% 접수율로 2시간 | 임시 파일, DB 연결, heap, thread 누적 |

**판정 기준 (초기 가정값, 제품 목표가 정해지면 교체)**

| 대상 | 기준 |
|---|---|
| 접수 → 완료 p95 (캐시 히트) | 30초 이하 |
| 대기열 | 평소 부하 15분 동안 늘지 않음 |
| lease 만료, 최종 실패 | 0건 |
| 정합성 invariant | 0건 |

### 2차: staging, 실제 외부 서비스로 소량 측정

- 작업 20~50건을 순차로 처리해 Azure와 Replicate의 실제 지연 분포를 얻는다. 이 값을 1차 stub의 지연 시간으로 다시 넣는다.
- 캐시 미스 영상 생성은 Replicate 비용이 들기 때문에 건수를 미리 정하고 실행한다.
- 외부 서비스의 rate limit에 걸리는지 확인한다. 걸린다면 Worker 수를 늘려도 처리량이 늘지 않는다.
- MinIO와 Replicate stub을 붙여 FFmpeg 병합의 CPU·메모리 비용을 따로 측정한다.

### 결과로 답할 질문

1. Worker 1개가 대기 시간 기준을 지키며 처리할 수 있는 최대 접수율은 얼마인가?
2. 캐시 미스 비율이 10%포인트 늘 때 접수 → 완료 p95는 얼마나 늘어나는가?
3. Worker를 2개로 늘리면 처리량이 2배가 되는가? 아니라면 DB lease와 외부 rate limit 중 무엇이 막는가?
4. 대기 시간이 길어질 때 polling 요청이 API 부하를 얼마나 늘리는가?
