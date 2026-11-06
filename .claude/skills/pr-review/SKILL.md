---
name: pr-review
description: LingKo PR 리뷰. PR diff와 연결된 코드를 읽고 correctness·regression·security·테스트 공백·API 계약·문서 동기화·주석 원칙·도메인 규칙을 점검해 심각도 순으로 보고하고, 요청 시 GitHub에 리뷰 코멘트를 남긴다. 사용자가 PR 리뷰를 요청할 때 또는 /pr-review <번호> 로 호출한다.
---

# PR 리뷰

대상은 PR 번호다. 번호가 없으면 현재 브랜치의 PR(`gh pr view --json number`)을 쓴다. 리뷰 결과는 먼저 대화에만 보고하고, **GitHub에 게시하는 것은 사용자가 요청할 때만** 한다.

## 1. 수집

```bash
gh pr view <n> --json title,body,baseRefName,headRefName,files,commits
gh pr diff <n>
gh pr checks <n>
```

diff만 보지 않는다. 변경된 함수의 호출부, 바뀐 DTO를 쓰는 앱 코드, 관련 테스트, 관련 문서를 Grep/Read로 확인한다. 로컬 checkout이 필요하면 작업 트리가 깨끗한지 먼저 확인하고 `gh pr checkout <n>` 한다. 끝나면 원래 브랜치로 돌아간다.

## 2. 점검 순서

중요한 순서대로 본다. 스타일은 마지막이다.

1. **Correctness**: 로직 오류, 경계값, null, 예외 경로, 동시성·transaction·idempotency
2. **Regression**: 기존 호출부·앱 클라이언트·기존 데이터가 깨지는지
3. **Security**: 인증·권한, 입력 검증(시스템 경계), Secret 노출, 로그의 개인정보
4. **테스트 공백**: 새 분기·실패 경로에 테스트가 있는지
5. **API 계약**: endpoint·DTO·error code가 바뀌었으면 `docs/api/api-reference.md`, `docs/api/error-codes.md`, 앱 client가 같이 바뀌었는지
6. **문서 동기화**: 기능·설정·migration 변경에 맞춰 활성 문서와 `.env.example` 이 갱신됐는지
7. **도메인 규칙**: CLAUDE.md 「자주 틀리는 도메인 규칙」(평가 기회 5회·1시간 충전, WAV 조건, 페이지 크기, 원본 음성 삭제, 기존 migration 불변 등)
8. **주석 원칙**: 새 public 선언, nullable·상태값, fallback 경로의 의도 주석
9. **PR 범위**: 한 기능만 담았는지, 본문이 `.github/pull_request_template.md` 의 섹션을 모두 채웠는지, 체크한 항목이 실제 diff와 맞는지

## 3. 보고

finding마다 아래를 쓴다. 확신이 낮은 것은 "확인 필요"로 표시하고, 추측을 사실처럼 쓰지 않는다.

```
[심각도] path/to/File.java:123 — 한 줄 요약
- 문제: 어떤 입력·상태에서 무엇이 잘못되는지
- 제안: 고치는 방법
```

심각도: `Blocker`(머지 전 필수) / `Major`(머지 전 권장) / `Minor` / `Nit`. 심각도 순으로 정렬하고, 문제가 없으면 "발견 없음"과 확인한 범위를 적는다.

## 4. GitHub 게시 (요청 시)

- 줄 단위 코멘트는 review로 묶어 한 번에 게시한다:

```bash
gh api repos/{owner}/{repo}/pulls/<n>/reviews --method POST --input - <<'EOF'
{"event":"COMMENT","body":"<요약>","commit_id":"<head sha>",
 "comments":[{"path":"<file>","line":<line>,"side":"RIGHT","body":"<finding>"}]}
EOF
```

- 자기 PR에는 `REQUEST_CHANGES`·`APPROVE` 를 쓸 수 없으므로 `COMMENT` 를 쓴다.
- 더 깊은 자동 리뷰가 필요하면 `/code-review` 를 함께 제안할 수 있다.
