---
name: issue-create
description: LingKo GitHub Issue 작성. 버그·기능·운영 작업을 저장소 Issue 형식(현상·원인·영향 또는 배경·작업 범위·완료 기준·제외 범위)으로 정리해 gh issue create 로 등록한다. 사용자가 이슈 생성을 요청할 때 또는 /issue-create 로 호출한다.
---

# Issue 생성

사용자가 요청할 때만 Issue를 만든다. Issue는 공개 저장소에 바로 게시되므로 **등록 전에 제목·본문·라벨을 보여주고 확인을 받는다.**

## 1. 사전 확인

1. `gh issue list --state all --search "<핵심 키워드>" --limit 10` 으로 중복 Issue를 찾는다. 같은 문제면 새로 만들지 않고 기존 Issue 번호를 알려준다.
2. 버그면 관련 코드·로그·재현 조건을 먼저 확인한다. 확인하지 못한 원인은 "추정" 또는 "미확인"으로 쓴다.

## 2. 템플릿 선택

템플릿은 `.github/ISSUE_TEMPLATE/` 이 유일한 기준이다. 스킬에 양식을 따로 두지 않는다. **작성할 때마다 템플릿 파일을 Read로 읽고** 그 구조를 그대로 채운다.

| 종류 | 템플릿 | 기본 라벨 |
|---|---|---|
| 기대와 다르게 동작·실패 | `bug_report.md` | `bug` |
| 기능·개선·리팩터링·운영·문서 | `task.md` | `enhancement` (문서만이면 `documentation`) |

- 제목·라벨 규칙은 템플릿 frontmatter와 상단 주석을 따른다.
- 섹션 제목과 순서를 바꾸거나 빼지 않는다. 채울 내용이 없으면 "없음" 또는 "미확인"이라고 쓴다.
- `<!-- -->` 안내 주석과 frontmatter는 본문에 넣지 않는다.
- 새 라벨은 만들지 않는다.

## 3. 등록

확인을 받은 뒤 heredoc으로 본문을 넘긴다.

```bash
gh issue create --title "<제목>" --label "<라벨>" --body "$(cat <<'EOF'
<본문>
EOF
)"
```

등록 후 Issue 번호와 URL을 알려준다.

## 금지

- token, Secret, 개인정보, 운영 credential, 긴 로그 전문을 본문에 넣지 않는다.
- `docs/private/` 경로를 링크하거나 개인 운영 메모를 옮기지 않는다. 공개할 기술 내용만 쓴다.
