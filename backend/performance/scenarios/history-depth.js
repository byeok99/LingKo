// 평가 기록 조회(`GET /api/evaluations/me`)를 이력 크기별로 측정한다.
//
// TODO Phase 5의 "이력 10/100/1,000/10,000건 비교"에 100,000건과 긴 문장 코호트를 더한 것이다.
// `#45` 최적화(기록당 lazy collection 2회 적재 제거)가 실제로 지나가는 **유일한 HTTP 경로**다.
//
// 코호트를 동시에 돌리지 않고 startTime으로 직렬화한다. Docker에 4 CPU만 할당된 환경에서
// 7개 scenario를 함께 돌리면 서로 자원을 빼앗아 코호트별 숫자가 오염된다.
//
// 실행:
//   PERF_WORKER_DISABLED=true k6 run scenarios/history-depth.js

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, COMMON_PARAMS, TARGETS, thresholdsFor } from '../config.js';

// 토큰은 Git 제외 경로에서만 읽는다. 결과 파일·로그에는 절대 복사하지 않는다.
const FIXTURE = JSON.parse(open('../../.local/perf-tokens.json'));

/** 첫 페이지 크기. 운영 앱이 기록 화면에서 쓰는 값과 같다. */
const PAGE_SIZE = Number(__ENV.PERF_HISTORY_PAGE_SIZE || 20);

/** 코호트당 실행 시간. 1 VU 기준이며 p95가 안정될 만큼만 둔다. */
const DURATION_SECONDS = Number(__ENV.PERF_COHORT_SECONDS || 30);

/** 깊은 offset 측정에 사용할 page 번호. 100,000건 코호트에서만 의미가 있다. */
const DEEP_PAGE = Number(__ENV.PERF_DEEP_PAGE || 500);

/**
 * 응답 본문 크기.
 *
 * latency만 보면 "데이터가 적어서 빠른 것"과 "최적화로 빨라진 것"을 구분할 수 없다.
 * 코호트별 응답 규모를 함께 기록해 결과 해석에서 두 경우를 분리한다.
 */
const responseBytes = new Trend('lingko_history_response_bytes');

/** 서버가 보고한 전체 기록 수. 의도한 코호트를 조회했는지 확인하는 근거다. */
const reportedTotal = new Trend('lingko_history_total_items');

const scenarios = {};
const thresholds = {};
let startTime = 0;

// 이력이 적은 코호트부터 실행해, 앞선 코호트가 buffer pool을 채운 상태가 뒤 코호트에
// 유리하게 작용하는 방향(작은 것 → 큰 것)으로 고정한다. 순서를 바꾸면 결과가 달라지므로
// 비교할 때는 항상 같은 순서를 쓴다.
const cohorts = [...FIXTURE.users].sort((left, right) => left.logs - right.logs);

for (const cohort of cohorts) {
  const tag = `history_${cohort.cohort}`;
  scenarios[tag] = {
    executor: 'constant-vus',
    vus: 1,
    duration: `${DURATION_SECONDS}s`,
    startTime: `${startTime}s`,
    exec: 'firstPage',
    env: { COHORT: cohort.cohort },
    tags: { scenario: tag, cohort: cohort.cohort, logs: String(cohort.logs) },
  };
  Object.assign(thresholds, thresholdsFor(tag, TARGETS.historyP95Ms));
  startTime += DURATION_SECONDS;
}

// 깊은 offset은 N+1과 다른 비용(앞 page를 건너뛰는 비용)이므로 별도 scenario로 분리한다.
const deepCohort = cohorts[cohorts.length - 1];
if (deepCohort && deepCohort.logs > DEEP_PAGE * PAGE_SIZE) {
  scenarios.history_deep = {
    executor: 'constant-vus',
    vus: 1,
    duration: `${DURATION_SECONDS}s`,
    startTime: `${startTime}s`,
    exec: 'deepPage',
    env: { COHORT: deepCohort.cohort },
    tags: { scenario: 'history_deep', cohort: deepCohort.cohort },
  };
  Object.assign(thresholds, thresholdsFor('history_deep', TARGETS.historyP95Ms));
}

export const options = {
  scenarios,
  thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function tokenFor(cohortName) {
  const found = FIXTURE.users.find((user) => user.cohort === cohortName);
  if (!found) {
    throw new Error(`fixture에 코호트가 없습니다: ${cohortName}. create-perf-tokens.sh를 먼저 실행하세요.`);
  }
  return found;
}

function requestHistory(cohort, page) {
  const user = tokenFor(cohort);
  const response = http.get(
    `${BASE_URL}/api/evaluations/me?page=${page}&size=${PAGE_SIZE}`,
    {
      ...COMMON_PARAMS,
      headers: { ...COMMON_PARAMS.headers, Authorization: `Bearer ${user.token}` },
      tags: { endpoint: 'history', page: String(page) },
    }
  );

  const body = safeJson(response);
  check(response, {
    'history 200': (r) => r.status === 200,
    'items 배열': () => Array.isArray(body && body.items),
    // 서버가 보고한 전체 건수가 코호트 규모와 같아야 의도한 사용자의 데이터를 본 것이다.
    // 응답에 userId가 없으므로 이것이 사용자 데이터 격리를 확인하는 근거다.
    '코호트 규모 일치': () => body && body.totalItems === user.logs,
    // 자식 적재가 빠지면 query는 줄지만 응답이 축소된다. 그 회귀를 함께 막는다.
    '음절 채워짐': () =>
      body &&
      Array.isArray(body.items) &&
      (body.items.length === 0 ||
        (Array.isArray(body.items[0].characters) && body.items[0].characters.length > 0)),
  });

  if (response.status === 200) {
    responseBytes.add(response.body ? response.body.length : 0);
    if (body && typeof body.totalItems === 'number') {
      reportedTotal.add(body.totalItems);
    }
  }
}

export function firstPage() {
  requestHistory(__ENV.COHORT, 0);
}

export function deepPage() {
  requestHistory(__ENV.COHORT, DEEP_PAGE);
}

/**
 * 응답을 JSON으로 읽되 실패해도 iteration을 멈추지 않는다.
 *
 * 부하 중 서버가 오류 본문이나 빈 응답을 돌려주는 일은 흔하다. 그때 script가 예외로 죽으면
 * "몇 번째 요청부터 깨졌는지"를 잃어버리므로 파싱 실패는 check 실패로만 드러낸다.
 */
function safeJson(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}
