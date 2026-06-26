# dmux Hooks

This directory contains hooks that run automatically at key lifecycle events in dmux.

## Quick Start

1. **Read the documentation**:
   - `AGENTS.md` - Hook reference for the current single-AI workflow
   - `CLAUDE.md` - Same hook reference for Claude-compatible tooling

2. **Check examples**:
   - `examples/` directory contains starter templates

3. **Create a hook**:
   ```bash
   touch worktree_created
   chmod +x worktree_created
   nano worktree_created
   ```

4. **Test it**:
   ```bash
   export DMUX_ROOT="$(pwd)"
   export DMUX_WORKTREE_PATH="$(pwd)"
   ./worktree_created
   ```

## Available Hooks

- `before_pane_create` - Before pane creation
- `pane_created` - After pane created
- `worktree_created` - After worktree setup
- `before_pane_close` - Before closing
- `pane_closed` - After closed
- `before_worktree_remove` - Before worktree removal
- `worktree_removed` - After worktree removed
- `pre_merge` - Before merge
- `post_merge` - After merge
- `run_test` - When running tests
- `run_dev` - When starting dev server

## Documentation

See `AGENTS.md` or `CLAUDE.md` for hook documentation including:
- Environment variables
- HTTP callback API
- Common patterns
- Best practices
- Testing strategies

## Note

This directory is **version controlled**. Hooks you create here will be shared with your team.

The project runtime is a single Codex AI in the main tmux pane. Server and verification panes are user-operated terminals, not delegated AI roles.
