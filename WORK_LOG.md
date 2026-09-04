## 2026-09-04 - Flutter 운영 API 기본 주소 문서화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: Xcode 직접 실행과 별도 설정 없는 Flutter 빌드가 DuckDNS 운영 HTTPS Backend를 기본 사용한다는 실행 계약을 루트 안내에 반영했다.
- 검증: `flutter analyze` 오류 0건, Flutter 전체 142개 테스트 통과(라인 80.35%), iOS simulator debug build 성공
- 리스크: 변경된 앱을 iPhone에 다시 설치한 뒤 실제 로그인을 확인해야 함

## 2026-08-24 - Spring 공통 설정의 정식 파일명 전환

- 변경 파일: `.gitignore`, `WORK_LOG.md`
- 내용: 실제 실행에 쓰이는 Backend 공통 설정만 `src/main/resources/application.yaml` 경로에서 추적하고, 이전 Backend 로컬 설정 백업은 Git에서 제외하도록 규칙을 조정했다.
- 검증: 설정 추적 가능 여부, 비밀값 placeholder 검사, Backend 전체 테스트
- 리스크: 실제 Secret은 계속 저장소 밖에서 주입해야 함

## 2026-08-12 - iOS Apple 로그인 구현 범위 안내

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: iOS Apple 로그인 구현 상태와 Android·Web 제외 범위를 루트 안내에 동기화했다.
- 검증: 앱·Backend 전체 테스트와 활성 문서 대조
- 리스크: Apple Developer capability와 실기기 E2E, authorization code revocation은 출시 전 필요

## 2026-08-04 - 단어 점수 저장 조건 정리와 상태값 타입화

- 변경 파일: backend 평가 DTO·service·test, app models·widgets·screens·test, `docs/api/api-reference.md`
- 내용: 리뷰에서 찾은 두 문제를 순서대로 처리했다. (1) 점수를 신뢰할 수 없을 때 문장 전체를 한 단어로 저장하던 경로를 없애 `word_text` 길이 초과를 막고 평가 작업 text 상한을 준비 endpoint와 같은 100자로 맞췄다. (2) 26곳에 흩어져 있던 `AVAILABLE`/`UNAVAILABLE` 문자열을 backend `ScoreStatus`·`GuideStatus` enum과 app `ScoreStatus` enum으로 승격하고, 앱은 모르는 값을 unavailable로 닫는 fail-closed 규칙을 적용했다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과. Jackson 기본 직렬화로 JSON 계약이 그대로임을 회귀 테스트로 고정
- 리스크: 저장된 단어 행이 없는 기록은 조회 시 `standard_pronunciation` 기준 복원에 의존한다. 서버가 상태값을 추가하면 구버전 앱은 해당 점수를 감춘 채 동작한다

## 2026-08-04 - 주석 자동 작성 규칙 명문화

- 변경 파일: `AGENTS.md`, `CLAUDE.md`, `WORK_LOG.md`
- 내용: 「코드 주석 원칙」에 '적용 시점' 절을 추가해 주석을 코드 작성과 같은 편집에서 넣는 기본 동작으로 규정하고, nullable·상태값·fallback·계층 경계를 주석 없이 통과시키지 않도록 했다. `CLAUDE.md`에는 Write/Edit 호출에 주석을 포함하고 완료 보고 전 자체 확인하라는 Claude 전용 실행 규칙을 넣었다.
- 검증: 규칙에 따라 이번 작업의 주석 보강을 실제로 수행하고 `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 지침 기반 규칙이므로 강제되지 않는다. 강제하려면 별도 hook이 필요함

## 2026-08-04 - Claude Code 운영 지침 추가

- 변경 파일: `CLAUDE.md`, `WORK_LOG.md`
- 내용: 기존 `AGENTS.md`를 정책 단일 기준으로 유지한 채, Claude Code 세션에서만 달라지는 운영 규칙(subagent 사용 제한, 전용 파일 도구 우선, rtk prefix)과 `AGENTS.md`에 없던 프로젝트 지형도·검증 명령·도메인 규칙을 담은 `CLAUDE.md`를 추가했다.
- 검증: `rtk git status`로 rtk 동작 확인, 기재한 디렉터리 구조와 Gradle `integrationTest` sourceSet 존재 확인, Claude Code 2.1.221 바이너리에서 메모리 탐색 대상이 `CLAUDE.md`·`CLAUDE.local.md`·`.claude/rules/`뿐이고 `AGENTS.md`는 Codex 임포터 경로에만 있음을 확인
- 리스크: `AGENTS.md`는 자동 로드되지 않아 `CLAUDE.md`의 `@AGENTS.md` import에 의존한다. 파일명을 바꾸면 정책 본문이 통째로 빠지므로 함께 갱신해야 함

## 2026-08-03 - 단일 Codex 세션 운영 기준으로 전환

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: pane 1/2/3 역할 분담을 제거하고 요구사항 정리부터 명령 실행, 검증, 완료 보고까지 현재 Codex 세션의 하나의 작업 흐름에서 처리하도록 운영 지침을 갱신했다.
- 검증: pane·tmux 관련 문구 검색, `git diff --check`
- 리스크: 사용자 환경에서만 가능한 장기 실행·수동 검증은 Codex가 안내한 명령을 사용자가 실행해야 할 수 있음

## 2026-07-24 - 트러블슈팅 문서화 정책과 AGENTS 추적 보장

- 변경 파일: `AGENTS.md`, `.gitignore`, `WORK_LOG.md`
- 내용: 재발·영향·학습 가치 기반의 선별적 트러블슈팅 규칙과 작업 절차를 추가하고 `AGENTS.md` ignore 규칙을 제거해 팀 공유 상태를 명시적으로 보장했다.
- 검증: Git 추적 상태, ignore 규칙 제거, 문서화 판단·제외·완료 조건 반영 여부 확인
- 리스크: 없음

## 2026-07-24 - 코드와 문서 동기화 규칙 추가

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: 코드 변경 전후에 관련 활성 문서를 확인하고 README, API 계약, 요구사항, roadmap, technical debt, 설정·운영 가이드를 같은 작업에서 갱신하도록 규칙을 추가했다. 문서 불일치가 남으면 Issue를 완료 처리하지 않도록 완료 조건도 명시했다.
- 검증: `git diff --check`, 문서 동기화 관련 규칙과 구현 체크리스트 반영 여부 확인
- 리스크: 기존 활성 문서에 이미 남아 있는 과거 상태 문구는 기능별 후속 정합화 작업에서 정리해야 함

## 2026-07-24 - 한국어 의도 중심 코드 주석 규칙 추가

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: Backend와 App의 새·변경 코드에 의도, 의미, 이유, 구현 선택을 설명하는 한국어 Javadoc·Dartdoc·결정 주석을 작성하도록 규칙을 추가하고 중복 주석과 Flyway checksum 변경을 금지했다.
- 검증: Flutter 정적 분석·49개 테스트 통과, Java 21 Backend 단위·통합 테스트 통과
- 리스크: 이후 구현 변경 시 코드와 주석을 같은 작업에서 함께 갱신해야 함

## 2026-07-23 - Backend Java 21 기준 반영

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 프로젝트 기술 스택의 Backend Java 기준을 17에서 21로 갱신했다.
- 검증: 활성 문서의 Java 17 참조 검색, Java 21 환경의 Backend 테스트와 Docker 빌드 통과
- 리스크: 로컬 개발 환경에는 JDK 21을 별도로 설치해야 함

## 2026-07-21 - 최신 문서 체계와 작업 로그 정책 정합화

- 변경 파일: `.gitignore`, `WORK_LOG.md`
- 내용: 작업 로그 정책을 최신 `develop`에 재배치하면서 표준 프로젝트 문서인 `docs/`는 계속 Git으로 추적하고, 로컬 앱 참고 문서에서는 `app/docs/WORK_LOG.md`만 추적하도록 ignore 규칙을 정리했다.
- 검증: `git diff --check`, 표준 `docs/` 추적 상태 확인
- 리스크: `app/docs/`의 과거 참고 문서는 별도 보존 문서 커밋에서 분류해야 함

## 2026-07-20 - 최소 경로별 작업 이력 파일 초기화

- 변경 파일: `AGENTS.md`, `.gitignore`, 프로젝트 각 관리 디렉터리의 `WORK_LOG.md`
- 내용: Git 관리 파일 또는 현재 작업 파일이 직접 위치한 최소 디렉터리마다 작업 이력 파일을 미리 두도록 규칙을 강화했다. 생성물, 캐시, 의존성 및 빌드 도구 전용 번들은 제외하고 가장 가까운 안전한 상위 폴더에서 이력을 관리하도록 정했다. 기존에 무시되던 문서 폴더에서도 `WORK_LOG.md`만 Git으로 추적할 수 있게 예외를 추가했다.
- 검증: 대상 디렉터리와 `WORK_LOG.md` 존재 여부를 비교하고 `git diff --check`로 문서 형식을 확인할 예정
- 리스크: 새 디렉터리 추가 시 같은 작업에서 해당 디렉터리의 `WORK_LOG.md`도 함께 생성해야 함

## 2026-07-20 - 프로젝트 전체 작업 이력 적용 범위 명확화

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: 폴더별 작업 이력 규칙이 `app/`에 한정되지 않고 `backend/`, `docs/`, `.codex/`, `.agents/`, 루트 설정 파일을 포함한 저장소 전체에 적용됨을 명시했다. 예시도 앱 중심에서 프로젝트 전체 범위 예시로 교체했다.
- 검증: 문서 변경만 수행해 별도 빌드/테스트는 실행하지 않음
- 리스크: 기존에 상위 폴더에 모아둔 이력은 필요 시 후속으로 각 직접 폴더별 이력으로 재분배할 수 있음

## 2026-07-20 - 직접 폴더별 작업 이력 분리 규칙 보강

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: 수정 파일이 위치한 각 직접 폴더마다 별도의 `WORK_LOG.md`를 작성하도록 규칙을 명확히 했다. 상위 `WORK_LOG.md`가 하위 폴더 상세 변경을 대신하지 않는다는 기준과 예시를 추가했다.
- 검증: 문서 변경만 수행해 별도 빌드/테스트는 실행하지 않음
- 리스크: 기존에 `app/WORK_LOG.md`에 모아둔 상세 이력은 필요 시 후속으로 하위 폴더별 이력으로 재분배할 수 있음

## 2026-07-20 - 작업 이력 확인 규칙 보강

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: 작업 시작 시 수정 대상 후보 폴더의 `WORK_LOG.md`를 확인하고, 필요한 경우 가까운 상위 이력도 확인하도록 규칙을 보강했다.
- 검증: 문서 변경만 수행해 별도 빌드/테스트는 실행하지 않음
- 리스크: 없음

## 2026-07-20 - 폴더별 작업 이력 규칙 추가

- 변경 파일: `AGENTS.md`, `WORK_LOG.md`
- 내용: 수정이 발생한 각 폴더에 `WORK_LOG.md`를 남기도록 프로젝트 작업 지침을 추가했다. 루트 지침 변경 이력을 기록하기 위해 루트 작업 이력 파일을 생성했다.
- 검증: 문서 변경만 수행해 별도 빌드/테스트는 실행하지 않음
- 리스크: 기존 하위 폴더에는 아직 `WORK_LOG.md`가 없으며, 다음 실제 수정 시 해당 폴더별로 생성해야 함
