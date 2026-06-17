# 프로젝트 작업 지침

## 프로젝트

Spring 백엔드 + Flutter 앱 프로젝트.

## 역할

* main: 사용자 승인, 작업 분해, 역할 분배, 결과 통합
* architect: API 계약, DB, 인증 흐름, 구조 검토
* backend: Spring API, DB, 인증/인가, 비즈니스 로직, 테스트
* app: Flutter 화면, 상태관리, API 연동
* review-qa: 코드 리뷰, 테스트, 회귀 위험 검토

## 승인 흐름

사용자 승인 요청은 main만 수행한다.

서브 역할은 main이 전달한 작업에 `USER_APPROVED: true`가 있을 때만 파일 생성, 수정, 삭제를 수행한다.

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
* main: `main.md`를 읽고, 작업 분배에 필요한 역할 파일만 추가로 확인한다.
* architect: `architect.md`를 읽고, API 계약, DB, 인증 흐름, 구조 검토에 필요한 rule/skill만 확인한다.
* backend: `backend.md`를 읽고, Spring, DB, 인증/인가, 테스트에 필요한 rule/skill만 확인한다.
* app: `app.md`를 읽고, Flutter, 상태관리, API 연동에 필요한 rule/skill만 확인한다.
* review-qa: `review-qa.md`를 읽고, 리뷰, QA, 테스트, 회귀 위험 검토에 필요한 rule/skill만 확인한다.

rules/skills 디렉터리는 필요한 파일명을 찾기 위한 목록 조회만 허용한다. 파일 내용은 현재 작업과 직접 관련이 있을 때만 읽는다.

다른 역할의 agent 파일이나 관련 없는 rule/skill 파일은 임의로 읽지 않는다. 필요하면 main에게 먼저 이유를 보고하고 범위를 확인받는다.

## 역할별 참고 rule/skill

아래 파일은 역할별 우선 참고 후보다. 실제 파일 내용은 현재 작업과 직접 관련이 있을 때만 읽는다.

* main
  * rule: `common.md`, `api-contract.md`, `security.md`, `testing.md`
  * skill: `api-design.md`, `verification-loop.md`
  * 조건부: Spring 작업 분배 시 `springboot.md`, `springboot-feature.md`; Flutter 작업 분배 시 `flutter.md`, `flutter-api-integration.md`
* architect
  * rule: `common.md`, `api-contract.md`, `security.md`, `springboot.md`, `flutter.md`
  * skill: `api-design.md`
  * 조건부: 검증 계획이 필요하면 `testing.md`, `verification-loop.md`
* backend
  * rule: `common.md`, `springboot.md`, `api-contract.md`, `security.md`, `testing.md`
  * skill: `springboot-feature.md`
  * 조건부: TDD가 필요한 변경이면 `tdd-workflow.md`; 검증 루프가 필요하면 `verification-loop.md`
* app
  * rule: `common.md`, `flutter.md`, `api-contract.md`, `security.md`, `testing.md`
  * skill: `flutter-api-integration.md`
  * 조건부: 검증 루프가 필요하면 `verification-loop.md`; TDD가 필요한 변경이면 `tdd-workflow.md`
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
