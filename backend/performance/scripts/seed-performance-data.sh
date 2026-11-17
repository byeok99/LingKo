#!/usr/bin/env bash
# 성능 측정용 평가 이력 mock 데이터를 로컬 MySQL에 생성·삭제한다.
#
# 목적은 "많이 넣는 것"이 아니라 **측정 조건을 통제하는 것**이다. seed 없이 측정하면 로컬에
# 우연히 쌓인 데이터를 재는 셈이어서 재현도 전후 비교도 불가능하다.
#
# 규모 근거(실측 기준):
#   evaluation_log 585B + 음절 344B/행 + 단어 390B/행
#   → 기록 1건 ≈ 3.7KB. InnoDB buffer pool 기본값은 128MB.
#   10,000건(37MB)은 buffer pool에 전부 적재되어 query 수 차이가 지연으로 드러나지 않는다.
#   그래서 기본 구성은 buffer pool을 넘기는 규모(약 490MB)를 만든다.
#
# 사용자별 행 수는 #45의 N+1을 증폭하지 않는다. 첫 페이지 query 수는 page size에만 비례하기
# 때문이다(size=20 → 43 queries). 사용자별 행 수가 영향을 주는 것은 count(*),
# max(score) 집계, 깊은 offset page다. N+1 자체를 증폭하는 것은 **기록당 자식 행 수**이므로
# 긴 문장 코호트를 따로 둔다.
#
# 안전 원칙:
#   - social_id가 PERF_USER_PREFIX로 시작하는 사용자와 그에 매달린 행만 건드린다
#   - syllables는 여러 사용자가 공유하는 참조 데이터이므로 **삭제하지 않는다**
#   - 로컬 compose 컨테이너가 아니면 실행을 거부한다
#   - 비밀값은 출력하지 않고 stdin으로만 전달한다
set -Eeuo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="$BACKEND_DIR/.env"

MYSQL_CONTAINER=${PERF_MYSQL_CONTAINER:-lingko-mysql}
DB_NAME=${PERF_DB_NAME:-lingko}

# 전용 test 사용자 식별자. cleanup 대상 판별과 실데이터 보호의 유일한 기준이다.
PERF_USER_PREFIX=${PERF_USER_PREFIX:-perf-seed-}

# 음절 pool 크기. evaluation_syllable.syllable_char가 참조하는 syllables 행 수다.
# 한글 음절은 U+AC00부터 11,172자이며, 흔히 쓰이는 범위를 덮도록 기본 1,000자를 만든다.
SYLLABLE_POOL_SIZE=${PERF_SYLLABLE_POOL_SIZE:-1000}
HANGUL_BASE=44032

# 코호트 정의: "이름:기록수:기록당_음절수"
#
# 앞의 다섯 개는 TODO Phase 5의 10/100/1,000/10,000 비교군에 100,000을 더한 것이다.
# long은 기록당 음절을 10배로 늘려 #45의 N+1 payload와 #46 저장 경로를 증폭한다.
DEFAULT_COHORTS="10:10:8 100:100:8 1k:1000:8 10k:10000:8 100k:100000:8 long:1000:80"
COHORTS=${PERF_COHORTS:-$DEFAULT_COHORTS}

MODE=seed

usage() {
  cat <<USAGE
사용법: $0 [--seed | --cleanup | --status]

  --seed     (기본) 기존 perf 데이터를 지우고 다시 생성한다
  --cleanup  perf 데이터만 삭제한다
  --status   현재 perf 데이터 규모를 보여준다

환경변수:
  PERF_CONFIRM_SEED=true   seed·cleanup 실행에 필수
  PERF_COHORTS             "이름:기록수:음절수" 공백 구분 목록
                           기본: $DEFAULT_COHORTS
  PERF_USER_PREFIX         전용 사용자 social_id 접두사 (기본: $PERF_USER_PREFIX)
  PERF_SYLLABLE_POOL_SIZE  음절 pool 크기 (기본: $SYLLABLE_POOL_SIZE)
  PERF_MYSQL_CONTAINER     대상 컨테이너 (기본: $MYSQL_CONTAINER)

예:
  PERF_CONFIRM_SEED=true $0 --seed
  PERF_CONFIRM_SEED=true PERF_COHORTS="10:10:8 1k:1000:8" $0 --seed
  $0 --status
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --seed) MODE=seed ;;
    --cleanup) MODE=cleanup ;;
    --status) MODE=status ;;
    -h | --help) usage; exit 0 ;;
    *) echo "알 수 없는 옵션: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

fail() {
  echo "❌ $*" >&2
  exit 1
}

# --- 안전장치 -------------------------------------------------------------

command -v docker >/dev/null 2>&1 || fail "docker를 찾을 수 없습니다."
[[ -f $ENV_FILE ]] || fail "backend/.env가 없습니다: $ENV_FILE"

docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1 \
  || fail "컨테이너를 찾을 수 없습니다: $MYSQL_CONTAINER"

container_state=$(docker inspect --format '{{.State.Status}}' "$MYSQL_CONTAINER")
[[ $container_state == "running" ]] || fail "$MYSQL_CONTAINER 상태가 running이 아닙니다: $container_state"

# 운영 DB를 가리킬 수 없음을 구조로 확인한다. 로컬 compose 컨테이너의 내부 MySQL만 대상이다.
mounted_volume=$(docker inspect "$MYSQL_CONTAINER" \
  --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}')
[[ -n $mounted_volume ]] || fail "$MYSQL_CONTAINER의 데이터 volume을 확인할 수 없습니다."

if [[ $MODE != "status" ]]; then
  [[ ${PERF_CONFIRM_SEED:-} == "true" ]] \
    || fail "PERF_CONFIRM_SEED=true 가 필요합니다. 대상: 컨테이너 $MYSQL_CONTAINER / volume $mounted_volume"
fi

DB_PASSWORD=$(grep -m1 '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2- | tr -d '\r')
[[ -n $DB_PASSWORD ]] || fail ".env에서 DB_PASSWORD를 읽을 수 없습니다."

# 비밀값을 process 인자에 노출하지 않도록 MYSQL_PWD와 stdin만 사용한다.
#
# collation을 명시하는 이유: 대상 컬럼은 모두 utf8mb4_unicode_ci인데(compose의
# --collation-server), `--default-character-set=utf8mb4`만 주면 연결 collation이
# utf8mb4_0900_ai_ci가 되어 LIKE 비교에서 "Illegal mix of collations"로 실패한다.
# CONVERT(... USING utf8mb4)로 만든 한글 문자도 연결 collation을 따르므로 함께 맞춰진다.
run_sql() {
  docker exec -i -e MYSQL_PWD="$DB_PASSWORD" "$MYSQL_CONTAINER" \
    mysql -uroot -D "$DB_NAME" \
    --default-character-set=utf8mb4 \
    --init-command="SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci" "$@"
}

# --- 공통 조회 ------------------------------------------------------------

print_status() {
  echo "== perf 데이터 현황 (접두사 '$PERF_USER_PREFIX') =="
  # 자식 테이블을 한 query에서 join하면 음절 × 단어로 행이 곱해져 COUNT가 부풀려진다.
  # 각 집계를 독립 subquery로 분리해 실제 행 수를 센다.
  run_sql --table <<SQL
SELECT u.social_id AS cohort_user,
       (SELECT COUNT(*) FROM evaluation_log l
         WHERE l.user_idx = u.user_idx) AS logs,
       (SELECT COUNT(*) FROM evaluation_syllable s
          JOIN evaluation_log l ON l.evaluation_log_idx = s.evaluation_log_idx
         WHERE l.user_idx = u.user_idx) AS syllables,
       (SELECT COUNT(*) FROM evaluation_word w
          JOIN evaluation_log l ON l.evaluation_log_idx = w.evaluation_log_idx
         WHERE l.user_idx = u.user_idx) AS words
  FROM users u
 WHERE u.social_id LIKE '${PERF_USER_PREFIX}%'
 ORDER BY logs;
SQL

  # bulk insert 직후 information_schema의 크기·행 수 통계는 갱신되지 않아 실제보다 훨씬 작게
  # 보인다(관측: 225MB를 0.2MB로 보고). 규모가 측정 조건의 근거이므로 먼저 통계를 갱신한다.
  run_sql >/dev/null <<SQL
ANALYZE TABLE evaluation_log, evaluation_syllable, evaluation_word, syllables;
SQL

  run_sql --table <<SQL
SELECT table_name,
       table_rows,
       avg_row_length AS avg_bytes,
       ROUND((data_length+index_length)/1024/1024, 1) AS size_MB
  FROM information_schema.tables
 WHERE table_schema='${DB_NAME}'
   AND table_name IN ('evaluation_log','evaluation_syllable','evaluation_word')
 ORDER BY size_MB DESC;
SELECT ROUND(SUM(data_length+index_length)/1024/1024, 1) AS eval_tables_MB,
       @@innodb_buffer_pool_size/1024/1024 AS buffer_pool_MB,
       ROUND(SUM(data_length+index_length)/@@innodb_buffer_pool_size, 2) AS ratio_to_pool
  FROM information_schema.tables
 WHERE table_schema='${DB_NAME}'
   AND table_name IN ('evaluation_log','evaluation_syllable','evaluation_word');
SQL
  echo
  echo "ratio_to_pool이 1보다 커야 cold read가 발생해 N+1 비용이 지연으로 드러난다."
}

# perf 사용자에 매달린 행만 지운다. FK 순서대로 자식부터 삭제한다.
# syllables는 공유 참조 데이터이므로 삭제 대상이 아니다.
cleanup_perf_data() {
  echo "perf 데이터 삭제 중..."
  run_sql <<SQL
SET @prefix = '${PERF_USER_PREFIX}%';

DELETE s FROM evaluation_syllable s
  JOIN evaluation_log l ON l.evaluation_log_idx = s.evaluation_log_idx
  JOIN users u ON u.user_idx = l.user_idx
 WHERE u.social_id LIKE @prefix;

DELETE w FROM evaluation_word w
  JOIN evaluation_log l ON l.evaluation_log_idx = w.evaluation_log_idx
  JOIN users u ON u.user_idx = l.user_idx
 WHERE u.social_id LIKE @prefix;

DELETE l FROM evaluation_log l
  JOIN users u ON u.user_idx = l.user_idx
 WHERE u.social_id LIKE @prefix;

DELETE FROM users WHERE social_id LIKE @prefix;
SQL
  echo "삭제 완료"
}

# --- seed -----------------------------------------------------------------

# 숫자 테이블. 재귀 CTE는 cte_max_recursion_depth(기본 1000)에 걸리므로 cross join으로 만든다.
create_sequence_table() {
  local max_rows=$1
  echo "숫자 테이블 생성 (최대 $max_rows 행)..."
  run_sql <<SQL
DROP TABLE IF EXISTS perf_seq;
CREATE TABLE perf_seq (n INT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO perf_seq (n)
SELECT 1 + d0.v + d1.v*10 + d2.v*100 + d3.v*1000 + d4.v*10000 + d5.v*100000 AS n
  FROM (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d0
 CROSS JOIN (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1
 CROSS JOIN (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2
 CROSS JOIN (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d3
 CROSS JOIN (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d4
 CROSS JOIN (SELECT 0 v UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d5
HAVING n <= $max_rows;
SQL
}

# 음절 pool을 확보한다. 이미 있는 문자는 건드리지 않아 기존 가이드 URL을 보존한다.
seed_syllable_pool() {
  echo "음절 pool 확보 ($SYLLABLE_POOL_SIZE 자)..."
  run_sql <<SQL
INSERT IGNORE INTO syllables (syllable_char, mouth_url, tongue_url)
SELECT CONVERT(CHAR($HANGUL_BASE + n - 1 USING ucs2) USING utf8mb4), NULL, NULL
  FROM perf_seq
 WHERE n <= $SYLLABLE_POOL_SIZE;
SQL
}

seed_cohort() {
  local name=$1 log_count=$2 syllables_per_log=$3
  local social_id="${PERF_USER_PREFIX}${name}"
  # 단어는 5음절마다 하나로 묶어 실제 문장 구조에 가깝게 만든다.
  local words_per_log=$(( (syllables_per_log + 4) / 5 ))

  echo "  [$name] 사용자 생성 및 기록 ${log_count}건 (음절 ${syllables_per_log}/건, 단어 ${words_per_log}/건)"

  run_sql <<SQL
INSERT INTO users (social_id, social_type, email, name, created_at, last_login_at)
VALUES ('${social_id}', 'GOOGLE', NULL, 'perf ${name}', NOW(6), NOW(6));
SET @uid = LAST_INSERT_ID();

-- created_at을 분 단위로 흩어 ORDER BY created_at DESC와 idx_user_created를 실제로 태운다.
INSERT INTO evaluation_log
  (user_idx, original_word, score, created_at, source, sentence_id,
   standard_pronunciation, recognized_text,
   accuracy_score, fluency_score, completeness_score, pronunciation_score, audio_url)
SELECT @uid,
       CONCAT('연습 문장 ', n),
       50 + (n % 50),
       NOW(6) - INTERVAL n MINUTE,
       IF(n % 2 = 0, 'RECOMMENDED', 'CUSTOM'),
       IF(n % 2 = 0, 1 + (n % 48), NULL),
       CONCAT('표준발음 ', n),
       CONCAT('인식결과 ', n),
       50 + (n % 50), 50 + (n % 50), 50 + (n % 50), 50 + (n % 50),
       NULL
  FROM perf_seq
 WHERE n <= ${log_count};
SQL

  # 음절은 position 단위로 나눠 넣는다. 한 statement가 (기록 수)행이므로 transaction 크기가
  # 코호트 크기에 비례해 무한정 커지지 않는다.
  local pos
  for ((pos = 1; pos <= syllables_per_log; pos++)); do
    run_sql <<SQL
INSERT INTO evaluation_syllable
  (evaluation_log_idx, syllable_char, score, position_no, word_position,
   feedback, mouth_guide_url, tongue_guide_url)
SELECT l.evaluation_log_idx,
       -- 기록마다 다른 문자가 나오도록 log id와 position을 섞어 pool에서 고른다.
       CONVERT(CHAR($HANGUL_BASE + ((l.evaluation_log_idx * 31 + ${pos}) % $SYLLABLE_POOL_SIZE) USING ucs2) USING utf8mb4),
       60 + (${pos} % 40),
       ${pos} - 1,
       (${pos} - 1) DIV 5,
       NULL, NULL, NULL
  FROM evaluation_log l
  JOIN users u ON u.user_idx = l.user_idx
 WHERE u.social_id = '${social_id}';
SQL
  done

  for ((pos = 1; pos <= words_per_log; pos++)); do
    run_sql <<SQL
INSERT INTO evaluation_word (evaluation_log_idx, position_no, word_text, score)
SELECT l.evaluation_log_idx,
       ${pos} - 1,
       CONCAT(
         CONVERT(CHAR($HANGUL_BASE + ((l.evaluation_log_idx * 17 + ${pos}) % $SYLLABLE_POOL_SIZE) USING ucs2) USING utf8mb4),
         CONVERT(CHAR($HANGUL_BASE + ((l.evaluation_log_idx * 17 + ${pos} + 1) % $SYLLABLE_POOL_SIZE) USING ucs2) USING utf8mb4)
       ),
       60 + (${pos} % 40)
  FROM evaluation_log l
  JOIN users u ON u.user_idx = l.user_idx
 WHERE u.social_id = '${social_id}';
SQL
  done
}

# --- 실행 -----------------------------------------------------------------

case "$MODE" in
  status)
    print_status
    ;;

  cleanup)
    echo "대상: 컨테이너 $MYSQL_CONTAINER / volume $mounted_volume / DB $DB_NAME"
    cleanup_perf_data
    run_sql <<<"DROP TABLE IF EXISTS perf_seq;"
    print_status
    ;;

  seed)
    echo "대상: 컨테이너 $MYSQL_CONTAINER / volume $mounted_volume / DB $DB_NAME"
    echo "코호트: $COHORTS"
    echo

    # 반복 실행 가능해야 하므로 항상 지운 뒤 다시 만든다.
    cleanup_perf_data

    max_logs=0
    for cohort in $COHORTS; do
      IFS=: read -r _name count _syll <<<"$cohort"
      ((count > max_logs)) && max_logs=$count
    done
    ((SYLLABLE_POOL_SIZE > max_logs)) && max_logs=$SYLLABLE_POOL_SIZE

    create_sequence_table "$max_logs"
    seed_syllable_pool
    echo
    echo "코호트 생성..."

    started=$(date +%s)
    for cohort in $COHORTS; do
      IFS=: read -r name count syll <<<"$cohort"
      [[ $count =~ ^[1-9][0-9]*$ ]] || fail "코호트 기록 수가 올바르지 않습니다: $cohort"
      [[ $syll =~ ^[1-9][0-9]*$ ]] || fail "코호트 음절 수가 올바르지 않습니다: $cohort"
      seed_cohort "$name" "$count" "$syll"
    done
    elapsed=$(( $(date +%s) - started ))

    run_sql <<<"DROP TABLE IF EXISTS perf_seq;"
    echo
    echo "seed 완료 (${elapsed}초)"
    echo
    print_status
    echo
    echo "주의: syllables는 공유 참조 데이터이므로 cleanup이 삭제하지 않습니다."
    echo "측정 전 buffer pool 적재 상태를 맞추려면 warm-up을 먼저 실행하세요."
    ;;
esac
