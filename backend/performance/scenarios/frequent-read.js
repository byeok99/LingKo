// 많이 불리는 API 묶음의 1단계(스모크)·2단계(평소 부하) scenario다. 계획은 ../plan.md 2~3절.
//
// open model(`ramping-arrival-rate`)을 쓴다. closed model은 서버가 느려지면 요청도 덜 보내
// 부하가 스스로 줄어들기 때문에, 목표 RPS를 실제로 버티는지 판정할 수 없다.
//
// iteration 1회 = 요청 1건이다. 그래서 iteration rate가 곧 RPS다. endpoint는 MIX 비율대로
// 돌아가며 고른다. 무작위 대신 iteration 번호로 고르는 이유는 실행마다 같은 순서의 요청을
// 보내야 결과를 비교할 수 있기 때문이다.
//
// 제외: `GET /api/evaluations/jobs/{id}` polling. 작업을 만드는 `POST /jobs`가 실제 S3 객체를
// 요구해 로컬에서 만들 수 없다. 로컬 S3 대체 수단이 준비되면 MIX에 추가한다.
//
// 실행 (README의 실행 준비와 perf 토큰 생성이 선행 조건):
//   PERF_WORKER_DISABLED=true PERF_MODE=smoke k6 run scenarios/frequent-read.js
//   PERF_WORKER_DISABLED=true k6 run scenarios/frequent-read.js        # 0→20 RPS 3분, 20 RPS 10분

import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { BASE_URL, COMMON_PARAMS, TARGETS, endpointThresholds, thresholdsFor } from '../config.js';

const SCENARIO_TAG = 'frequent_read';
const SMOKE = __ENV.PERF_MODE === 'smoke';

const FIXTURE = JSON.parse(open('../../.local/perf-tokens.json'));
// quota 조회는 기록 수와 무관하므로 어느 perf 사용자든 결과가 같다. 첫 사용자를 고정해 쓴다.
const AUTH_HEADERS = {
  ...COMMON_PARAMS.headers,
  Authorization: `Bearer ${FIXTURE.users[0].token}`,
};

/** prepare 입력 문장. 길이가 다른 문장을 돌려 써서 한 문장만 반복하는 편향을 줄인다. */
const PREPARE_TEXTS = [
  '안녕하세요',
  '안녕하세요 반갑습니다',
  '오늘 날씨가 정말 좋아서 공원에 산책하러 갔어요',
];

/**
 * endpoint별 요청 비율. plan.md 2절의 가정 비율에서 로컬 측정이 가능한 API만 남겼다.
 * quota 2 : prepare 2 : recommended 1 : detail 1 이다. 운영 실측 비율이 나오면 교체한다.
 */
const ENDPOINTS = {
  quota_today: 2,
  pronunciation_prepare: 2,
  sentences_recommended: 1,
  sentences_detail: 1,
};
const MIX = Object.entries(ENDPOINTS).flatMap(([name, weight]) => Array(weight).fill(name));

const TARGET_RPS = Number(__ENV.PERF_RPS || 20);

export const options = {
  scenarios: {
    [SCENARIO_TAG]: SMOKE
      ? {
          // 스모크는 부하 없이 MIX의 endpoint를 한 번씩 모두 거치게 한다.
          executor: 'per-vu-iterations',
          vus: 1,
          iterations: MIX.length,
          maxDuration: '1m',
          tags: { scenario: SCENARIO_TAG },
        }
      : {
          executor: 'ramping-arrival-rate',
          startRate: 0,
          timeUnit: '1s',
          preAllocatedVUs: Number(__ENV.PERF_PRE_VUS || 10),
          maxVUs: Number(__ENV.PERF_MAX_VUS || 50),
          stages: [
            { target: TARGET_RPS, duration: __ENV.PERF_RAMP || '3m' },
            { target: TARGET_RPS, duration: __ENV.PERF_HOLD || '10m' },
          ],
          tags: { scenario: SCENARIO_TAG },
        },
  },
  thresholds: {
    ...thresholdsFor(SCENARIO_TAG, TARGETS.publicReadP95Ms),
    ...endpointThresholds(Object.keys(ENDPOINTS), TARGETS.publicReadP95Ms),
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function frequentRead() {
  const name = MIX[exec.scenario.iterationInTest % MIX.length];
  REQUESTS[name]();
}

const REQUESTS = {
  quota_today: () => {
    const response = get('quota_today', '/api/quota/today', AUTH_HEADERS);
    check(response, {
      'quota_today 200': (r) => r.status === 200,
    });
  },
  pronunciation_prepare: () => {
    const text = PREPARE_TEXTS[exec.scenario.iterationInTest % PREPARE_TEXTS.length];
    const response = http.post(
      `${BASE_URL}/api/pronunciation/prepare`,
      JSON.stringify({ source: 'CUSTOM', text }),
      {
        ...COMMON_PARAMS,
        headers: { ...COMMON_PARAMS.headers, 'Content-Type': 'application/json' },
        tags: { endpoint: 'pronunciation_prepare' },
      }
    );
    check(response, {
      'pronunciation_prepare 200': (r) => r.status === 200,
    });
  },
  sentences_recommended: () => {
    const response = get('sentences_recommended', '/api/sentences/recommended?limit=20');
    check(response, {
      'sentences_recommended 200': (r) => r.status === 200,
      'sentences_recommended items 배열': (r) => Array.isArray(safeJson(r, 'items')),
    });
  },
  sentences_detail: () => {
    const response = get('sentences_detail', '/api/sentences/1');
    check(response, {
      'sentences_detail 200': (r) => r.status === 200,
      'sentences_detail sentenceId 일치': (r) => safeJson(r, 'sentenceId') === 1,
    });
  },
};

function get(name, path, headers = COMMON_PARAMS.headers) {
  return http.get(`${BASE_URL}${path}`, { ...COMMON_PARAMS, headers, tags: { endpoint: name } });
}

/** JSON이 아닌 오류 응답에서도 iteration을 죽이지 않고 check 실패로만 드러낸다. */
function safeJson(response, field) {
  try {
    const body = response.json();
    return body === null || body === undefined ? undefined : body[field];
  } catch (error) {
    return undefined;
  }
}
