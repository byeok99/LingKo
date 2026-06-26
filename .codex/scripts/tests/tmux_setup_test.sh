#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SETUP_SCRIPT="$SCRIPT_DIR/tmux_setup.sh"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

make_fixture() {
    FIXTURE_DIR="$(mktemp -d)"
    mkdir -p "$FIXTURE_DIR/bin" "$FIXTURE_DIR/home/.codex/agents" "$FIXTURE_DIR/project/.codex/agents"
    : > "$FIXTURE_DIR/home/.codex/config.toml"
    : > "$FIXTURE_DIR/home/.codex/AGENTS.md"

    cat > "$FIXTURE_DIR/bin/codex" <<'EOF'
#!/bin/bash
if [ -n "${CODEX_CAPTURE_DIR:-}" ]; then
    if [ "${LC_ALL+x}" = x ]; then
        printf '%s\n' "$LC_ALL" > "$CODEX_CAPTURE_DIR/lc-all"
    else
        printf '<unset>\n' > "$CODEX_CAPTURE_DIR/lc-all"
    fi
    locale charmap > "$CODEX_CAPTURE_DIR/charmap"
    printf '%s' "${!#}" > "$CODEX_CAPTURE_DIR/prompt"
fi
exit 0
EOF

    cat > "$FIXTURE_DIR/bin/locale" <<'EOF'
#!/bin/bash
case "${1:-}" in
    -a)
        printf 'C\nen_US.UTF-8\n'
        ;;
    charmap)
        case "${LC_ALL-}:${LC_CTYPE-}:${LANG-}" in
            :*UTF-8*:*) printf 'UTF-8\n' ;;
            *) printf 'US-ASCII\n' ;;
        esac
        ;;
    *)
        exit 2
        ;;
esac
EOF

    cat > "$FIXTURE_DIR/bin/sleep" <<'EOF'
#!/bin/bash
printf '%s\n' "${1:-0}" >> "${TMUX_TEST_STATE:?}/sleep-log"
exit 0
EOF

    cat > "$FIXTURE_DIR/bin/tmux" <<'EOF'
#!/bin/bash
set -euo pipefail

state_dir="${TMUX_TEST_STATE:?}"
command_name="${1:-}"
shift || true
printf '%s\n' "$command_name" >> "$state_dir/tmux-log"

case "$command_name" in
    has-session)
        exit 1
        ;;
    split-window)
        count_file="$state_dir/split-count"
        count="$(cat "$count_file" 2>/dev/null || echo 0)"
        count=$((count + 1))
        printf '%s\n' "$count" > "$count_file"
        printf '%%%s\n' "$((count + 1))"
        ;;
    display-message)
        arguments=" $* "
        if [[ "$arguments" == *" #{pane_id} "* ]]; then
            printf '%%1\n'
        elif [[ "$arguments" == *" #{pane_current_command} "* ]]; then
            index_file="$state_dir/command-index"
            index="$(cat "$index_file" 2>/dev/null || echo 0)"
            index=$((index + 1))
            printf '%s\n' "$index" > "$index_file"
            value="$(sed -n "${index}p" "$state_dir/commands")"
            if [ -z "$value" ]; then
                value="$(tail -n 1 "$state_dir/commands")"
            fi
            printf '%s\n' "$value"
        fi
        ;;
    *)
        exit 0
        ;;
esac
EOF

    chmod +x "$FIXTURE_DIR/bin/codex" "$FIXTURE_DIR/bin/locale" \
        "$FIXTURE_DIR/bin/sleep" "$FIXTURE_DIR/bin/tmux"
}

cleanup_fixture() {
    rm -rf "$FIXTURE_DIR"
}

run_setup() {
    env \
        PATH="$FIXTURE_DIR/bin:$PATH" \
        HOME="$FIXTURE_DIR/home" \
        CODEX_HOME="$FIXTURE_DIR/home/.codex" \
        TMUX_TEST_STATE="$FIXTURE_DIR" \
        PROJECT_DIR="$FIXTURE_DIR/project" \
        RUN_DIR="$FIXTURE_DIR/run" \
        LOG_DIR="$FIXTURE_DIR/log" \
        SESSION="test-team-$$" \
        CODEX_START_DELAY="${TEST_START_DELAY:-0}" \
        CODEX_READY_TIMEOUT="${TEST_READY_TIMEOUT:-3}" \
        CODEX_STABLE_SECONDS="${TEST_STABLE_SECONDS:-1}" \
        bash "$SETUP_SCRIPT"
}

test_runner_overrides_inherited_non_utf8_lc_all() {
    local capture prompt runner

    make_fixture
    trap cleanup_fixture RETURN
    printf 'codex\ncodex\ncodex\n' > "$FIXTURE_DIR/commands"

    TEST_STABLE_SECONDS=0 run_setup >/dev/null 2>&1 || fail "setup failed while generating runners"
    runner="$FIXTURE_DIR/run/run_main.sh"
    prompt="$FIXTURE_DIR/run/prompt_main.txt"
    capture="$FIXTURE_DIR/capture"
    mkdir -p "$capture"

    LC_ALL=C LANG=C CODEX_RUNNER_NO_TEE=1 CODEX_CAPTURE_DIR="$capture" \
        PATH="$FIXTURE_DIR/bin:$PATH" bash "$runner" || fail "generated runner failed"

    [ "$(cat "$capture/lc-all")" = '<unset>' ] || fail "Codex inherited LC_ALL=C"
    [ "$(cat "$capture/charmap")" = 'UTF-8' ] || fail "Codex effective charmap is not UTF-8"
    cmp -s "$prompt" "$capture/prompt" || fail "Korean prompt argument bytes changed"
    grep -q '^너는 tmux 세션에서 유일하게 실행되는 Codex AI다.' "$capture/prompt" || \
        fail "Korean prompt text was not preserved"
    iconv -f UTF-8 -t UTF-8 "$capture/prompt" >/dev/null 2>&1 || \
        fail "Korean prompt argument is not valid UTF-8"
}

test_waits_through_transient_shell_during_startup() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'zsh\ncodex\ncodex\ncodex\n' > "$FIXTURE_DIR/commands"

    output="$(run_setup 2>&1)" || fail "setup rejected a transient startup shell:\n$output"
    [[ "$output" == *"✅ Codex 단일 AI tmux 세션 구성 완료"* ]] || fail "success message missing"
}

test_fails_when_shell_never_starts_codex() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'zsh\n' > "$FIXTURE_DIR/commands"

    if TEST_READY_TIMEOUT=2 run_setup >/dev/null 2>&1; then
        fail "setup accepted a pane that never started Codex"
    fi
}

test_fails_when_codex_returns_to_shell_before_stable() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'codex\nzsh\n' > "$FIXTURE_DIR/commands"

    if TEST_STABLE_SECONDS=2 run_setup >/dev/null 2>&1; then
        fail "setup accepted Codex exiting back to the shell"
    fi
}

test_stable_seconds_are_elapsed_time_not_poll_count() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'codex\n%.0s' {1..3} > "$FIXTURE_DIR/commands"

    TEST_STABLE_SECONDS=2 run_setup >/dev/null 2>&1 || fail "setup rejected stable Codex panes"
    polls="$(cat "$FIXTURE_DIR/command-index")"
    [ "$polls" -eq 3 ] || fail "stable=2 should require three observations for the main pane; got $polls total"
}

test_timeout_does_not_sleep_past_deadline() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'zsh\n' > "$FIXTURE_DIR/commands"

    TEST_READY_TIMEOUT=2 run_setup >/dev/null 2>&1 && fail "setup accepted a pane that never started Codex"
    readiness_sleeps="$(awk '$1 == 1 { count += 1 } END { print count + 0 }' "$FIXTURE_DIR/sleep-log")"
    [ "$readiness_sleeps" -eq 2 ] || fail "timeout=2 should perform two one-second readiness sleeps; got $readiness_sleeps"
}

test_start_delay_is_capped_by_ready_timeout() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'zsh\n' > "$FIXTURE_DIR/commands"

    TEST_START_DELAY=5 TEST_READY_TIMEOUT=2 run_setup >/dev/null 2>&1 && fail "setup accepted a pane that never started Codex"
    capped_sleeps="$(awk '$1 == 2 { count += 1 } END { print count + 0 }' "$FIXTURE_DIR/sleep-log")"
    [ "$capped_sleeps" -eq 1 ] || fail "start delay should be capped to one two-second sleep; got $capped_sleeps"
}

test_rejects_invalid_timing_values_before_tmux_changes() {
    local variable value output

    for variable in TEST_START_DELAY TEST_READY_TIMEOUT TEST_STABLE_SECONDS; do
        for value in -1 1.5 invalid; do
            make_fixture
            trap cleanup_fixture RETURN
            printf 'codex\n' > "$FIXTURE_DIR/commands"

            printf -v "$variable" '%s' "$value"
            export "$variable"
            output="$(run_setup 2>&1)" && fail "$variable=$value should be rejected"
            unset "$variable"
            [[ "$output" == *"0 이상의 정수"* ]] || fail "validation message missing for $variable=$value"
            [ ! -s "$FIXTURE_DIR/tmux-log" ] || fail "tmux was invoked for invalid $variable=$value"
            cleanup_fixture
            trap - RETURN
        done
    done
}

test_accepts_zero_timing_boundaries() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'codex\ncodex\ncodex\n' > "$FIXTURE_DIR/commands"

    TEST_START_DELAY=0 TEST_READY_TIMEOUT=0 TEST_STABLE_SECONDS=0 run_setup >/dev/null 2>&1 || \
        fail "zero timing boundaries should be accepted"
}

test_accepts_leading_zero_timing_values_as_decimal() {
    make_fixture
    trap cleanup_fixture RETURN
    printf 'codex\ncodex\ncodex\n' > "$FIXTURE_DIR/commands"
    output="$(TEST_START_DELAY=08 TEST_READY_TIMEOUT=8 TEST_STABLE_SECONDS=0 run_setup 2>&1)" || \
        fail "CODEX_START_DELAY=08 should be treated as decimal 8"
    [[ "$output" == *"Codex start delay: 8"* ]] || fail "start delay was not normalized to decimal"

    cleanup_fixture
    make_fixture
    printf 'codex\ncodex\ncodex\n' > "$FIXTURE_DIR/commands"
    output="$(TEST_START_DELAY=0 TEST_READY_TIMEOUT=08 TEST_STABLE_SECONDS=0 run_setup 2>&1)" || \
        fail "CODEX_READY_TIMEOUT=08 should be treated as decimal 8"
    [[ "$output" == *"Codex ready timeout: 8"* ]] || fail "ready timeout was not normalized to decimal"

    cleanup_fixture
    make_fixture
    printf 'codex\n%.0s' {1..27} > "$FIXTURE_DIR/commands"
    output="$(TEST_START_DELAY=0 TEST_READY_TIMEOUT=8 TEST_STABLE_SECONDS=08 run_setup 2>&1)" || \
        fail "CODEX_STABLE_SECONDS=08 should be treated as decimal 8"
    [[ "$output" == *"Codex stable seconds: 8"* ]] || fail "stable seconds was not normalized to decimal"
}

test_waits_through_transient_shell_during_startup
test_fails_when_shell_never_starts_codex
test_fails_when_codex_returns_to_shell_before_stable
test_stable_seconds_are_elapsed_time_not_poll_count
test_timeout_does_not_sleep_past_deadline
test_start_delay_is_capped_by_ready_timeout
test_rejects_invalid_timing_values_before_tmux_changes
test_accepts_zero_timing_boundaries
test_accepts_leading_zero_timing_values_as_decimal
test_runner_overrides_inherited_non_utf8_lc_all
echo "PASS: tmux_setup"
