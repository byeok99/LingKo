# LingKo 성능 측정 harness

k6 기반 부하 측정 script다. API 분류, 테스트 순서, 합격 기준은 [plan.md](plan.md)에 있고, 이 README는 그 계획을 실행하는 도구와 안전장치를 설명한다.

현재 scenario는 `public_read`, `all-apis`, `history-depth`(closed model)와 `frequent-read`(open model, plan.md 1~2단계)다. 나머지 scenario는 plan.md 6절 순서대로 추가한다.

## 안전장치

측정 사고는 대부분 "대상을 잘못 지정했다"에서 시작한다. 그래서 `guards.js`가 init context에서 대상을 검증하고, 통과하지 못하면 VU가 하나도 실행되기 전에 중단한다.

| 규칙 | 동작 |
|---|---|
| 기본 대상 | `http://127.0.0.1:8080` |
| 로컬 허용 host | `127.0.0.1`, `localhost`, `[::1]` |
| 그 밖의 host | 차단 |
| 운영 추정 host (`amazonaws.com` 등) | staging 확인을 줘도 차단 |
| staging | `PERF_TARGET=staging` **과** `PERF_CONFIRM_STAGING=true` 둘 다 필요 |
| URL 내 인증 정보 | 형식 자체를 차단 |
| 외부 호출 차단 선언 | `PERF_WORKER_DISABLED=true` 없으면 실행 거부 |

staging에 환경변수 2개를 요구하는 이유는 하나만으로는 오타·복붙으로 통과할 수 있기 때문이다. 둘 다 의도적으로 넣어야 하므로 실수로 staging을 때리는 경로가 사라진다.

`PERF_WORKER_DISABLED`는 script가 서버 설정을 직접 읽을 수 없어서 둔 **실행자 선언**이다. 평가 Worker가 돌면 Azure·Replicate 비용이 발생하므로, 확인했다는 사실을 명시적으로 남기게 한다.

### guard 자동 검증

```bash
./backend/performance/scripts/tests/guards-test.sh
```

허용·차단 9건을 검증한다. 서버가 떠 있지 않아도 실행된다. k6 버전이 올라가 flag가 바뀌면 guard가 평가되지 않은 채 "통과"로 보일 수 있으므로, flag 오류를 감지하면 test를 즉시 중단한다.

## 실행 준비

평가 Worker를 띄우지 않는 것이 비용 안전장치의 핵심이다. `backend/.env`에 worker flag가 없으면 compose 기본값(`false`)이 적용되어 API 컨테이너는 Worker를 실행하지 않는다.

```bash
cd backend

# mysql + backend만 띄운다. evaluation-worker는 의도적으로 제외한다.
docker compose up -d mysql backend

# health가 UP이 될 때까지 기다린다. DB 연결까지 포함한 계약이다.
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

### `.env.performance.local`에 대한 메모

TODO 문서 Phase 0은 전용 `.env.performance.local`을 요구하지만 아직 만들지 않았다. 이유는 두 가지다.

1. `docker-compose.yml`의 `env_file`이 `.env`로 고정되어 있어, 별도 파일을 쓰려면 override compose 파일이 함께 필요하다.
2. 이미 초기화된 MySQL volume이 기존 `.env`의 자격 증명으로 만들어져 있어, 새 파일에 다른 DB 비밀번호를 넣으면 접속이 실패한다.

따라서 지금은 기존 `.env`를 쓰고 **Worker를 띄우지 않는 것**으로 비용 안전을 확보한다. 외부 credential까지 주입 자체를 막으려면 `env_file: [.env, .env.performance.local]` 형태의 override 파일을 추가하는 것이 다음 단계다.

## 실행

```bash
cd backend/performance

# 1회 smoke — 계약만 확인하고 부하를 만들지 않는다
PERF_WORKER_DISABLED=true PERF_MODE=smoke \
  k6 run scenarios/public-read.js

# 단일 사용자 기준선 3분
PERF_WORKER_DISABLED=true PERF_VUS=1 PERF_DURATION=3m \
  k6 run scenarios/public-read.js

# 결과 요약을 개인 경로에 저장
PERF_WORKER_DISABLED=true PERF_VUS=1 PERF_DURATION=3m \
  k6 run --summary-export \
  "../../docs/private/performance/results/$(date +%F)-public-read-$(git rev-parse --short HEAD)/summary.json" \
  scenarios/public-read.js
```

### 환경변수

| 변수 | 기본값 | 용도 |
|---|---|---|
| `PERF_WORKER_DISABLED` | 없음 (**필수**) | 외부 호출 차단 확인 선언 |
| `BASE_URL` | `http://127.0.0.1:8080` | 측정 대상 |
| `PERF_MODE` | 없음 | `smoke`면 1 iteration으로 끝낸다 |
| `PERF_VUS` | `1` | 동시 사용자 수 |
| `PERF_DURATION` | `3m` | 실행 시간 (smoke에서는 무시) |
| `PERF_SENTENCE_ID` | `1` | 상세 조회 대상. 실행 간 고정해야 비교 가능 |
| `PERF_RECOMMENDED_LIMIT` | `20` | 추천 목록 개수 (서버 허용 1~50) |
| `PERF_TARGET` | `local` | `staging`일 때만 비로컬 host 허용 |
| `PERF_CONFIRM_STAGING` | 없음 | staging 2차 확인 |

## 측정용 mock 데이터

측정 조건을 통제하기 위한 전용 데이터를 만든다. seed 없이 측정하면 로컬에 우연히 쌓인 데이터를 재는 셈이어서 재현도 전후 비교도 불가능하다.

```bash
# 생성 (기존 perf 데이터를 지우고 다시 만든다)
PERF_CONFIRM_SEED=true ./backend/performance/scripts/seed-performance-data.sh --seed

# 현황 확인 (확인 변수 불필요)
./backend/performance/scripts/seed-performance-data.sh --status

# 삭제
PERF_CONFIRM_SEED=true ./backend/performance/scripts/seed-performance-data.sh --cleanup
```

### 기본 구성

| 사용자 (`perf-seed-*`) | 기록 수 | 기록당 음절 | 용도 |
|---|---:|---:|---|
| `perf-seed-10` | 10 | 8 | Phase 5 비교군 |
| `perf-seed-100` | 100 | 8 | 〃 |
| `perf-seed-1k` | 1,000 | 8 | 〃 |
| `perf-seed-10k` | 10,000 | 8 | 〃 |
| `perf-seed-100k` | 100,000 | 8 | `count(*)`·`max(score)`·깊은 offset 스트레스 |
| `perf-seed-long` | 1,000 | 80 | `#45` N+1 payload와 `#46` 저장 경로 증폭 |

합계 112,110 기록 / 968,880 음절 / 238,220 단어.

`PERF_COHORTS="이름:기록수:음절수 ..."`로 바꿀 수 있다.

### 실측 크기 (seed 후 `ANALYZE TABLE` 기준)

| 테이블 | 행 수 | avg 행 크기 | 크기 |
|---|---:|---:|---:|
| `evaluation_syllable` | 966,705 | 57 B | 164.4 MB |
| `evaluation_word` | 237,900 | 55 B | 31.6 MB |
| `evaluation_log` | 111,390 | 146 B | 29.1 MB |
| **합계** | | | **225.1 MB** |

**buffer pool 128 MB의 1.76배**이므로 목표 조건(cold read 발생)을 충족한다.

> 처음에는 기록 1건을 3.7 KB로 잡아 약 490 MB를 예상했다. 그 계산은 35행짜리 테이블의
> `avg_row_length`(585 B / 344 B)를 근거로 삼았는데, 작은 테이블에서는 InnoDB 페이지
> 오버헤드가 과대 반영되어 실제보다 크게 나온다. 실제 규모에서 측정한 값은 위 표와 같다.
> **규모를 추정할 때는 반드시 seed 후 `ANALYZE TABLE`로 재측정한다.**

### 규모를 이렇게 정한 이유

N+1의 실제 비용은 query 개수가 아니라 **디스크를 때리는 round trip 수**에서 나온다. 데이터가 buffer pool에 전부 들어가면 추가 query 38개가 모두 메모리 적중이라 지연 차이가 거의 생기지 않는다.

| 기록 수 | 예상 크기 | buffer pool(128 MB) 대비 |
|---:|---:|---|
| 10,000 | ~20 MB | 전부 적재 → **query 수 차이가 지연으로 드러나지 않는다** |
| 100,000 | ~200 MB | 1.6배 → cold read 발생 ✅ |
| 1,000,000 | ~2 GB | Docker 할당(3.8 GiB)의 절반. seed·cleanup이 느려져 반복 실행성이 떨어진다 |

### 알려진 현실성 한계

seed 행은 **실제 운영 행보다 좁다.** `mouth_guide_url`·`tongue_guide_url`을 NULL로 두고 문자열도 짧게 넣었기 때문이다(`evaluation_syllable` 57 B). 운영에서는 가이드 URL이 최대 500자씩 들어가므로 행이 훨씬 넓다.

이것이 측정에 주는 영향: `#45`의 추가 query 38개가 옮기는 **바이트 수가 실제보다 적다.** 따라서 이 dataset에서 측정한 개선폭은 **보수적인 하한**으로 읽어야 한다. 운영 데이터 폭에 맞추려면 seed에 가이드 URL을 채워 행을 넓히면 된다.

### 사용자별 행 수는 N+1을 증폭하지 않는다

`#45`의 N+1은 **page size에만 비례**한다(`size=20` → 43 queries). 사용자가 1,000건을 갖든 100만 건을 갖든 첫 페이지 query 수는 같다. 사용자별 행 수가 영향을 주는 것은 `count(*)`, `max(score)` 집계, 깊은 offset page다.

N+1 자체를 증폭하는 것은 **기록당 자식 행 수**이므로 `perf-seed-long` 코호트를 따로 둔다. query 수는 그대로 43개지만 각 query가 옮기는 데이터가 10배가 된다.

### 안전장치

- `PERF_CONFIRM_SEED=true` 없이는 seed·cleanup이 실행되지 않는다
- `social_id`가 `perf-seed-`로 시작하는 사용자와 그에 매달린 행만 건드린다
- 로컬 compose 컨테이너가 아니면 거부한다. 대상 컨테이너와 volume 이름을 실행 시 출력한다
- 비밀값은 `MYSQL_PWD`와 stdin으로만 전달하고 출력하지 않는다
- **`syllables`는 삭제하지 않는다.** 여러 사용자가 공유하는 참조 데이터이므로 cleanup 대상이 아니다. seed는 음절 pool을 `INSERT IGNORE`로 채우며 기존 가이드 URL을 보존한다

### cleanup 격리 검증 결과

실데이터 보존을 실제로 확인했다.

| 항목 | cleanup 전 | cleanup 후 |
|---|---:|---:|
| 실사용자 | 2 | 2 |
| 실사용자 기록 | 35 | 35 |
| 실사용자 음절 | 267 | 267 |
| 실사용자 단어 | 46 | 46 |
| 잔여 perf 사용자 | — | 0 |
| orphan 음절 행 | — | 0 |

## 환경 기록

측정 환경은 `docs/private/performance/api-baseline.md` 머리말에 직접 적는다. commit, Docker 할당 자원, MySQL·buffer pool 설정, 데이터 규모가 결과 해석에 필요한 최소 항목이다.

## 결과 해석에서 반드시 적을 것

- **k6와 Docker Compose가 같은 Host 자원을 경쟁한다.** 이 Mac은 Docker에 4 CPU / 약 3.8 GiB만 할당되어 있어 20 RPS 구간에서는 부하 발생기 자체가 병목일 수 있다.
- `dropped_iterations`가 0이 아니면 목표 요청률을 만들지 못한 것이므로, 성공 요청의 latency만 보고 통과로 읽지 않는다.
- 로컬 loopback 결과는 staging·운영 성능을 보장하지 않는다.
- 같은 조건 3회의 중앙값과 편차를 신뢰한다. 한 번의 좋은 결과는 신뢰하지 않는다.

## threshold

`config.js`가 scenario tag 기준으로 threshold를 둔다. 전체 요청 하나로 묶으면 느린 endpoint가 평균에 가려지기 때문이다.

| 대상 | 초기 기준 |
|---|---|
| `public_read` p95 | 300ms |
| 기록 조회 p95 | 500ms |
| 오류율 | 1% 미만 |
| checks | 100% |
| `dropped_iterations` | 0 |

이 값은 최종 용량 약속이 아니라 기존 NFR을 검증하기 위한 첫 기준이다.
