#!/bin/bash

set -euo pipefail

SESSION="${SESSION:-team}"
SUBMIT_DELAY="${SUBMIT_DELAY:-0.3}"

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <main|developer|review-qa> <message>" >&2
    exit 2
fi

TARGET="$1"
shift
MESSAGE="$*"

if ! command -v tmux >/dev/null 2>&1; then
    echo "tmux command not found" >&2
    exit 1
fi

if ! tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "tmux session not found: $SESSION" >&2
    exit 1
fi

case "$TARGET" in
    main|메인)
        TARGET_TITLE="메인"
        FALLBACK_PANE="$SESSION:0.0"
        ;;
    developer|dev|구현)
        TARGET_TITLE="구현"
        FALLBACK_PANE="$SESSION:0.1"
        ;;
    review-qa|review|qa|"리뷰 & QA")
        TARGET_TITLE="리뷰 & QA"
        FALLBACK_PANE="$SESSION:0.2"
        ;;
    *)
        echo "Unknown target: $TARGET" >&2
        echo "Allowed targets: main, developer, review-qa" >&2
        exit 2
        ;;
esac

PANE_ID="$(
    tmux list-panes -t "$SESSION:0" -F '#{pane_id} #{pane_title}' |
        awk -v title="$TARGET_TITLE" '$0 ~ " " title "$" { print $1; exit }'
)"

if [ -z "$PANE_ID" ]; then
    PANE_ID="$FALLBACK_PANE"
fi

PROMPT="[$TARGET_TITLE pane 전달]
$MESSAGE"

BUFFER_NAME="send-to-agent-$$"
TMP_FILE="$(mktemp)"

cleanup() {
    rm -f "$TMP_FILE"
    tmux delete-buffer -b "$BUFFER_NAME" 2>/dev/null || true
}
trap cleanup EXIT

printf '%s' "$PROMPT" > "$TMP_FILE"
tmux load-buffer -b "$BUFFER_NAME" "$TMP_FILE"
tmux paste-buffer -t "$PANE_ID" -b "$BUFFER_NAME"
sleep "$SUBMIT_DELAY"
tmux send-keys -t "$PANE_ID" C-m
