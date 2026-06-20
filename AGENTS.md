# 프로젝트 작업 지침

## 프로젝트

Spring 백엔드 + Flutter 앱 프로젝트.

## 역할

tmux 기본 운영은 3-pane 구조를 따른다.

* main: 사용자 승인, 요구사항 정리, 작업 분해, 범위 통제, 결과 통합
* developer: Spring 백엔드와 Flutter 앱 구현, 테스트, 수정 결과 보고
* review-qa: 코드 리뷰, 테스트 누락, 보안 위험, 회귀 위험 검토

세부 전문성은 developer 내부에서 작업 범위에 따라 나눈다.

* backend 관점: Spring API, DB, 인증/인가, 비즈니스 로직, 테스트
* app 관점: Flutter 화면, 상태관리, API 연동
* architect 관점: API 계약, DB, 인증 흐름, 구조 검토

## 승인 흐름

사용자 승인 요청은 main만 수행한다.

main은 제품 코드를 직접 생성, 수정, 삭제하지 않는다. 구현이 필요한 작업은 반드시 developer에게 전달한다.

developer와 review-qa는 main이 전달한 작업에 `USER_APPROVED: true`가 있을 때만 파일 생성, 수정, 삭제를 수행한다.

승인 범위를 벗어나는 변경이 필요하면 사용자에게 직접 묻지 말고 main에게 보고한다.

승인된 작업 전달 형식:

```text
USER_APPROVED: true
APPROVED_SCOPE:
- 목적:
- 변경 허용 파일:
- 변경 금지 파일:
- 허용 작업:
- 검증 방법:
```

`USER_APPROVED: true`가 없으면 조사, 분석, 계획 수립까지만 수행한다.

## 작업 원칙

* 수정 전 실제 디렉터리 구조와 기존 스타일을 확인한다.
* 백엔드와 앱이 함께 쓰는 기능은 API 계약을 먼저 맞춘다.
* API Key, 비밀번호, 토큰은 코드에 하드코딩하지 않는다.
* Entity를 API 응답으로 직접 노출하지 않는다.
* Flutter는 화면, 상태관리, API client를 가능한 한 분리한다.
* 테스트 또는 실행 확인 없이 완료했다고 단정하지 않는다.
* 테스트를 못 하면 이유와 수동 검증 방법을 보고한다.

## 브랜치와 커밋 원칙

브랜치와 PR은 phase 단위가 아니라 기능 단위로 나눈다.

* 하나의 브랜치는 독립적으로 리뷰, 테스트, 배포 판단이 가능한 기능이나 운영 변경 하나만 포함한다.
* backend API, Flutter 화면/API 연동, native 설정, 문서/운영 스크립트는 서로 다른 검토 단위이면 별도 브랜치로 분리한다.
* 여러 기능이 순서 의존성을 가지면 stacked PR로 만들고, 각 PR의 base branch를 명확히 둔다.
* phase 번호는 작업 추적용일 뿐 브랜치명, PR 범위, 커밋 범위의 기준으로 쓰지 않는다.
* 커밋은 작은 논리 단위로 만든다. 한 커밋에는 하나의 목적과 그 목적을 검증하는 테스트만 포함한다.
* 커밋 전 staged diff가 여러 기능을 섞고 있으면 커밋하지 말고 기능별로 분리한다.
* 대규모 변경을 push하거나 PR 생성하기 전에 main은 변경 파일 목록을 확인하고 기능 단위 분리가 필요한지 사용자에게 보고한다.
* PR 본문에는 포함 기능, 제외한 후속 기능, 검증 결과, stacked PR 의존 관계를 명시한다.

## 참고 파일

필요할 때만 아래 파일을 참고한다.

* 역할: `.codex/agents/*.md` 또는 `~/.codex/agents/*.md`
* 공통 규칙: `.codex/rules/*.md` 또는 `~/.codex/rules/*.md`
* 작업 절차: `.codex/skills/*.md` 또는 `~/.codex/skills/*.md`

예:

* API 설계: `api-design.md`
* Spring 기능 구현: `springboot-feature.md`
* Flutter API 연동: `flutter-api-integration.md`
* 검증 루프: `verification-loop.md`

## 참고 범위 제한

에이전트는 자기 역할과 현재 작업에 필요한 파일만 확인한다.

* 공통: 프로젝트 `AGENTS.md`와 자신의 agent 파일만 기본으로 읽는다.
* main: `.codex/agents/main.md`를 읽고, 작업 분배에 필요한 범위만 추가로 확인한다.
* developer: `.codex/agents/developer.md`를 읽고, Spring/Flutter 구현에 필요한 rule/skill만 확인한다.
* review-qa: `.codex/agents/review-qa.md`를 읽고, 리뷰, QA, 테스트, 회귀 위험 검토에 필요한 rule/skill만 확인한다.

rules/skills 디렉터리는 필요한 파일명을 찾기 위한 목록 조회만 허용한다. 파일 내용은 현재 작업과 직접 관련이 있을 때만 읽는다.

다른 역할의 agent 파일이나 관련 없는 rule/skill 파일은 임의로 읽지 않는다. 필요하면 main에게 먼저 이유를 보고하고 범위를 확인받는다.

## 역할별 참고 rule/skill

아래 파일은 역할별 우선 참고 후보다. 실제 파일 내용은 현재 작업과 직접 관련이 있을 때만 읽는다.

* main
  * rule: `common.md`, `api-contract.md`, `security.md`, `testing.md`
  * skill: `api-design.md`, `verification-loop.md`
  * 조건부: Spring 작업 분배 시 `springboot.md`, `springboot-feature.md`; Flutter 작업 분배 시 `flutter.md`, `flutter-api-integration.md`
* developer
  * rule: `common.md`, `api-contract.md`, `security.md`, `springboot.md`, `flutter.md`
  * skill: `springboot-feature.md`, `flutter-api-integration.md`
  * 조건부: API 계약 설계가 필요하면 `api-design.md`; TDD가 필요한 변경이면 `tdd-workflow.md`; 검증 루프가 필요하면 `verification-loop.md`
* review-qa
  * rule: `common.md`, `testing.md`, `api-contract.md`, `security.md`
  * skill: `verification-loop.md`
  * 조건부: 백엔드 검토 시 `springboot.md`, `springboot-feature.md`; 앱 검토 시 `flutter.md`, `flutter-api-integration.md`; 테스트 설계가 필요하면 `tdd-workflow.md`

## 완료 보고

작업 완료 시 아래만 보고한다.

1. 작업 요약
2. 변경 파일
3. 검증 결과
4. 남은 리스크
5. 다음 액션
