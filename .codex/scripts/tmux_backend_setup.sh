#!/bin/bash

set -euo pipefail

SESSION="${SESSION:-team}"
PROJECT_DIR="$(pwd)"

# ECC sync 기준 경로
CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
GLOBAL_CODEX_AGENTS_DIR="$CODEX_HOME/agents"
GLOBAL_CODEX_PROMPTS_DIR="$CODEX_HOME/prompts"
GLOBAL_CODEX_AGENTS_MD="$CODEX_HOME/AGENTS.md"
GLOBAL_CODEX_CONFIG="$CODEX_HOME/config.toml"

# Codex Skills 공식 경로
PROJECT_SKILL_DIR="$PROJECT_DIR/.agents/skills"
GLOBAL_SKILL_DIR="$HOME/.agents/skills"

# send_to_agent.sh 위치 자동 탐색
if [ -x "$HOME/.codex/send_to_agent.sh" ]; then
    SEND_TO_AGENT="$HOME/.codex/send_to_agent.sh"
elif [ -x "$HOME/.codex/scripts/send_to_agent.sh" ]; then
    SEND_TO_AGENT="$HOME/.codex/scripts/send_to_agent.sh"
else
    SEND_TO_AGENT="$HOME/.codex/send_to_agent.sh"
fi

# 필요하면 실행 시 환경변수로 덮어쓸 수 있음
# 예:
# CODEX_MODEL="gpt-5.5" MAIN_REASONING="medium" ./tmux_team_setup.sh
DEFAULT_MODEL="${CODEX_MODEL:-gpt-5.5}"

MAIN_REASONING="${MAIN_REASONING:-medium}"
DEV_REASONING="${DEV_REASONING:-medium}"
QA_REASONING="${QA_REASONING:-medium}"

CODEX_START_DELAY="${CODEX_START_DELAY:-7}"

TMUX_VALUE="${TMUX:-}"
TMUX_SOCKET_DIR="${TMUX_VALUE%%,*}"

if [ -z "$TMUX_SOCKET_DIR" ]; then
    TMUX_SOCKET_DIR="/private/tmp/tmux-$(id -u)/default"
fi

TMUX_SOCKET_PARENT="$(dirname "$TMUX_SOCKET_DIR")"

if ! command -v tmux >/dev/null 2>&1; then
    echo "❌ tmux 명령어를 찾을 수 없습니다."
    exit 1
fi

if ! command -v codex >/dev/null 2>&1; then
    echo "❌ codex 명령어를 찾을 수 없습니다."
    echo "👉 Codex CLI 설치 또는 PATH 설정을 확인해주세요."
    exit 1
fi

if [ ! -f "$GLOBAL_CODEX_CONFIG" ]; then
    echo "⚠️ Codex config.toml을 찾을 수 없습니다: $GLOBAL_CODEX_CONFIG"
    echo "   ECC sync가 정상 실행되었는지 확인해주세요."
fi

if [ ! -f "$GLOBAL_CODEX_AGENTS_MD" ]; then
    echo "⚠️ Codex AGENTS.md를 찾을 수 없습니다: $GLOBAL_CODEX_AGENTS_MD"
    echo "   ECC sync가 정상 실행되었는지 확인해주세요."
fi

if [ ! -d "$GLOBAL_CODEX_AGENTS_DIR" ]; then
    echo "⚠️ Codex 공식 subagent 디렉터리를 찾을 수 없습니다: $GLOBAL_CODEX_AGENTS_DIR"
    echo "   ECC sync가 정상 실행되었는지 확인해주세요."
fi

if [ ! -d "$GLOBAL_SKILL_DIR" ]; then
    echo "⚠️ 전역 skills 디렉터리를 찾을 수 없습니다: $GLOBAL_SKILL_DIR"
    echo "   ECC sync 스크립트는 skills를 직접 설치하지 않습니다."
    echo "   필요한 경우 ~/.agents/skills에 필요한 skill만 복사하세요."
fi

if [ ! -x "$SEND_TO_AGENT" ]; then
    echo "⚠️ send_to_agent.sh helper를 찾을 수 없거나 실행 권한이 없습니다."
    echo "   확인한 경로:"
    echo "   - $HOME/.codex/send_to_agent.sh"
    echo "   - $HOME/.codex/scripts/send_to_agent.sh"
    echo "   자동 pane 호출 기능은 사용할 수 없습니다."
fi

CODEX_BIN="$(command -v codex)"

if tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "⚠️ 기존 tmux 세션 '$SESSION'을 종료합니다."
    tmux kill-session -t "$SESSION"
fi

TERM_WIDTH=$(tput cols 2>/dev/null || echo 240)
TERM_HEIGHT=$(tput lines 2>/dev/null || echo 60)

tmux new-session -d -s "$SESSION" -x "$TERM_WIDTH" -y "$TERM_HEIGHT" -c "$PROJECT_DIR"

MAIN_PANE="$(tmux display-message -p -t "$SESSION:0.0" "#{pane_id}")"
DEV_PANE="$(tmux split-window -h -P -F "#{pane_id}" -t "$MAIN_PANE" -c "$PROJECT_DIR")"
QA_PANE="$(tmux split-window -v -P -F "#{pane_id}" -t "$DEV_PANE" -c "$PROJECT_DIR")"

tmux select-layout -t "$SESSION:0" main-vertical
tmux set-option -t "$SESSION" main-pane-width 90

# pane title 표시 설정
tmux set-window-option -t "$SESSION:0" pane-border-status top
tmux set-window-option -t "$SESSION:0" pane-border-format " #{pane_index}: #T "

# Codex/shell이 pane title을 덮어쓰는 것 방지
tmux set-window-option -t "$SESSION:0" automatic-rename off 2>/dev/null || true
tmux set-window-option -t "$SESSION:0" allow-rename off 2>/dev/null || true
tmux set-window-option -t "$SESSION:0" allow-set-title off 2>/dev/null || true
tmux set-option -t "$SESSION" set-titles off 2>/dev/null || true

set_pane_titles() {
    tmux select-pane -t "$MAIN_PANE" -T "메인"
    tmux select-pane -t "$DEV_PANE" -T "구현"
    tmux select-pane -t "$QA_PANE" -T "리뷰 & QA"
}

set_pane_titles

build_codex_command() {
    local model="$1"
    local reasoning="$2"

    local args=(
        "$CODEX_BIN"
        --model "$model"
        --config "model_reasoning_effort=$reasoning"
        --sandbox workspace-write
        --ask-for-approval on-request
        --add-dir "$TMUX_SOCKET_PARENT"
    )

    printf '%q ' "${args[@]}"
}

make_role_prompt() {
    local role="$1"
    local dispatch_rule="$2"

    cat <<PROMPT_EOF
너는 tmux 멀티에이전트 팀의 "$role" 역할이다.

프로젝트 경로:
$PROJECT_DIR

ECC 기준 환경:
- 전역 Codex 지침: $GLOBAL_CODEX_AGENTS_MD
- 전역 Codex 설정: $GLOBAL_CODEX_CONFIG
- Codex 공식 subagent TOML 경로: $GLOBAL_CODEX_AGENTS_DIR
- Codex prompts 경로: $GLOBAL_CODEX_PROMPTS_DIR

작업 skills 경로:
- 프로젝트: $PROJECT_SKILL_DIR
- 전역: $GLOBAL_SKILL_DIR

pane 호출 helper:
$SEND_TO_AGENT

pane 호출 권한:
$dispatch_rule

공통 운영 원칙:
- 이 프로젝트는 소규모 프로젝트다.
- 불필요하게 여러 pane, agent, subagent를 호출하지 마라.
- 기본적으로 현재 pane 역할 안에서 해결한다.
- Codex 공식 subagent는 사용자가 명시적으로 요청했을 때만 사용한다.
- 전체 코드베이스 탐색보다 관련 파일 중심으로 확인한다.
- 긴 설명보다 변경 범위, 판단, 다음 행동을 짧게 보고한다.
- 항상 적용할 개발 지침은 AGENTS.md를 따른다.
- 상황별 작업 절차는 .agents/skills 또는 ~/.agents/skills의 SKILL.md를 참고한다.
- .codex/rules/*.rules는 Codex 명령 실행 정책용이며, 개발 규칙 문서가 아니다.

역할별 지침:
PROMPT_EOF

    case "$role" in
        "메인")
            cat <<'PROMPT_EOF'
- 요구사항 정리, 작업 분해, API/아키텍처 판단, 최종 취합을 담당한다.
- 작은 작업은 다른 pane에 보내지 말고 직접 처리한다.
- 구현이 필요한 작업만 구현 pane에 전달한다.
- 리뷰가 필요한 시점에만 리뷰 & QA pane에 전달한다.
- 다른 pane에 보낸 요청과 결과는 사용자에게 요약 보고한다.
- Spring/Flutter 둘 다 관련된 기능은 먼저 API 계약을 짧게 정리한 뒤 진행한다.
PROMPT_EOF
            ;;
        "구현")
            cat <<'PROMPT_EOF'
- Spring 백엔드와 Flutter 앱 구현을 담당한다.
- 메인이 정리한 요구사항과 범위를 우선 따른다.
- 아키텍처 판단이 필요하면 임의로 크게 바꾸지 말고 메인에게 보고한다.
- 백엔드와 앱을 동시에 수정할 때는 변경 범위를 먼저 요약한다.
- 구현 완료 후 변경 파일, 테스트 결과, 남은 위험을 메인에게 보고한다.
- 원칙적으로 리뷰 & QA pane에 직접 작업을 보내지 않는다.
PROMPT_EOF
            ;;
        "리뷰 & QA")
            cat <<'PROMPT_EOF'
- 변경사항 리뷰, 테스트 누락, 보안 위험, 회귀 위험 확인을 담당한다.
- 원칙적으로 직접 구현하지 않는다.
- 발견사항은 심각도 순으로 정리한다.
- 버그 가능성, 보안 문제, 테스트 누락, 사용자 흐름 깨짐을 우선 확인한다.
- 리뷰 완료 후 메인에게 요약 보고한다.
PROMPT_EOF
            ;;
    esac

    cat <<'PROMPT_EOF'

응답 첫머리에 현재 역할을 짧게 확인해라.
PROMPT_EOF
}

start_codex_in_pane() {
    local pane="$1"
    local role="$2"
    local role_key="$3"
    local model="$4"
    local reasoning="$5"
    local dispatch_rule="$6"

    local prompt_file
    prompt_file="$(mktemp)"

    make_role_prompt "$role" "$dispatch_rule" > "$prompt_file"

    tmux send-keys -t "$pane" C-c 2>/dev/null || true
    sleep 0.2
    tmux send-keys -t "$pane" C-u 2>/dev/null || true
    sleep 0.2

    local codex_cmd
    codex_cmd="$(build_codex_command "$model" "$reasoning")"

    tmux select-pane -t "$pane"
    tmux send-keys -t "$pane" "cd $(printf '%q' "$PROJECT_DIR") && $codex_cmd" Enter

    sleep "$CODEX_START_DELAY"

    tmux select-pane -t "$pane"
    tmux load-buffer -b "codex-${role_key}" "$prompt_file"
    tmux paste-buffer -t "$pane" -b "codex-${role_key}"
    sleep 0.1
    tmux send-keys -t "$pane" Enter

    rm -f "$prompt_file"

    # Codex 실행 후 title이 덮어써질 수 있으므로 마지막에 다시 설정
    tmux select-pane -t "$pane" -T "$role"

    echo "✅ $role pane 준비 완료"
    echo "   - pane: $pane"
    echo "   - model: $model"
    echo "   - reasoning: $reasoning"
}

MAIN_DISPATCH_RULE='메인 역할은 필요한 경우에만 send_to_agent.sh를 사용해 구현 또는 리뷰&QA pane에 요청을 보낸다. 다른 pane에 보낸 요청과 결과는 사용자에게 요약 보고한다.'

DEV_DISPATCH_RULE='구현 역할은 원칙적으로 다른 pane에 직접 작업을 전송하지 않는다. 작업 완료 후 필요하면 send_to_agent.sh main "작업 완료: [요약]" 형식으로 메인에게 보고한다.'

QA_DISPATCH_RULE='리뷰 & QA 역할은 원칙적으로 직접 구현하지 않고 발견사항을 메인에게 보고한다. 작업 완료 후 필요하면 send_to_agent.sh main "리뷰 완료: [요약]" 형식으로 메인에게 보고한다.'

start_codex_in_pane "$MAIN_PANE" "메인" "main" "$DEFAULT_MODEL" "$MAIN_REASONING" "$MAIN_DISPATCH_RULE"
start_codex_in_pane "$DEV_PANE" "구현" "developer" "$DEFAULT_MODEL" "$DEV_REASONING" "$DEV_DISPATCH_RULE"
start_codex_in_pane "$QA_PANE" "리뷰 & QA" "review-qa" "$DEFAULT_MODEL" "$QA_REASONING" "$QA_DISPATCH_RULE"

# 모든 Codex 실행 후 한 번 더 title 고정
set_pane_titles

tmux select-pane -t "$MAIN_PANE"

echo ""
echo "✅ Codex 멀티에이전트 tmux 세션 구성 완료"
echo "📁 프로젝트 경로: $PROJECT_DIR"
echo "📌 세션 이름: $SESSION"
echo "🧠 모델: $DEFAULT_MODEL"
echo "🧠 메인 reasoning: $MAIN_REASONING"
echo "🧠 구현 reasoning: $DEV_REASONING"
echo "🧠 리뷰 reasoning: $QA_REASONING"
echo "📨 pane 호출 helper: $SEND_TO_AGENT"
echo "📂 Codex home: $CODEX_HOME"
echo "📂 skill 경로: $PROJECT_SKILL_DIR / $GLOBAL_SKILL_DIR"
echo "👉 접속 명령어: tmux attach -t $SESSION"