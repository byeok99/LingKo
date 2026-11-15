---
name: pr-create
description: LingKo Pull Request 생성. 브랜치 범위 점검, push, 저장소 PR 본문 형식(포함 기능·제외한 후속 기능·검증 결과·Stacked PR 의존 관계)으로 gh pr create 를 실행한다. 사용자가 PR 생성을 요청할 때 또는 /pr-create 로 호출한다.
---

# PR 생성

사용자가 요청할 때만 실행한다. push와 PR은 외부에 공개되므로 **push 전에 PR 제목·본문·base를 보여주고 확인을 받는다.**

## 1. 범위 점검

1. 커밋되지 않은 변경이 있으면 먼저 알리고, 필요하면 `commit` 스킬로 정리한다.
2. base를 정한다. 기본은 `develop`. 앞선 PR에 의존하면 그 PR의 브랜치를 base로 하는 stacked PR로 만든다.
3. `git log --oneline <base>..HEAD` 와 `git diff --stat <base>...HEAD` 로 포함 커밋과 파일을 확인한다.
4. 여러 기능이 섞였거나 변경이 크면 PR을 만들지 말고 기능별 분리안을 사용자에게 보고한다.
5. `gh pr list --head <branch>` 로 같은 브랜치의 PR이 이미 있는지 확인한다. 있으면 새로 만들지 않고 본문 갱신을 제안한다.

## 2. 검증

변경 범위에 맞는 검증을 실행하거나 이미 실행한 결과를 모은다(CLAUDE.md 「리뷰와 검증」). 실행하지 못한 검증은 본문에 이유와 함께 ⚠️로 적는다. 실행하지 않은 것을 통과로 쓰지 않는다.

## 3. 제목

`<type>: <한국어 요약>`. 커밋 제목과 같은 규칙(`commit` 스킬 「메시지」)이다. 커밋이 하나면 그 커밋 제목을 쓰고, 여러 개면 PR 전체 목적을 한 줄로 요약한다. 예: `docs: README 구체화와 시스템 설계도 추가`

## 4. 본문

본문은 `.github/pull_request_template.md` 가 유일한 기준이다. **작성할 때마다 그 파일을 Read로 읽고** 구조를 그대로 채운다.

- 섹션 제목과 순서를 바꾸거나 빼지 않는다. 내용이 없으면 "없음"이라고 쓴다.
- `<!-- -->` 안내 주석은 지운다.
- 체크리스트는 실제로 확인한 항목만 `[x]` 로 바꾼다. 확인하지 못한 항목은 `[ ]` 로 두고 「검증 결과」에 ⚠️로 이유를 적는다.
- 마지막 줄에는 하네스가 지정한 PR attribution 줄을 붙인다.

## 5. 실행

확인을 받은 뒤:

```bash
git push -u origin <branch>
gh pr create --base <base> --title "<제목>" --body "$(cat <<'EOF'
<본문>
EOF
)"
```

PR 번호와 URL을 알려준다. CI가 있으면 `gh pr checks <n>` 로 시작 여부만 확인하고, 결과를 기다려야 하면 그렇게 안내한다.

## 금지

- force push를 하지 않는다. 필요하면 이유를 설명하고 확인을 받는다.
- `docs/private/` 를 링크하거나 개인 운영 메모, Secret, 로그 전문을 본문에 넣지 않는다.
