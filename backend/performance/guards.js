// 부하 test가 의도하지 않은 대상에 요청을 보내지 못하게 막는 안전장치다.
//
// 성능 script의 사고는 대부분 "대상을 잘못 지정했다"에서 시작한다. BASE_URL 오타 하나로
// 운영 서버에 20 RPS를 10분 넣는 상황을 코드로 차단하는 것이 이 파일의 유일한 목적이다.
// 검증은 init context에서 수행해 VU가 하나도 실행되기 전에 test를 중단시킨다.

/** 추가 확인 없이 허용하는 host. 로컬 loopback만 둔다. */
const LOCAL_HOSTS = ['127.0.0.1', 'localhost', '[::1]'];

/** 기본 대상. 환경변수를 주지 않으면 항상 로컬이다. */
export const DEFAULT_BASE_URL = 'http://127.0.0.1:8080';

/**
 * 운영 식별자로 쓰일 수 있는 문자열.
 *
 * 실수로 운영 domain을 넣었을 때 "로컬이 아님" 메시지보다 명확한 이유를 주기 위해 분리했다.
 * 실제 운영 domain과 EC2 주소는 TODO 문서의 안전장치에 따라 저장소에 적지 않는다.
 */
const PRODUCTION_MARKERS = ['amazonaws.com', 'lingko.app', 'lingko.co'];

/**
 * BASE_URL을 검증해 반환한다.
 *
 * 허용 조건:
 * - 기본값 또는 로컬 loopback host
 * - staging은 PERF_TARGET=staging 과 PERF_CONFIRM_STAGING=true 가 모두 있을 때만
 *
 * staging에 두 개의 환경변수를 요구하는 이유는 하나만으로는 오타·복붙으로 통과할 수 있기
 * 때문이다. 둘 다 의도적으로 넣어야 하므로 "실수로 staging을 때리는" 경로가 사라진다.
 *
 * @returns {string} 뒤쪽 slash를 제거한 base URL
 * @throws {Error} 허용되지 않은 대상이면 init context에서 test 전체를 중단시킨다
 */
export function resolveBaseUrl() {
  const raw = (__ENV.BASE_URL || DEFAULT_BASE_URL).trim();
  const target = (__ENV.PERF_TARGET || 'local').trim();
  const confirmed = (__ENV.PERF_CONFIRM_STAGING || '').trim() === 'true';

  // goja 환경에서 URL 생성자 지원이 버전마다 다르므로 정규식으로 직접 분해한다.
  const parsed = /^(https?):\/\/([^/?#]+)(.*)$/i.exec(raw);
  if (!parsed) {
    throw new Error(
      `GUARD_REJECTED: BASE_URL은 http(s)://host[:port] 형식이어야 합니다: ${raw}`
    );
  }

  const authority = parsed[2];
  if (authority.includes('@')) {
    // URL에 들어간 credential은 결과 파일과 로그로 새기 쉬우므로 형식 자체를 금지한다.
    throw new Error('GUARD_REJECTED: BASE_URL에 인증 정보를 포함할 수 없습니다.');
  }

  const host = authority.replace(/:\d+$/, '').toLowerCase();

  const productionMarker = PRODUCTION_MARKERS.find((marker) => host.includes(marker));
  if (productionMarker) {
    throw new Error(
      `GUARD_REJECTED: 운영으로 보이는 host는 대상이 될 수 없습니다(${productionMarker}): ${host}`
    );
  }

  if (LOCAL_HOSTS.includes(host)) {
    return stripTrailingSlash(raw);
  }

  if (target === 'staging') {
    if (!confirmed) {
      throw new Error(
        'GUARD_REJECTED: staging 대상에는 PERF_CONFIRM_STAGING=true 가 함께 필요합니다.'
      );
    }
    return stripTrailingSlash(raw);
  }

  throw new Error(
    `GUARD_REJECTED: 허용된 host가 아닙니다(${host}). ` +
      '로컬은 127.0.0.1 또는 localhost만 사용하고, staging은 ' +
      'PERF_TARGET=staging 과 PERF_CONFIRM_STAGING=true 를 함께 지정하세요.'
  );
}

/**
 * 외부 유료 연동이 꺼진 상태인지 확인한다.
 *
 * TODO 문서의 안전장치 "평가 Worker는 초기 API 기준선 동안 비활성화해 Azure·Replicate
 * 비용을 차단한다"를 script에서도 한 번 더 요구한다. 서버 설정을 script가 직접 읽을 수는
 * 없으므로, 실행자가 확인했다는 사실을 환경변수로 선언하게 만든다.
 *
 * @throws {Error} 선언이 없으면 중단한다
 */
export function requireExternalCallsDisabled() {
  if ((__ENV.PERF_WORKER_DISABLED || '').trim() !== 'true') {
    throw new Error(
      'GUARD_REJECTED: PERF_WORKER_DISABLED=true 가 필요합니다. ' +
        'evaluation-worker를 내리고 .env에 외부 credential이 없는지 확인한 뒤 지정하세요.'
    );
  }
}

function stripTrailingSlash(value) {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}
