// 인증이 필요 없는 읽기 경로의 기준선 scenario다.
//
// 측정 대상: health, 추천 문장 목록, 문장 상세. 세 endpoint는 비용 특성이 달라 tag를 나눠
// 전체 평균이 느린 쪽을 가리지 않게 한다. 외부 서비스를 호출하지 않는 범위만 포함한다.
//
// 실행 방법은 ../README.md 참고. 단독 실행:
//   PERF_WORKER_DISABLED=true k6 run scenarios/public-read.js

import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, COMMON_PARAMS, TARGETS, thresholdsFor } from '../config.js';

const SCENARIO_TAG = 'public_read';

/** 문장 상세 조회 대상. 응답이 일정해야 비교 가능하므로 실행 간 고정한다. */
const SENTENCE_ID = Number(__ENV.PERF_SENTENCE_ID || 1);

/** 추천 목록 조회 개수. 서버 허용 범위는 1~50이다. */
const RECOMMENDED_LIMIT = Number(__ENV.PERF_RECOMMENDED_LIMIT || 20);

/**
 * 추천 목록의 항목 수.
 *
 * latency만 보면 seed 데이터가 비어 있어 빠른 것과 최적화로 빨라진 것을 구분할 수 없다.
 * 응답 규모를 함께 기록해 결과 해석에서 두 경우를 분리한다.
 */
const recommendedItemCount = new Trend('lingko_recommended_item_count');

export const options = {
  scenarios: {
    [SCENARIO_TAG]: {
      executor: __ENV.PERF_MODE === 'smoke' ? 'per-vu-iterations' : 'constant-vus',
      vus: Number(__ENV.PERF_VUS || 1),
      // smoke는 1 VU × 1 iteration으로 계약만 확인하고 부하를 만들지 않는다.
      ...(__ENV.PERF_MODE === 'smoke'
        ? { iterations: 1, maxDuration: '30s' }
        : { duration: __ENV.PERF_DURATION || '3m' }),
      tags: { scenario: SCENARIO_TAG },
    },
  },
  thresholds: thresholdsFor(SCENARIO_TAG, TARGETS.publicReadP95Ms),
  // 요청·응답 본문은 결과 파일에 남기지 않는다. 음성·토큰·개인정보 유출 경로를 차단한다.
  discardResponseBodies: false,
  noConnectionReuse: false,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function publicRead() {
  group('health', () => {
    const response = http.get(`${BASE_URL}/actuator/health`, {
      ...COMMON_PARAMS,
      tags: { endpoint: 'health' },
    });
    check(response, {
      'health 200': (r) => r.status === 200,
      // JVM 기동만이 아니라 DB 연결까지 포함한 계약을 확인한다. CD의 판정 기준과 같다.
      'health UP': (r) => safeJson(r, 'status') === 'UP',
    });
  });

  group('recommended', () => {
    const response = http.get(
      `${BASE_URL}/api/sentences/recommended?limit=${RECOMMENDED_LIMIT}`,
      { ...COMMON_PARAMS, tags: { endpoint: 'recommended' } }
    );
    const items = safeJson(response, 'items');
    check(response, {
      'recommended 200': (r) => r.status === 200,
      'recommended JSON': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
      'recommended items 배열': () => Array.isArray(items),
      // 필수 계약: 항목이 있으면 식별자와 표준 발음이 채워져 있어야 한다.
      'recommended 필수 field': () =>
        !Array.isArray(items) ||
        items.length === 0 ||
        (items[0].sentenceId !== undefined && !!items[0].standardPronunciation),
    });
    if (Array.isArray(items)) {
      recommendedItemCount.add(items.length);
    }
  });

  group('detail', () => {
    const response = http.get(`${BASE_URL}/api/sentences/${SENTENCE_ID}`, {
      ...COMMON_PARAMS,
      tags: { endpoint: 'detail' },
    });
    check(response, {
      'detail 200': (r) => r.status === 200,
      'detail sentenceId 일치': (r) => safeJson(r, 'sentenceId') === SENTENCE_ID,
      'detail 표준 발음 존재': (r) => !!safeJson(r, 'standardPronunciation'),
    });
  });
}

/**
 * 응답 본문에서 field를 읽되 JSON이 아니어도 test를 중단하지 않는다.
 *
 * 서버가 오류 HTML이나 빈 본문을 돌려주는 상황은 측정 중에 충분히 일어난다. 그때 script가
 * 예외로 죽으면 "몇 번째 요청부터 깨졌는지"를 잃어버리므로, 파싱 실패는 check 실패로만
 * 드러내고 iteration은 계속 진행한다.
 *
 * @returns field 값. 파싱 실패 시 undefined
 */
function safeJson(response, field) {
  try {
    const body = response.json();
    return body === null || body === undefined ? undefined : body[field];
  } catch (error) {
    return undefined;
  }
}
