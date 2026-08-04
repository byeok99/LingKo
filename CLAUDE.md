# Claude Code 작업 지침

작업 정책의 단일 기준은 `AGENTS.md`이며, 아래 한 줄로 이 파일에 함께 로드된다.

@AGENTS.md

## 이 파일의 역할

폴더별 `WORK_LOG.md`, 코드-문서 동기화, 트러블슈팅 문서화, 주석 원칙, 브랜치·커밋 원칙, 완료 보고 형식은 위에서 import한 `AGENTS.md`를 그대로 따르며 이 파일에 다시 적지 않는다.

이 파일은 두 가지만 담는다.

1. Claude Code 세션에서만 달라지는 운영 규칙
2. `AGENTS.md`에 없는 프로젝트 지형도와 자주 틀리는 도메인 규칙

공통 정책이 바뀌면 `AGENTS.md`를 고친다. Claude 전용 동작만 이 파일에 남긴다. 두 파일을 모두 수정하면 루트 `WORK_LOG.md`에 기록한다.

## Claude Code 전용 운영 규칙

- `AGENTS.md`의 "Codex 세션"은 **현재 Claude Code 세션**으로 읽는다. 요구사항 정리부터 구현, 명령 실행, 자체 리뷰, 검증, 요약까지 이 세션에서 연속 처리한다.
- **subagent(Agent 도구)는 사용자가 명시적으로 요청한 경우에만 사용한다.** `Explore`, `general-purpose`, `Plan` 을 자동으로 띄우지 않는다. 탐색이 넓어 보여도 이 세션의 Grep/Glob/Read 로 직접 처리한다. `AGENTS.md`의 "별도 AI 세션에 위임하지 않는다" 규칙과 같은 취지다.
- 파일 조회·수정은 Read/Edit/Write/Grep/Glob 도구를 쓴다. `cat`, `head`, `sed`, `awk` 로 파일을 읽지 않는다.
- shell 명령은 `rtk` prefix로 실행한다. 예: `rtk git status`, `rtk ./gradlew test`. raw 출력이 필요하면 `rtk proxy <cmd>`. Read/Grep 등 전용 도구가 있는 작업에는 쓰지 않는다.
- 수정 대상 폴더가 정해지면 **파일을 고치기 전에** 그 폴더의 `WORK_LOG.md`를 먼저 읽는다.
- 완료 보고는 `AGENTS.md`의 5항목(작업 요약 / 변경 파일 / 검증 결과 / 남은 리스크 / 다음 액션)을 유지한다.
- **Write/Edit로 코드를 만들 때 의도 주석을 같은 호출에 포함한다.** `AGENTS.md`의 「코드 주석 원칙 > 적용 시점」이 기본 동작이며, 주석 없는 코드를 먼저 쓰고 뒤이은 편집으로 채우지 않는다. 사용자가 주석을 따로 요청하지 않아도 마찬가지다.
- 코드를 만든 뒤 완료 보고 전에 이번 turn의 Write/Edit 대상을 훑어, 새 public 선언과 nullable·상태값·fallback 경로에 주석이 붙었는지 확인한다.
- 커밋과 push는 사용자가 요청할 때만 한다. `develop` 이 기본 브랜치이므로 기능 작업은 브랜치를 먼저 만든다.

## 프로젝트 지형도

한국어 발음 학습 서비스. Flutter 앱 + Spring Boot API + 외부 음성·미디어 연동.

```
app/          Flutter 클라이언트 (Dart, Material 3)
  lib/api/        서버 REST client (auth, evaluation, sentence, quota, preferences)
  lib/app/        앱 shell(lingko_app.dart)과 디자인 토큰(app_theme.dart)
  lib/models/     API 응답 모델
  lib/screens/    Home / Practice / Result / Review / Profile / AuthGate
  lib/services/   기기 경계(녹음, TTS, Google 로그인, secure storage 세션)
  lib/widgets/    화면 간 공용 위젯
  test/           widget · API 테스트

backend/      Spring Boot 3.4 / Java 21 / MySQL 8 / Flyway
  src/main/java/com/lingko/lingko/
    api/          Controller + 요청·응답 DTO (auth, evaluation, quota, sentence, user)
    core/         domain(entity·repository·service), config, util
    infra/        외부 연동 경계 (auth, pronunciation=Azure Speech, storage=S3)
  src/main/resources/db/migration/   Flyway V*.sql
  src/test/                          단위 테스트
  src/integrationTest/               통합 테스트 (별도 sourceSet)

docs/         api / architecture(+adr) / data / development / operations /
              overview / performance / requirements / roadmap / security /
              troubleshooting / design, technical-debt.md
```

계층 경계: `api` 는 DTO 변환과 검증, `core.domain` 은 업무 규칙과 트랜잭션, `infra` 는 외부 서비스 호출. Entity는 `api` 응답으로 직접 나가지 않는다.

외부 의존: Azure Speech(발음 평가), Replicate(가이드 영상 생성), AWS S3(음성 업로드), FFmpeg(영상 병합), MySQL.

## 검증 명령

변경 범위에 맞게 최소로 실행한다.

```bash
cd backend && rtk ./gradlew compileJava      # 백엔드 컴파일
cd backend && rtk ./gradlew test             # 백엔드 단위 테스트
cd backend && rtk ./gradlew integrationTest  # 백엔드 통합 테스트
cd app && rtk flutter analyze                # 앱 정적 분석
cd app && rtk flutter test                   # 앱 테스트
```

- `./gradlew externalIntegrationTest` 는 실제 외부 서비스 자격증명이 필요하므로 사용자 확인 없이 실행하지 않는다.
- 로컬 실행(`docker compose up --build`, `./gradlew bootRun`, `flutter run`)은 장기 실행 프로세스다. 필요하면 명령과 확인 지점을 사용자에게 안내한다.
- 상세 절차는 `docs/development/local-development.md`, `docs/development/testing-and-troubleshooting.md`.

## 자주 틀리는 도메인 규칙

구현·리뷰 전에 확인한다. 값이 바뀌면 `docs/overview/product-and-scope.md` 와 `docs/api/api-reference.md` 를 같은 작업에서 갱신한다.

- 평가 기회는 최대 5회 보유. 5회 미만이면 서버 기준 **1시간마다 1회 충전**되며 날짜 변경으로 초기화하지 않는다.
- 보상 횟수(광고)는 자연 충전 상한과 별개다. 현재 광고 SDK·서버 보상 검증은 미연결이다.
- 음성은 WAV만 허용. 16-bit mono PCM, 샘플링 레이트 48kHz 이하, 최대 10MiB.
- 추천 문장 조회 개수와 연습 기록 페이지 크기는 각각 1~50.
- 평가는 업로드 티켓 발급 → S3 PUT → Idempotency 키로 작업 생성 → 상태 폴링 순서의 비동기 흐름이다.
- 평가 종료 즉시 원본 음성을 삭제한다. 미제출 객체는 1일 Lifecycle 정책으로 정리된다.
- 이미 적용된 Flyway migration 파일은 수정하지 않는다. 스키마 변경은 새 `V<n>__*.sql` 로 추가한다.
- 표준 발음은 한국어 음운 규칙을 적용한 평가 기준 문자열이다. 과거 기록의 snapshot을 재연습에 재사용하지 않고 현재 규칙으로 다시 준비한다.

## 참고 파일

필요할 때만 읽는다.

- 작업 정책 본문: `AGENTS.md`
- 트러블슈팅 템플릿과 인덱스: `docs/troubleshooting/README.md`
- API 계약: `docs/api/api-reference.md`, `docs/api/error-codes.md`
- 아키텍처 결정: `docs/architecture/adr/README.md`
- dmux 훅을 수정할 때만: `.dmux-hooks/AGENTS.md`