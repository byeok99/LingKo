# dmux Hooks Reference

이 문서는 `.dmux-hooks/` 안의 훅 스크립트를 수정할 때만 참고한다. 프로젝트의 기본 운영은 단일 Codex AI + tmux 보조 pane 방식이며, dmux 훅 문서가 여러 AI pane 운영을 의미하지 않는다.

## 현재 운영 전제

- AI는 메인 pane 하나에서만 실행된다.
- 서버 실행 pane과 로그/검증 pane은 사용자가 직접 사용하는 터미널이다.
- 훅 작업도 현재 Codex 세션에서 처리하며 다른 AI pane에 위임하지 않는다.
- Codex 공식 subagent는 사용자가 명시적으로 요청한 경우에만 사용한다.
- 긴 로그가 필요하면 전체 로그가 아니라 실패 명령, 핵심 에러 구간, 재현 조건만 확인한다.

## 훅 작성 원칙

- `.dmux-hooks/`의 실행 훅은 bash 스크립트다.
- 새 훅은 shebang으로 시작한다: `#!/bin/bash`
- 실행 권한이 필요하다: `chmod +x .dmux-hooks/<hook_name>`
- 경로는 하드코딩하지 말고 dmux 환경 변수를 사용한다.
- 오래 걸리는 작업은 훅 실행 모델을 확인하고 필요한 경우 background로 돌린다.
- secret, token, 개인 로컬 경로를 커밋하지 않는다.
- 수정 전에는 관련 훅 파일만 확인한다.

## 주요 환경 변수

```bash
DMUX_ROOT="/path/to/project"
DMUX_SERVER_PORT="3142"
DMUX_PANE_ID="dmux-1234567890"
DMUX_SLUG="fix-auth-bug"
DMUX_PROMPT="Fix authentication bug"
DMUX_AGENT="codex"
DMUX_TMUX_PANE_ID="%38"
DMUX_WORKTREE_PATH="/path/.dmux/worktrees/fix-auth-bug"
DMUX_BRANCH="fix-auth-bug"
DMUX_TARGET_BRANCH="main"
```

## 사용 가능한 훅

- `before_pane_create`: pane 생성 전
- `pane_created`: pane 생성 후
- `worktree_created`: worktree 생성 후, agent 시작 전
- `before_pane_close`: pane 종료 전
- `pane_closed`: pane 종료 후
- `before_worktree_remove`: worktree 제거 전
- `worktree_removed`: worktree 제거 후
- `pre_merge`: merge 전
- `post_merge`: merge 후
- `run_test`: 테스트 실행 요청 시
- `run_dev`: 개발 서버 실행 요청 시

## 최소 예시

```bash
#!/bin/bash
set -e

cd "$DMUX_WORKTREE_PATH"

echo "[Hook] Running test command"
./gradlew test
```

## 검증

```bash
bash -n .dmux-hooks/<hook_name>
shellcheck .dmux-hooks/<hook_name>
```

`shellcheck`가 설치되어 있지 않으면 `bash -n`과 수동 실행으로 확인한다.
