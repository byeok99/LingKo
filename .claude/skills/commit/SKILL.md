---
name: commit
description: LingKo 커밋 절차. 브랜치 확인, 기능 단위 staging 분리, 주석·문서·WORK_LOG·테스트 점검 후 저장소 커밋 메시지 규칙(type: 한국어 요약)으로 커밋한다. 사용자가 커밋을 요청할 때 또는 /commit 으로 호출한다.
---

# 커밋

사용자가 커밋을 요청할 때만 실행한다. push는 이 스킬의 범위가 아니다(`pr-create` 스킬).

## 1. 브랜치

1. `rtk git status -sb` 로 현재 브랜치와 변경 파일을 본다.
2. `develop` 또는 `main` 이면 커밋하지 않고 기능 브랜치를 먼저 만든다: `git switch -c <type>/<kebab-case-summary>`. type은 커밋 type과 같다.
3. 현재 브랜치 이름이 변경 내용과 맞지 않으면 사용자에게 알린다.

## 2. 범위 분리

- 한 커밋에는 **하나의 목적과 그 목적을 검증하는 테스트만** 넣는다.
- `git diff` / `git diff --staged` 를 보고 여러 목적이 섞였으면 커밋하지 말고 묶음별 파일 목록을 제안한다.
- 이번 작업과 무관한 변경·미추적 파일은 stage하지 않는다. `git add -A` 나 `git add .` 대신 파일을 명시한다.
- backend API, Flutter 화면, native 설정, 문서·운영 스크립트가 독립 검토 단위면 브랜치부터 나눌 것을 제안한다.

## 3. 커밋 전 점검

staged 파일을 기준으로 확인한다. 하나라도 빠지면 보완하거나 사용자에게 알린다.

- [ ] 새 public 선언, nullable·상태값, fallback 경로에 CLAUDE.md 「코드 주석 원칙」 주석이 있다.
- [ ] 기능·API·설정·데이터 모델이 바뀌었으면 관련 활성 문서도 함께 staged 되어 있다.
- [ ] `docs/private/` 가 있으면 대응하는 `WORK_LOG.md` 가 갱신됐다(Git에는 포함되지 않는다).
- [ ] 이미 적용된 Flyway migration 파일을 수정하지 않았다.
- [ ] Secret, token, `.env` 류가 staged 되지 않았다: `git diff --staged | grep -iE '^\+.*(secret|password|token|api[_-]?key)\s*[:=]\s*\S'` 결과를 확인한다. 값을 대입하는 줄만 잡으므로, 정책 문장 속 단어는 걸리지 않는다.
- [ ] 변경 범위에 맞는 최소 검증을 실행했다(CLAUDE.md 「리뷰와 검증」 명령). 실행하지 못했으면 이유를 커밋 보고에 남긴다.

## 4. 메시지

- 제목: `<type>: <한국어 요약>`, 50자 안팎, 마침표 없음.
  - type은 영어 소문자: `feat` `fix` `chore` `docs` `refactor` `ci` `ops` `perf` `test`
  - 요약은 한국어로 쓰고 명사형으로 끝낸다(…추가, …수정, …제거). 코드 식별자·파일명·기술 용어는 원문 그대로 쓴다.
  - 예: `fix: 오래된 평가 worker를 attempt로 차단하고 잠긴 작업 건너뛰기`
- 본문(선택): 한국어로, 무엇보다 **왜** 바꿨는지를 짧게. 관련 Issue가 있으면 `Refs #<n>`.
- 2026-11-06 이전 커밋은 영어 제목이다. 과거 커밋을 고치지 않는다.
- 마지막 줄: 하네스가 지정한 attribution 줄(Co-Authored-By)을 붙인다.

```bash
git commit -m "$(cat <<'EOF'
<type>: <summary>

<why>

Co-Authored-By: ...
EOF
)"
```

## 5. 보고

커밋 해시, 제목, 포함 파일, 실행한 검증과 결과, stage하지 않고 남긴 파일을 알려준다.

## 금지

- `--no-verify`, `--amend`(사용자 요청 없을 때), force 옵션을 쓰지 않는다.
- hook이 실패하면 우회하지 말고 원인을 고친 뒤 새 커밋으로 다시 만든다.
