#!/usr/bin/env bash
# perf 사용자용 Access Token과 활성 세션을 만든다.
#
# 인증이 필요한 측정(`GET /api/evaluations/me`)의 선행 조건이다. 보호 API는 JWT 서명만
# 검증하지 않고 `sid` claim이 가리키는 세션 행이 **존재하고 revoke·만료되지 않았는지**까지
# 확인한다(ADR-0002). 그래서 토큰 서명과 세션 행을 함께 만든다.
#
# 실제 OAuth 공급자를 호출하지 않는 이유: TODO 안전장치가 "Apple·Google 공급자에 반복
# 로그인"을 금지한다. 로컬 측정은 공급자 신원 확인 이후 단계만 필요하므로 서버와 같은
# 방식으로 토큰을 직접 서명한다.
#
# 토큰은 Git에서 제외된 `backend/.local/`에만 쓴다. TODO 안전장치가 Token을 결과 파일과
# 로그에 남기지 못하게 하므로 측정 결과 디렉터리에는 절대 저장하지 않는다.
set -Eeuo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="$BACKEND_DIR/.env"

MYSQL_CONTAINER=${PERF_MYSQL_CONTAINER:-lingko-mysql}
DB_NAME=${PERF_DB_NAME:-lingko}
PERF_USER_PREFIX=${PERF_USER_PREFIX:-perf-seed-}

OUTPUT_DIR="$BACKEND_DIR/.local"
OUTPUT_FILE="$OUTPUT_DIR/perf-tokens.json"

# 세션 수명. 12시간으로 뒀다가 이틀 뒤 재측정에서 인증 endpoint가 전부 401이 되어
# 오류율 50%로만 드러난 적이 있다. 로컬 전용 fixture이므로 넉넉히 두되 무기한은 피한다.
SESSION_HOURS=${PERF_SESSION_HOURS:-168}

fail() {
  echo "❌ $*" >&2
  exit 1
}

command -v python3 >/dev/null 2>&1 || fail "python3를 찾을 수 없습니다."
[[ -f $ENV_FILE ]] || fail "backend/.env가 없습니다."
docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1 || fail "컨테이너를 찾을 수 없습니다: $MYSQL_CONTAINER"
[[ $(docker inspect --format '{{.State.Status}}' "$MYSQL_CONTAINER") == "running" ]] \
  || fail "$MYSQL_CONTAINER가 실행 중이 아닙니다."

[[ ${PERF_CONFIRM_SEED:-} == "true" ]] \
  || fail "PERF_CONFIRM_SEED=true 가 필요합니다. 세션 행을 생성하는 작업입니다."

DB_PASSWORD=$(grep -m1 '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2- | tr -d '\r')
JWT_SECRET=$(grep -m1 '^JWT_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2- | tr -d '\r')
JWT_ALGORITHM=$(grep -m1 '^JWT_ALGORITHM=' "$ENV_FILE" | cut -d= -f2- | tr -d '\r')
JWT_ALGORITHM=${JWT_ALGORITHM:-HS256}

[[ -n $DB_PASSWORD ]] || fail ".env에서 DB_PASSWORD를 읽을 수 없습니다."
[[ -n $JWT_SECRET ]] || fail ".env에서 JWT_SECRET_KEY를 읽을 수 없습니다."
[[ $JWT_ALGORITHM == "HS256" ]] || fail "이 script는 HS256만 지원합니다: $JWT_ALGORITHM"

run_sql() {
  docker exec -i -e MYSQL_PWD="$DB_PASSWORD" "$MYSQL_CONTAINER" \
    mysql -uroot -D "$DB_NAME" -N \
    --default-character-set=utf8mb4 \
    --init-command="SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci" "$@"
}

# perf 사용자 목록과 각자의 기록 수를 가져온다. 기록 수는 결과 해석에서 코호트를 구분하는 값이다.
users_tsv=$(run_sql <<SQL
SELECT u.user_idx,
       SUBSTRING(u.social_id, CHAR_LENGTH('${PERF_USER_PREFIX}') + 1),
       (SELECT COUNT(*) FROM evaluation_log l WHERE l.user_idx = u.user_idx)
  FROM users u
 WHERE u.social_id LIKE '${PERF_USER_PREFIX}%'
 ORDER BY 3;
SQL
)

[[ -n $users_tsv ]] || fail "perf 사용자가 없습니다. 먼저 seed-performance-data.sh --seed 를 실행하세요."

mkdir -p "$OUTPUT_DIR"

# 기존 perf 세션을 정리해 반복 실행 시 세션 행이 누적되지 않게 한다.
run_sql >/dev/null <<SQL
DELETE s FROM auth_refresh_sessions s
  JOIN users u ON u.user_idx = s.user_idx
 WHERE u.social_id LIKE '${PERF_USER_PREFIX}%';
SQL

# 토큰 생성과 세션 행 INSERT 문을 함께 만든다. 비밀값은 stdin으로만 주고 출력하지 않는다.
umask 077
sql_file=$(mktemp)
py_file=$(mktemp)
trap 'rm -f "$sql_file" "$py_file"' EXIT

# python 본문을 파일로 분리한다. 같은 명령에 heredoc을 두 번 쓰면 뒤쪽 redirection이
# stdin을 덮어써 program 자체가 입력 데이터로 읽힌다.
cat >"$py_file" <<'PY'
import base64, hashlib, hmac, json, os, sys, time, uuid

secret = os.environ["JWT_SECRET"].encode("utf-8")
session_hours = int(os.environ["SESSION_HOURS"])
output_file = os.environ["OUTPUT_FILE"]
sql_file = os.environ["SQL_FILE"]


def b64url(raw: bytes) -> str:
    """서버와 같은 base64url(padding 없음) 인코딩을 사용한다."""
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def encode_segment(value: dict) -> str:
    # 서버는 Jackson 기본 설정으로 공백 없이 직렬화하므로 separators를 맞춘다.
    return b64url(json.dumps(value, separators=(",", ":")).encode("utf-8"))


now = int(time.time())
expires = now + session_hours * 3600

users, statements = [], []
for line in sys.stdin.read().splitlines():
    line = line.strip()
    if not line:
        continue
    user_idx, cohort, logs = line.split("\t")
    session_id = str(uuid.uuid4())

    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": str(user_idx),
        "typ": "access",
        "iat": now,
        "exp": expires,
        "jti": str(uuid.uuid4()),
        "sid": session_id,
    }
    signing_input = f"{encode_segment(header)}.{encode_segment(payload)}"
    signature = b64url(hmac.new(secret, signing_input.encode("utf-8"), hashlib.sha256).digest())
    token = f"{signing_input}.{signature}"

    # current_token_hash는 Refresh Token 회전 검증에만 쓰이고 Access 인증 경로는 보지 않는다.
    # 실제 Refresh Token을 만들지 않으므로 무작위 64 hex를 넣어 NOT NULL만 충족한다.
    statements.append(
        "INSERT INTO auth_refresh_sessions "
        "(session_id, user_idx, current_token_hash, expires_at, revoked_at, created_at, updated_at) "
        f"VALUES ('{session_id}', {user_idx}, '{uuid.uuid4().hex + uuid.uuid4().hex}', "
        f"FROM_UNIXTIME({expires}), NULL, NOW(6), NOW(6));"
    )
    users.append({"cohort": cohort, "userId": int(user_idx), "logs": int(logs), "token": token})

with open(sql_file, "w", encoding="utf-8") as handle:
    handle.write("\n".join(statements) + "\n")

with open(output_file, "w", encoding="utf-8") as handle:
    json.dump(
        {
            "note": "성능 측정 전용. Git 제외 경로이며 결과 파일·로그에 복사하지 않는다.",
            "expiresAtEpoch": expires,
            "users": users,
        },
        handle,
        ensure_ascii=False,
        indent=2,
    )

print(f"토큰 {len(users)}개 생성", file=sys.stderr)
for user in users:
    print(f"  {user['cohort']}: userId={user['userId']} logs={user['logs']}", file=sys.stderr)
PY

printf '%s\n' "$users_tsv" | JWT_SECRET="$JWT_SECRET" SESSION_HOURS="$SESSION_HOURS" \
  OUTPUT_FILE="$OUTPUT_FILE" SQL_FILE="$sql_file" python3 "$py_file"

run_sql <"$sql_file" >/dev/null

session_count=$(run_sql <<SQL
SELECT COUNT(*) FROM auth_refresh_sessions s
  JOIN users u ON u.user_idx = s.user_idx
 WHERE u.social_id LIKE '${PERF_USER_PREFIX}%' AND s.revoked_at IS NULL;
SQL
)

chmod 600 "$OUTPUT_FILE"
echo
echo "활성 세션: ${session_count}개"
echo "토큰 파일: $OUTPUT_FILE (권한 600, Git 제외 경로)"
