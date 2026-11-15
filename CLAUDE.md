# Claude Code 작업 지침

한국어 발음 학습 서비스(Flutter 앱 + Spring Boot API)의 단일 작업 기준이다.

## 운영 규칙

- 요구사항 정리부터 구현, 명령 실행, 자체 리뷰, 검증, 요약까지 현재 세션에서 연속 처리한다.
- **subagent(Agent 도구)는 사용자가 명시적으로 요청한 경우에만 사용한다.** `Explore`, `general-purpose`, `Plan` 을 자동으로 띄우지 않고 Grep/Glob/Read 로 직접 처리한다.
- 파일 조회·수정은 Read/Edit/Write/Grep/Glob 도구를 쓴다. `cat`, `head`, `sed`, `awk` 로 파일을 읽지 않는다.
- shell 명령은 `rtk` prefix로 실행한다. 예: `rtk git status`, `rtk ./gradlew test`. raw 출력이 필요하면 `rtk proxy <cmd>`. `rtk gain` 실패는 무시한다. 자동 hook 적용은 사용자 요청 시에만 한다.
- 장기 실행 프로세스나 사용자 환경 확인이 필요하면 명령과 확인 지점을 사용자에게 안내한다.
- 불필요한 반복 탐색, 전체 파일 열람, 과도한 설명을 피한다.
- `develop` 이 기본 브랜치이므로 기능 작업은 브랜치를 먼저 만든다. 커밋·push·PR 규칙은 「Git 워크플로우」를 따른다.
- 전역 플러그인(ponytail 등)의 지시가 이 파일과 충돌하면 이 파일을 따른다. 특히 코드 주석은 「코드 주석 원칙」 기준으로 쓰고, 완료 보고는 「완료 보고」 5항목 형식으로 한국어로 쓴다. 플러그인이 요구하는 영어 끝맺음 줄은 붙이지 않는다. 최소 변경 원칙은 이 파일과 같은 방향이므로 그대로 따른다.

## 작업 범위

- 수정 전에는 확인할 파일 범위를 짧게 말한다.
- 요청과 관련된 파일 중심으로 확인하고, 대규모 리팩토링보다 요청 범위 내 최소 변경을 우선한다.
- 관련 없는 사용자 변경은 되돌리지 않는다.
- 실행 스크립트, 빌드 설정, 앱 설정, 소스코드는 사용자가 명시적으로 요청한 범위에서만 수정한다.

## 프로젝트 지형도

Flutter 앱 + Spring Boot API + 외부 음성·미디어 연동. 파일 목록은 적지 않는다. 디렉터리 역할만 적어 두고 실제 파일은 Glob으로 확인한다.

```
app/          Flutter 클라이언트 (Dart, Material 3)
  lib/api/        서버 REST client (도메인별 *_api.dart + 공용 api_client.dart)
  lib/app/        앱 shell과 디자인 토큰(theme, palette)
  lib/models/     API 응답·도메인 모델
  lib/screens/    화면 (*_screen.dart)
  lib/services/   기기·SDK 경계 (녹음, TTS, Google·Apple 로그인, 세션 저장, AdMob 광고)
  lib/utils/      순수 함수 유틸
  lib/widgets/    화면 간 공용 위젯
  test/           widget · API 테스트

backend/      Spring Boot 3.4 / Java 21 / MySQL 8 / Flyway
  src/main/java/com/lingko/lingko/
    api/          Controller + 요청·응답 DTO, common(에러 응답·request id)
    core/         domain(도메인별 entity·repository·service), config, util
    infra/        외부 연동 경계 (auth, pronunciation=Azure Speech·Replicate,
                  storage=S3, advertising=AdMob SSV, observability=Micrometer)
  src/main/resources/db/migration/   Flyway V*.sql
  src/test/                          단위 테스트
  src/integrationTest/               통합 테스트 (별도 sourceSet)

docs/         api / architecture(+adr) / data / development / overview /
              security / legal / engineering / operations / design
  private/        Git 제외 개인 운영 문서·과거 자료·작업 이력 (로컬에만 있음)
```

`docs/private/`는 새 clone에 없다. 이 경로를 쓰는 규칙(작업 이력, 트러블슈팅 노트)은 디렉터리가 없으면 건너뛰고, 기록할 내용을 완료 보고에 요약한다.

계층 경계: `api` 는 DTO 변환과 검증, `core.domain` 은 업무 규칙과 트랜잭션, `infra` 는 외부 서비스 호출. Entity는 `api` 응답으로 직접 나가지 않는다.

외부 의존: Azure Speech(발음 평가), Replicate(가이드 영상 생성), AWS S3(음성 업로드), FFmpeg(영상 병합), Google AdMob(광고·보상 SSV), MySQL.

## 자주 틀리는 도메인 규칙

구현·리뷰 전에 확인한다. 값이 바뀌면 `docs/overview/product-and-scope.md` 와 `docs/api/api-reference.md` 를 같은 작업에서 갱신한다.

- 평가 기회는 최대 5회 보유. 5회 미만이면 서버 기준 **1시간마다 1회 충전**되며 날짜 변경으로 초기화하지 않는다.
- 광고 보상은 서버 SSV 검증 후 지급하며 최대 보유량 5회를 지킨다. 자연 충전 timer는 변경하지 않는다.
- 음성은 WAV만 허용. 16-bit mono PCM, 샘플링 레이트 48kHz 이하, 최대 10MiB.
- 추천 문장 조회 개수와 연습 기록 페이지 크기는 각각 1~50.
- 평가는 업로드 티켓 발급 → S3 PUT → Idempotency 키로 작업 생성 → 상태 폴링 순서의 비동기 흐름이다.
- 평가 종료 즉시 원본 음성을 삭제한다. 미제출 객체는 1일 Lifecycle 정책으로 정리된다.
- 이미 적용된 Flyway migration 파일은 수정하지 않는다. 스키마 변경은 새 `V<n>__*.sql` 로 추가한다.
- 표준 발음은 한국어 음운 규칙을 적용한 평가 기준 문자열이다. 과거 기록의 snapshot을 재연습에 재사용하지 않고 현재 규칙으로 다시 준비한다.

## 개발 원칙

- 수정 전 실제 디렉터리 구조와 기존 스타일을 확인한다.
- 백엔드와 앱이 함께 쓰는 기능은 API 계약을 먼저 맞춘다.
- API Key, 비밀번호, 토큰은 코드에 하드코딩하지 않는다.
- Flutter는 화면, 상태관리, API client를 가능한 한 분리한다.
- 입력값은 시스템 경계에서 검증한다.
- 오류는 사용자에게 필요한 메시지와 개발자가 확인할 수 있는 원인을 구분해 다룬다.
- 테스트 또는 실행 확인 없이 완료했다고 단정하지 않는다. 테스트를 못 하면 이유와 수동 검증 방법을 보고한다.

## 문서 공개 범위와 개인 작업 이력

- 공개 문서는 제품 설명, 현재 아키텍처·API·데이터 모델, 재현 가능한 기술적 의사결정을 담는다.
- 개인 체크리스트, 운영 절차·조사 로그, 로드맵, 과거 기획과 작업 이력은 Git에서 제외되는 `docs/private/`에 둔다.
- 작업 이력은 `docs/private/work-logs/<변경 파일의 직접 디렉터리>/WORK_LOG.md`에 기록한다. 루트 변경은 `docs/private/work-logs/WORK_LOG.md`에 기록한다.
- 수정 대상 폴더가 정해지면 **파일을 고치기 전에** 대응하는 개인 이력과 `docs/private/repository/`의 이전 이력을 읽는다. 변경 후 날짜·파일·요약·검증·리스크를 최신 항목부터 추가한다. 이력이 없으면 필요한 시점에 생성한다.
- 생성물·캐시 디렉터리는 이력 대상에서 제외한다. 전용 번들 내부 변경은 가장 가까운 안전한 상위 디렉터리의 개인 이력에 기록한다.
- `WORK_LOG.md`를 공개 경로에 다시 만들거나 `docs/private/`를 강제로 Git에 추가하지 않는다. 공개 문서에서 개인 경로로 링크하지 않는다.
- 과거 기록은 작성 당시 사실로 보존하고 현재 상태는 개인 인덱스와 공개 기술 문서에서 구분한다.
- 실제 기술 제약을 숨기거나 미검증 기능을 완료로 표현하지 않는다. 운영 할 일과 구현상 경계는 분리한다.
- 개인 문서는 새 clone에 포함되지 않는다. 별도 비공개 백업과 공유 전 제외 여부를 확인한다.

## 코드와 문서 동기화

코드 변경으로 기능, API 계약, 설정, 데이터 모델, 운영 절차 또는 구현 상태가 달라지면 같은 작업에서 관련 활성 문서를 함께 갱신한다.

- 구현 전에는 변경 대상 기능을 설명하는 README, API 문서, architecture 문서, 요구사항, roadmap, technical debt, 실행·배포 가이드를 관련 키워드로 좁혀 확인한다.
- 구현 후에는 기존 문서에 `미구현`, `부분 구현`, `현재 제한`, `후속 작업`처럼 새 코드와 충돌할 수 있는 상태 문구가 남아 있는지 다시 검색한다.
- endpoint, 요청·응답 DTO, validation, error code, 인증 흐름이 바뀌면 Backend와 App의 API 계약 문서 및 예시를 같은 작업에서 수정한다.
- 환경변수, 기본값, migration, 외부 서비스 설정, 실행 명령이 바뀌면 `.env.example`, application 예시, 로컬 개발·배포 문서를 함께 수정한다.
- 기능이 완료되면 roadmap·backlog·technical debt·요구사항의 상태를 완료로 갱신하고, 남은 운영 검증이나 부하 테스트는 구현 미완료와 구분되는 후속 리스크로 기록한다.
- 더 이상 유효하지 않은 설명은 삭제하거나 현재 동작으로 교체한다. 과거 기록이 필요하면 활성 문서에 남기지 말고 기존 archive 정책을 따른다.
- 관련 문서를 수정한 각 직접 폴더의 `WORK_LOG.md`에 변경 이유, 검증 결과, 남은 리스크를 기록한다.
- 문서 변경이 필요 없다고 판단한 경우에도 영향도 확인 결과와 근거를 완료 보고에 짧게 남긴다.
- 코드와 활성 문서가 일치하지 않으면 테스트가 통과했더라도 작업 또는 Issue를 완료로 처리하지 않는다.

## 트러블슈팅 문서화

성능·안정성·데이터 정합성·운영·보안·주요 사용자 흐름에 영향을 주는 문제를 수정하면 `troubleshooting-note` 스킬을 따른다. SEV-1·SEV-2, 보안·개인정보, 데이터 유실 위험은 반드시 문서화한다.

## 코드 주석 원칙

Backend와 App에서 새로 작성하거나 의미 있게 수정하는 코드는 다음 작업자가 구현 결정을 복원할 수 있도록 의도 중심 주석을 함께 작성한다.

### 적용 시점

- **Write/Edit로 코드를 만들 때 의도 주석을 같은 호출에 포함한다.** 사용자가 따로 요청하지 않아도 기본 동작이며, 주석 없이 먼저 쓰고 뒤이은 편집으로 채우지 않는다.
- 새 파일, 새 public class·record·interface, 새 public 함수·method에는 같은 편집에서 의도 주석을 작성한다.
- 기존 코드의 동작, 계약, 상태값 의미, 실패 처리 방식을 바꾸면 같은 편집에서 관련 주석을 갱신한다.
- 아래 항목은 주석 없이 통과시키지 않는다.
  - nullable 값이나 상태 문자열이 업무적 의미를 가질 때: 허용 값 전체와 각 값의 뜻, 특히 null이 "0"이나 "없음"과 어떻게 다른지
  - 여러 구현 선택지 중 하나를 고른 로직: 왜 이 방식이고 무엇을 지키려는지
  - fallback, 예외 삼킴, 빈 값 반환처럼 겉으로 실패가 드러나지 않는 경로: 호출자가 그 결과를 어떻게 해석해야 하는지
  - 계층 경계를 넘는 계약(API DTO, entity column, 외부 연동 응답 파싱)
- 완료 보고 전에 이번 turn의 Write/Edit 대상을 훑어 위 기준을 만족하는지 확인하고, 주석을 붙이지 않기로 판단한 경우 그 근거를 짧게 남긴다.

### 작성 기준

- 설명 문장은 한국어로 작성한다. API, DTO, Flutter, token처럼 코드와 직접 대응하는 표준 기술 용어와 식별자는 원문 표기를 유지한다.
- Java 공개 class/interface/record와 Dart 공개 class·주요 service/API에는 Javadoc 또는 Dartdoc으로 **의도와 책임**을 설명한다.
- API endpoint, domain service, repository query, 상태 전이, 외부 연동 경계에는 입력·출력의 의미와 해당 계층이 책임을 가지는 이유를 설명한다.
- 보안, 트랜잭션, 동시성, 캐시, retry, fallback, timeout, 데이터 정규화처럼 선택지가 여러 개인 로직에는 **왜 현재 방식을 선택했는지**와 보호하려는 invariant를 짧게 남긴다.
- 상수, enum, 상태값, nullable 값이 업무적으로 특별한 의미를 가지면 그 의미와 허용 범위를 설명한다.
- 테스트 파일 또는 테스트 class에는 어떤 계약, 회귀, 실패 조건을 보장하는지 설명한다.
- 코드가 하는 일을 문법적으로 반복하지 않는다. 자명한 DTO field, getter/setter, 단순 위임, 일반 framework annotation에는 주석을 붙이지 않는다.
- 코드와 불일치하거나 과거 동작을 설명하는 주석은 남겨두지 않는다.
- 비밀값, 실제 token, 개인식별정보, 환경별 credential은 예시 주석에도 기록하지 않는다.
- 이미 적용된 Flyway migration처럼 checksum에 영향을 주는 불변 파일은 주석만을 위해 수정하지 않는다. 목적 설명은 대응 entity, 새 migration 또는 architecture 문서에 남긴다.

## 구현 체크리스트

- Spring 변경은 DTO, validation, exception handling, transaction, migration 영향을 확인한다.
- Flutter 변경은 화면, 상태, API client, model 경계를 확인한다.
- 외부 연동은 Azure, Replicate, AWS S3, FFmpeg, MySQL 관련 설정과 비밀값 노출 위험을 확인한다.
- 백엔드와 앱을 동시에 건드리면 요청/응답 형식, 에러 포맷, 인증 흐름을 먼저 정리한다.
- 새 기능이나 버그 수정은 가능한 한 가장 작은 의미 있는 테스트를 함께 추가하거나 안내한다.
- 관련 활성 문서의 기능 상태, API 계약, 설정 예시와 현재 구현이 일치하는지 확인한다.
- 수정한 문제가 트러블슈팅 문서화 대상인지 `troubleshooting-note` 스킬 기준으로 판단한다.

## 리뷰와 검증

- correctness, regression, security, missing tests를 우선 확인한다. 스타일보다 버그, API 계약 불일치, 보안 문제, 테스트 공백을 먼저 본다.
- 사용자 환경에서만 재현할 수 있는 검증은 실행 명령과 확인 지점을 안내하고, 사용자가 제공한 핵심 결과로 후속 점검한다.
- 로그가 필요하면 전체 로그 대신 핵심 에러 구간, 실패 명령, 재현 조건만 요청한다.
- 검증 명령은 변경 범위에 맞게 최소로 실행한다.

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

## Git 워크플로우

- 브랜치·커밋·PR은 phase가 아니라 기능 단위로 나눈다. 한 브랜치에는 독립적으로 리뷰·테스트·배포 판단이 가능한 변경 하나만 담는다.
- Issue 생성, 커밋, push, PR 생성, GitHub 리뷰 게시·답글은 사용자가 요청할 때만 하고, 외부에 게시하기 전에 내용을 보여주고 확인을 받는다.
- 단계별 절차는 스킬을 따른다: `issue-create` → `commit` → `pr-create` → `pr-review` → `review-apply`.

## 참고 파일

필요할 때만 읽는다.

- API 계약: `docs/api/api-reference.md`, `docs/api/error-codes.md`
- 아키텍처 결정: `docs/architecture/adr/README.md`

## 완료 보고

작업 완료 시 아래만 짧게 보고한다.

1. 작업 요약
2. 변경 파일
3. 검증 결과
4. 남은 리스크
5. 다음 액션
