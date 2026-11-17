// 외부 서비스를 호출하지 않는 모든 API를 한 번에 측정한다.
//
// endpoint별 tag로 나눠 전체 평균이 느린 endpoint를 가리지 않게 한다.
// 인증 API는 `perf-seed-1k` 사용자 토큰을 쓴다(이력 1,000건).
//
// 실행: PERF_WORKER_DISABLED=true k6 run scenarios/all-apis.js

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, COMMON_PARAMS } from '../config.js';

const FIXTURE = JSON.parse(open('../../.local/perf-tokens.json'));
const USER = FIXTURE.users.find((u) => u.cohort === '1k') || FIXTURE.users[0];
const AUTH = { ...COMMON_PARAMS.headers, Authorization: `Bearer ${USER.token}` };

/**
 * 측정 대상 endpoint 목록.
 *
 * threshold에 참조되지 않은 submetric은 `--summary-export` 결과에 포함되지 않는다.
 * 그래서 endpoint마다 threshold를 선언해 개별 수치가 결과 파일에 남게 한다.
 */
const ENDPOINTS = [
  'health',
  'sentences_recommended',
  'sentences_detail',
  'pronunciation_convert',
  'pronunciation_prepare',
  'sentences_saved',
  'quota_today',
  'evaluations_me',
  'weak_sounds',
  'sound_detail',
];

const thresholds = { 'http_req_failed{scenario:all_apis}': ['rate<0.01'] };
for (const name of ENDPOINTS) {
  // 500ms는 TODO §3의 느슨한 쪽 기준이다. 여기서는 통과·실패 판정보다 수치 기록이 목적이다.
  thresholds[`http_req_duration{endpoint:${name}}`] = ['p(95)<500'];
  thresholds[`http_req_failed{endpoint:${name}}`] = ['rate<0.01'];
}

export const options = {
  scenarios: {
    all_apis: {
      executor: 'constant-vus',
      // 동시성 ramp에 쓴다. read 전용 경로라 모든 VU가 같은 사용자를 써도 행 잠금 경합이
      // 생기지 않으므로, 코호트를 섞지 않고 비교 가능성을 유지한다.
      vus: Number(__ENV.PERF_VUS || 1),
      duration: __ENV.PERF_DURATION || '60s',
      tags: { scenario: 'all_apis' },
    },
  },
  thresholds,
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

function get(name, path, authenticated) {
  const response = http.get(`${BASE_URL}${path}`, {
    ...COMMON_PARAMS,
    headers: authenticated ? AUTH : COMMON_PARAMS.headers,
    tags: { endpoint: name },
  });
  check(response, { [`${name} 200`]: (r) => r.status === 200 });
}

function post(name, path, body) {
  const response = http.post(`${BASE_URL}${path}`, JSON.stringify(body), {
    ...COMMON_PARAMS,
    headers: { ...COMMON_PARAMS.headers, 'Content-Type': 'application/json' },
    tags: { endpoint: name },
  });
  check(response, { [`${name} 200`]: (r) => r.status === 200 });
}

export default function allApis() {
  get('health', '/actuator/health', false);
  get('sentences_recommended', '/api/sentences/recommended?limit=20', false);
  get('sentences_detail', '/api/sentences/1', false);
  post('pronunciation_convert', '/api/pronunciation/convert', { text: '안녕하세요 반갑습니다' });
  post('pronunciation_prepare', '/api/pronunciation/prepare', {
    source: 'CUSTOM',
    text: '안녕하세요 반갑습니다',
  });
  get('sentences_saved', '/api/sentences/saved', true);
  get('quota_today', '/api/quota/today', true);
  get('evaluations_me', '/api/evaluations/me?page=0&size=20', true);
  get('weak_sounds', '/api/evaluations/me/weak-sounds?limit=3', true);
  // 음절 상세는 seed가 만든 음절 pool의 첫 문자를 쓴다.
  get('sound_detail', `/api/evaluations/me/sounds/${encodeURIComponent('가')}`, true);
}
