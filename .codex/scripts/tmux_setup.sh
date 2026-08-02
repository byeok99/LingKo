#!/usr/bin/env bash

set -Eeuo pipefail

# tmux + Codex single-AI multi-pane setup
# 구조:
# - pane 1: Codex AI 메인
# - pane 2: 서버 실행용
# - pane 3: 로그 / 테스트 / git diff 확인용

SESSION="${SESSION:-team}"
PROJECT_DIR="${PROJECT_DIR:-$(pwd)}"

CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
GLOBAL_CODEX_AGENTS_DIR="$CODEX_HOME/agents"
GLOBAL_CODEX_PROMPTS_DIR="$CODEX_HOME/prompts"
GLOBAL_CODEX_AGENTS_MD="$CODEX_HOME/AGENTS.md"
GLOBAL_CODEX_CONFIG="$CODEX_HOME/config.toml"
PROJECT_CODEX_AGENTS_DIR="$PROJECT_DIR/.codex/agents"

PROJECT_SKILL_DIR="$PROJECT_DIR/.agents/skills"
GLOBAL_SKILL_DIR="$HOME/.agents/skills"

DEFAULT_MODEL="${CODEX_MODEL:-gpt-5.6-sol}"
MAIN_REASONING="${MAIN_REASONING:-medium}"

CODEX_START_DELAY="${CODEX_START_DELAY:-4}"
CODEX_READY_TIMEOUT="${CODEX_READY_TIMEOUT:-30}"
CODEX_STABLE_SECONDS="${CODEX_STABLE_SECONDS:-2}"

LOG_DIR="${LOG_DIR:-$PROJECT_DIR/.codex/logs}"
RUN_DIR="${RUN_DIR:-$PROJECT_DIR/.codex/tmp/tmux-team}"

mkdir -p "$LOG_DIR" "$RUN_DIR"

now() { date '+%Y-%m-%d %H:%M:%S'; }
info() { echo "[$(now)] $*"; }
warn() { echo "⚠️ $*"; }
fail() { echo "❌ $*" >&2; exit 1; }

validate_nonnegative_integer() {
  local name="$1" value="$2"

  case "$value" in
    ''|*[!0-9]*)
      fail "$name 값은 0 이상의 정수여야 합니다: $value"
      ;;
  esac
}

validate_nonnegative_integer "CODEX_START_DELAY" "$CODEX_START_DELAY"
validate_nonnegative_integer "CODEX_READY_TIMEOUT" "$CODEX_READY_TIMEOUT"
validate_nonnegative_integer "CODEX_STABLE_SECONDS" "$CODEX_STABLE_SECONDS"

CODEX_START_DELAY=$((10#$CODEX_START_DELAY))
CODEX_READY_TIMEOUT=$((10#$CODEX_READY_TIMEOUT))
CODEX_STABLE_SECONDS=$((10#$CODEX_STABLE_SECONDS))

info "Codex start delay: $CODEX_START_DELAY"
info "Codex ready timeout: $CODEX_READY_TIMEOUT"
info "Codex stable seconds: $CODEX_STABLE_SECONDS"

if ! command -v tmux >/dev/null 2>&1; then
  fail "tmux 명령어를 찾을 수 없습니다."
fi

if ! command -v codex >/dev/null 2>&1; then
  fail "codex 명령어를 찾을 수 없습니다. Codex CLI 설치 또는 PATH 설정을 확인해주세요."
fi

CODEX_BIN="$(command -v codex)"

TMUX_VALUE="${TMUX:-}"
TMUX_SOCKET_DIR="${TMUX_VALUE%%,*}"

if [ -z "$TMUX_SOCKET_DIR" ]; then
  TMUX_SOCKET_DIR="/private/tmp/tmux-$(id -u)/default"
fi

TMUX_SOCKET_PARENT="$(dirname "$TMUX_SOCKET_DIR")"

info "Codex binary: $CODEX_BIN"

if ! "$CODEX_BIN" --version >/dev/null 2>&1; then
  warn "codex --version 실행이 실패했습니다. 상태를 확인하세요."
else
  info "Codex version: $("$CODEX_BIN" --version 2>/dev/null | head -1)"
fi

check_path() {
  local kind="$1"
  local path="$2"
  local msg="$3"

  case "$kind" in
    file)
      [ -f "$path" ] || warn "$msg: $path"
      ;;
    dir)
      [ -d "$path" ] || warn "$msg: $path"
      ;;
  esac
}

check_path file "$GLOBAL_CODEX_CONFIG" "Codex config.toml을 찾을 수 없습니다"
check_path file "$GLOBAL_CODEX_AGENTS_MD" "Codex AGENTS.md를 찾을 수 없습니다"
check_path dir "$GLOBAL_CODEX_AGENTS_DIR" "Codex 공식 subagent 디렉터리를 찾을 수 없습니다"
check_path dir "$PROJECT_CODEX_AGENTS_DIR" "프로젝트 agent 지침 디렉터리를 찾을 수 없습니다"

make_main_prompt() {
  local agent_file="$1"

  cat <<PROMPT_EOF
너는 tmux 세션에서 유일하게 실행되는 Codex AI다.

프로젝트 경로:
$PROJECT_DIR

환경:
- 전역 Codex 지침: $GLOBAL_CODEX_AGENTS_MD
- 전역 Codex 설정: $GLOBAL_CODEX_CONFIG
- Codex 공식 subagent TOML 경로: $GLOBAL_CODEX_AGENTS_DIR
- Codex prompts 경로: $GLOBAL_CODEX_PROMPTS_DIR
- 프로젝트 role 지침: $agent_file

작업 skills 경로:
- 프로젝트: $PROJECT_SKILL_DIR
- 전역: $GLOBAL_SKILL_DIR

현재 tmux pane 구성:
- pane 1: AI 메인, Codex 실행 중
- pane 2: 서버 실행용, 예: npm run dev, uvicorn, docker compose
- pane 3: 로그/검증용, 예: git status, git diff, test, tail -f

운영 원칙:
- 이 세션에서 AI는 너 하나만 실행된다.
- 다른 pane에는 AI가 없으므로 작업을 위임하지 않는다.
- Codex 공식 subagent는 사용자가 명시적으로 요청했을 때만 사용한다.
- 불필요하게 여러 agent, subagent를 호출하지 않는다.
- 전체 코드베이스 탐색보다 관련 파일 중심으로 확인한다.
- 수정 전에는 확인할 파일 범위를 짧게 말한다.
- 수정 후에는 변경 파일, 변경 이유, git diff 기준 요약을 짧게 보고한다.
- 긴 로그 전체를 요구하지 말고, 핵심 에러 부분만 요청한다.
- 사용자가 서버 실행, 테스트, 로그 확인, git diff 확인은 다른 pane에서 직접 수행한다고 가정한다.
- 항상 적용할 개발 지침은 AGENTS.md를 따른다.
- 시작 시 프로젝트 role 지침 파일이 존재하면 읽고, 현재 pane의 우선 운영 규칙으로 적용한다.

역할:
- 요구사항 정리
- 코드 수정
- 테스트 방법 안내
- 오류 원인 분석
- 리뷰 및 회귀 위험 점검
- 최종 변경사항 요약

응답 첫머리에 "AI 메인" 역할임을 짧게 확인해라.
PROMPT_EOF
}

write_runner() {
  local role_key="$1"
  local model="$2"
  local reasoning="$3"
  local prompt_file="$4"

  local runner_file="$RUN_DIR/run_${role_key}.sh"

  cat > "$runner_file" <<RUNNER_EOF
#!/usr/bin/env bash
set -Eeuo pipefail

cd "$PROJECT_DIR"

unset LC_ALL
export LANG="en_US.UTF-8"
export LC_CTYPE="en_US.UTF-8"

IFS= read -r -d '' PROMPT_TEXT < "$prompt_file" || true

CODEX_ARGS=(
  "$CODEX_BIN"
  --model "$model"
  --config "model_reasoning_effort=$reasoning"
  --sandbox workspace-write
  --ask-for-approval on-request
  --add-dir "$TMUX_SOCKET_PARENT"
  "\$PROMPT_TEXT"
)

exec "\${CODEX_ARGS[@]}"
RUNNER_EOF

  chmod +x "$runner_file"
  printf '%s' "$runner_file"
}

capture_pane_log() {
  local pane="$1"
  local role="$2"
  local log_file="$3"

  {
    echo ""
    echo "===== tmux capture for $role / $pane / $(now) ====="
    tmux capture-pane -t "$pane" -p -S -300 2>/dev/null || true
  } >> "$log_file"
}

wait_for_codex_pane() {
  local pane="$1"
  local role="$2"
  local log_file="$3"

  local waited=0
  local initial_wait="$CODEX_START_DELAY"
  local stable_since=-1
  local codex_seen=0
  local command="unknown"

  if [ "$initial_wait" -gt "$CODEX_READY_TIMEOUT" ]; then
    initial_wait="$CODEX_READY_TIMEOUT"
  fi

  [ "$initial_wait" -gt 0 ] && sleep "$initial_wait"
  waited="$initial_wait"

  while [ "$waited" -le "$CODEX_READY_TIMEOUT" ]; do
    command="$(tmux display-message -p -t "$pane" "#{pane_current_command}" 2>/dev/null || true)"

    case "$command" in
      *codex*)
        codex_seen=1

        if [ "$stable_since" -lt 0 ]; then
          stable_since="$waited"
        fi

        if [ $((waited - stable_since)) -ge "$CODEX_STABLE_SECONDS" ]; then
          return 0
        fi
        ;;
      zsh|bash|fish|sh)
        stable_since=-1

        if [ "$codex_seen" -eq 1 ] || [ "$waited" -gt $((CODEX_START_DELAY + 1)) ]; then
          break
        fi
        ;;
      *)
        stable_since=-1
        ;;
    esac

    [ "$waited" -ge "$CODEX_READY_TIMEOUT" ] && break

    sleep 1
    waited=$((waited + 1))
  done

  capture_pane_log "$pane" "$role" "$log_file"

  echo "❌ $role pane에서 Codex가 정상 유지되지 않았습니다. 현재 명령어: ${command:-unknown}"
  tail -20 "$log_file" 2>/dev/null || true

  return 1
}

set_pane_titles() {
  tmux select-pane -t "$MAIN_PANE" -T "AI 메인" 2>/dev/null || true
  tmux select-pane -t "$SERVER_PANE" -T "서버 실행" 2>/dev/null || true
  tmux select-pane -t "$CHECK_PANE" -T "로그/검증" 2>/dev/null || true
}

start_codex_in_main_pane() {
  local pane="$1"
  local role_key="main"
  local model="$DEFAULT_MODEL"
  local reasoning="$MAIN_REASONING"
  local agent_file="$PROJECT_CODEX_AGENTS_DIR/main.md"

  local log_file="$LOG_DIR/${role_key}_$(date +%Y%m%d_%H%M%S).log"
  local prompt_file="$RUN_DIR/prompt_${role_key}.txt"
  local runner_file

  make_main_prompt "$agent_file" > "$prompt_file"

  runner_file="$(write_runner "$role_key" "$model" "$reasoning" "$prompt_file")"

  tmux send-keys -t "$pane" C-c 2>/dev/null || true
  sleep 0.1
  tmux send-keys -t "$pane" C-u 2>/dev/null || true
  sleep 0.1

  tmux send-keys -t "$pane" "bash $(printf '%q' "$runner_file")" Enter

  wait_for_codex_pane "$pane" "AI 메인" "$log_file"
}

prepare_server_pane() {
  local pane="$1"

  tmux send-keys -t "$pane" "clear" Enter
  tmux send-keys -t "$pane" "echo '🖥️ 서버 실행 pane'" Enter
  tmux send-keys -t "$pane" "echo ''" Enter
  tmux send-keys -t "$pane" "echo '여기서는 서버를 실행하세요.'" Enter
  tmux send-keys -t "$pane" "echo ''" Enter
  tmux send-keys -t "$pane" "echo '예시:'" Enter
  tmux send-keys -t "$pane" "echo '  cd front && npm run dev'" Enter
  tmux send-keys -t "$pane" "echo '  cd back && uvicorn main:app --reload'" Enter
  tmux send-keys -t "$pane" "echo '  docker compose up'" Enter
}

prepare_check_pane() {
  local pane="$1"

  tmux send-keys -t "$pane" "clear" Enter
  tmux send-keys -t "$pane" "echo '🔍 로그 / 검증 pane'" Enter
  tmux send-keys -t "$pane" "echo ''" Enter
  tmux send-keys -t "$pane" "echo '여기서는 변경사항, 테스트, 로그를 확인하세요.'" Enter
  tmux send-keys -t "$pane" "echo ''" Enter
  tmux send-keys -t "$pane" "echo '자주 쓰는 명령어:'" Enter
  tmux send-keys -t "$pane" "echo '  git status'" Enter
  tmux send-keys -t "$pane" "echo '  git diff'" Enter
  tmux send-keys -t "$pane" "echo '  git diff --stat'" Enter
  tmux send-keys -t "$pane" "echo '  npm test'" Enter
  tmux send-keys -t "$pane" "echo '  pytest'" Enter
  tmux send-keys -t "$pane" "echo '  tail -f app.log'" Enter
}

if tmux has-session -t "$SESSION" 2>/dev/null; then
  warn "기존 tmux 세션 '$SESSION'을 종료합니다."
  tmux kill-session -t "$SESSION"
fi

TERM_WIDTH="$(tput cols 2>/dev/null || echo 240)"
TERM_HEIGHT="$(tput lines 2>/dev/null || echo 60)"

tmux new-session -d -s "$SESSION" -x "$TERM_WIDTH" -y "$TERM_HEIGHT" -c "$PROJECT_DIR"

MAIN_PANE="$(tmux display-message -p -t "$SESSION:0.0" "#{pane_id}")"
SERVER_PANE="$(tmux split-window -h -P -F "#{pane_id}" -t "$MAIN_PANE" -c "$PROJECT_DIR")"
CHECK_PANE="$(tmux split-window -v -P -F "#{pane_id}" -t "$SERVER_PANE" -c "$PROJECT_DIR")"

tmux select-layout -t "$SESSION:0" main-vertical >/dev/null 2>&1 || true
tmux set-option -t "$SESSION" main-pane-width 90 >/dev/null 2>&1 || true

tmux set-window-option -t "$SESSION:0" pane-border-status top >/dev/null 2>&1 || true
tmux set-window-option -t "$SESSION:0" pane-border-format " #{pane_index}: #T " >/dev/null 2>&1 || true
tmux set-window-option -t "$SESSION:0" automatic-rename off >/dev/null 2>&1 || true
tmux set-window-option -t "$SESSION:0" allow-rename off >/dev/null 2>&1 || true

set_pane_titles

start_codex_in_main_pane "$MAIN_PANE"

prepare_server_pane "$SERVER_PANE"
prepare_check_pane "$CHECK_PANE"

set_pane_titles

tmux select-pane -t "$MAIN_PANE" >/dev/null 2>&1 || true

cat <<DONE

✅ Codex 단일 AI tmux 세션 구성 완료

📌 세션 이름: $SESSION
🧠 모델: $DEFAULT_MODEL
👉 접속 명령어:
   tmux attach -t $SESSION

구성:
- AI 메인 pane: Codex 실행
- 서버 실행 pane: npm run dev / uvicorn / docker compose
- 로그/검증 pane: git status / git diff / test / log 확인

토큰 절약 포인트:
- Codex는 1개 pane에서만 실행됩니다.
- 나머지 pane은 AI가 아니라 일반 터미널입니다.
- 로그는 전체를 붙이지 말고 핵심 에러 부분만 AI에게 전달하세요.

DONE
