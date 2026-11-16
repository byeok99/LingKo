// scenario와 threshold 공통 설정이다.
//
// scenario를 한 파일에 섞지 않고 여기서 조립하는 이유는 전체 평균이 느린 endpoint를 가리는
// 것을 막기 위해서다. threshold도 전체 요청 하나가 아니라 scenario tag 기준으로 둔다.

import { resolveBaseUrl, requireExternalCallsDisabled } from './guards.js';

requireExternalCallsDisabled();

export const BASE_URL = resolveBaseUrl();

/**
 * 초기 통과 기준.
 *
 * `performance-evaluation-todo.md` §3의 값을 그대로 쓴다. 최종 용량 약속이 아니라 기존 NFR을
 * 검증하기 위한 첫 기준이며, 로컬 loopback 결과는 staging·운영 성능을 보장하지 않는다.
 */
export const TARGETS = {
  publicReadP95Ms: 300,
  historyP95Ms: 500,
  writeP95Ms: 500,
  errorRate: 0.01,
};

/**
 * scenario tag별 threshold.
 *
 * `dropped_iterations`는 요청을 아예 만들지 못한 경우이므로 성공 요청의 latency와 분리해
 * 0으로 둔다. 이 값이 0이 아니면 결과를 통과로 읽지 않는다.
 */
export function thresholdsFor(scenarioTag, p95Ms) {
  return {
    [`http_req_duration{scenario:${scenarioTag}}`]: [`p(95)<${p95Ms}`],
    [`http_req_failed{scenario:${scenarioTag}}`]: [`rate<${TARGETS.errorRate}`],
    [`checks{scenario:${scenarioTag}}`]: ['rate==1.0'],
    dropped_iterations: ['count==0'],
  };
}

/**
 * endpoint tag별 threshold.
 *
 * threshold에 참조되지 않은 submetric은 `--summary-export` 결과에 남지 않는다. endpoint마다
 * 선언해야 혼합 scenario에서도 endpoint별 지연·실패율을 따로 읽을 수 있다.
 */
export function endpointThresholds(endpointNames, p95Ms) {
  const thresholds = {};
  for (const name of endpointNames) {
    thresholds[`http_req_duration{endpoint:${name}}`] = [`p(95)<${p95Ms}`];
    thresholds[`http_req_failed{endpoint:${name}}`] = [`rate<${TARGETS.errorRate}`];
  }
  return thresholds;
}

// 결과 요약은 script가 경로를 만들지 않고 `k6 run --summary-export`로 저장한다.
// 경로 규칙(TODO 문서 §7)은 README의 실행 명령이 소유한다. script가 경로를 들고 있으면
// 실행마다 환경변수를 맞춰야 하고, 잘못된 디렉터리에 조용히 쓰일 위험이 생긴다.

/** 모든 요청에 붙이는 공통 parameter. 응답 본문 외의 추적 정보는 남기지 않는다. */
export const COMMON_PARAMS = {
  headers: { Accept: 'application/json' },
  // k6 기본 timeout(60s)은 부하 test에서 너무 길다. 느린 응답을 실패로 빨리 드러낸다.
  timeout: '10s',
};
