# LingKo MVP 출시 실행 체크리스트

기준일: 2026-08-06
기준 브랜치: `feat/design-a-tokens`
대상 디자인: [LingKo Direction A](design_handoff_lingko_direction_a/README.md)

이 문서는 LingKo를 초대 기반 MVP로 출시하기 위해 남은 작업을 **한 번에 하나씩 처리하는 실행 기준**이다. 제품 요구사항의 상세 정의는 기존 요구사항 문서를 유지하고, 여기서는 작업 순서·완료 조건·검증 증거만 관리한다.

## 사용 방법

1. `현재 작업`의 첫 번째 미완료 항목 하나만 선택한다.
2. 연결된 요구사항·Issue·코드를 확인하고 테스트를 먼저 정의한다.
3. 구현, 자체 리뷰, 검증, 관련 활성 문서 갱신을 같은 작업에서 끝낸다.
4. 완료 조건을 모두 충족했을 때만 `[x]`로 변경한다.
5. 검증 명령과 PR 또는 Issue 링크를 해당 항목의 `완료 기록`에 추가한다.
6. 선행 작업이 끝나기 전에는 후속 항목을 시작하지 않는다. 단, 문서·스토어 자료처럼 코드와 충돌하지 않는 준비 작업은 별도로 진행할 수 있다.

상태 표기:

- `[ ]` 미착수 또는 완료 증거 없음
- `[~]` 진행 중
- `[x]` 완료 및 검증됨
- `[!]` 외부 결정이나 권한이 필요해 중단됨

## CAPABILITY

한글을 읽지 못하는 한국어 학습자가 Google 또는 허용된 소셜 계정으로 로그인한 뒤 문장을 선택하거나 입력하고, 표준 발음을 확인해 녹음하며, 실제 평가 결과를 단어 점수와 음절 발음 가이드로 이해하고 다시 연습할 수 있어야 한다. 이 흐름은 초대 사용자 10~30명이 실제 기기와 운영형 인프라에서 사용하더라도 중복 과금·중복 차감·개인정보 노출 없이 복구 가능해야 한다.

현재 Direction A의 11개 화면과 핵심 학습 흐름은 기능 브랜치에 대부분 구현되어 있다. 남은 핵심은 신규 화면 추가가 아니라 통합, 배포, 정책, 보안, 실환경 검증과 운영 안전장치다.

## CONSTRAINTS

### 고정된 제품 규칙

- UI는 영어를 기본으로 하고 모든 한국어 학습 콘텐츠에는 Revised Romanization을 제공한다.
- 단어 점수는 평가 공급자의 신뢰 가능한 응답만 사용한다.
- 음절은 점수 단위가 아니라 단어 하위의 발음 가이드 단위다. 단어 점수를 음절에 복제하지 않는다.
- 자연 충전 가능한 평가 기회는 최대 5회이며, 5회 미만에서 서버 시각 기준 1시간마다 1회 충전한다.
- 광고 보상은 자연 충전 시각을 초기화하지 않아야 한다.
- 동일한 Idempotency Key의 평가 요청은 외부 호출, 쿼터 차감, 결과 저장을 각각 한 번만 수행한다.
- 녹음 원본은 평가 성공 또는 최종 실패 후 삭제하고, 미제출·삭제 실패 객체는 S3 Lifecycle로 만료한다.
- 사용자는 자신의 기록, 저장 문장, 세션과 탈퇴 대상 데이터에만 접근할 수 있다.

### 출시 경계
- 실제 운영 Secret은 저장소, 앱 번들, Docker image와 로그에 포함하지 않는다.
- 운영 앱은 HTTPS API만 사용하며 localhost 기본값으로 빌드되지 않아야 한다.
- 가이드 생성은 일반 사용자 기능이 아니다. 인증되지 않은 공개 생성 API를 운영에 노출하지 않는다.
- Android와 iOS를 동시에 출시하려면 Apple 로그인을 구현하고 실기기에서 검증한다.
- 광고 SDK가 연결되지 않은 상태에서는 동작하지 않는 보상 버튼을 노출하지 않는다.

## IMPLEMENTATION CONTRACT

### 사용자와 운영자

- 학습자: 로그인, 문장 선택·저장, 녹음, 평가 결과 확인, 재연습, 기록 확인, 탈퇴
- 운영자: 배포, 외부 서비스와 비용 모니터링, 장애 복구, 가이드 자산 관리, 데이터 삭제 확인
- 외부 시스템: Google/Apple 인증, Azure 발음 평가, AWS S3, Replicate 가이드 생성, MySQL

### 핵심 상태 전이

```text
로그아웃
→ 로그인됨
→ 문장 선택됨
→ 녹음 완료
→ 업로드 완료
→ 쿼터 예약
→ 평가 대기/처리
→ 성공: 결과 저장 + 쿼터 확정 + 원본 삭제
→ 실패: 재시도 가능 상태 + 정책에 따른 쿼터 복구 + 원본 삭제
```

다음 invariant는 모든 출시 작업에서 유지한다.

- 재시도와 동시 요청으로 평가 job, 쿼터 차감, 결과가 중복되지 않는다.
- 앱 종료나 Worker 재시작 후에도 제출된 평가의 상태를 다시 조회할 수 있다.
- 인증 만료 시 한 번만 갱신하고, 갱신 불가 시 로그인 화면으로 안전하게 전환한다.
- 외부 서비스의 내부 오류, URL, credential은 사용자 응답에 노출하지 않는다.
- 탈퇴 완료 응답 전에 사용자 소유 S3 객체와 DB 데이터 삭제가 확인된다.

### 출시 시 관측할 최소 지표

- 로그인 성공률
- 첫 평가 완료율
- 평가 성공률과 p95 처리시간
- WAV 업로드 실패율
- Azure·Replicate 오류율과 429 발생 수
- 중복 차감·중복 저장 건수
- Crash-free 사용자 비율
- 사용자당 평가 횟수와 외부 서비스 비용
- 탈퇴·음성 삭제 성공률
- DB·S3 증가량과 Worker 대기 작업 수

## 현재 작업

아래 순서대로 하나씩 완료한다.

### 0. 출시 채널과 기능 범위 확정

- [ ] 첫 초대 MVP를 `Android closed testing`으로 시작할지, Android와 iOS를 동시에 배포할지 결정한다.
- [x] 광고 연동을 출시 범위에 포함한다. Flutter AdMob·UMP·서버 지급 test flow를 연결했고 운영 전 SSV 검증을 완료한다.
- [x] 실시간 가이드 생성 HTTP API는 기본 비활성화하고, 내부 service Secret을 설정한 환경에서만 제한적으로 연다.
- [ ] 디자인 문서의 `weak sound/syllable` 표현을 실제 단어 중심 구현과 일치시키기로 확정한다.

완료 조건:

- 출시 플랫폼, 포함 기능, 제외 기능이 이 문서의 `결정 기록`에 적혀 있다.
- 선택에 따라 4번의 Apple 로그인과 광고 항목의 필수 여부가 명확하다.

완료 기록: 미작성

### 1. Direction A 변경 통합

- [ ] [PR #88](https://github.com/byeok99/LingKo/pull/88)의 앱·백엔드 API 계약과 70개 변경 파일을 최종 리뷰한다.
- [ ] Direction A 11개 화면, 저장 문장, 취약 단어, 로마자 가이드 회귀 테스트를 확인한다.
- [ ] 문서에서 완료된 Issue를 미완료로 안내하는 오래된 상태를 정리한다.
- [ ] `develop`에 병합한 뒤 깨끗한 checkout에서 전체 검증한다.

완료 조건:

- PR #88이 `develop`에 병합되어 있다.
- `./gradlew test integrationTest`, `flutter analyze`, `flutter test --coverage`가 통과한다.
- Flutter line coverage가 프로젝트 기준 80% 이상이거나, 미달 범위와 보완 Issue가 승인되어 있다.
- 관련 요구사항·로드맵·기술부채 문서가 현재 코드와 일치한다.

완료 기록: 현재 로컬 기준 Backend test/integrationTest, Flutter analyze, Flutter 86 tests 통과. Flutter line coverage 73.59%로 완료 조건 미달.

### 2. 가이드 생성 API 비용·보안 경계 확립

- [x] [#41](https://github.com/byeok99/LingKo/issues/41)에 따라 `/api/pronunciation/guide-jobs`를 내부 service 인증·인가한다.
- [x] 명시적 활성화 설정이 없으면 endpoint가 등록되지 않도록 한다.
- [x] 생성 요청에 caller별 Rate Limit, 동시 실행·입력 크기 제한과 민감값 제외 감사 로그를 적용한다.
- [x] 익명·일반 사용자·내부 service 요청과 429 경계에 대한 보안 테스트를 추가한다.

완료 조건:

- 익명 사용자가 외부 생성 비용을 발생시킬 수 없다.
- 일반 학습 앱 흐름은 가이드 생성 권한 없이도 동작한다.
- 공개 endpoint를 유지한다면 작업 유실 대응 범위를 [#42](https://github.com/byeok99/LingKo/issues/42)에서 확정한다.

완료 기록: [Issue #41](https://github.com/byeok99/LingKo/issues/41). HTTP surface 기본 비활성화, 32자 이상 내부 Secret, 기본 분당 2회·동시 1개, URL allowlist·SSRF/redirect/25MiB 제한, Micrometer admission/completion 지표를 적용했다. `./gradlew test --tests '*GuideGenerationJob*' --tests '*MicrometerGuideGenerationJobTelemetryTest'` 통과. 작업 상태 영속화는 [#42](https://github.com/byeok99/LingKo/issues/42) 후속 범위다.

### 3. 개인정보·약관·계정 삭제 공개 경로

- [ ] 개인정보처리방침과 이용약관을 공개 HTTPS URL로 배포한다.
- [ ] 앱의 `Audio & privacy`와 `About`에서 정책과 버전을 열 수 있게 한다.
- [ ] Google Play용 외부 계정 삭제 안내·요청 URL을 제공한다.
- [ ] 녹음 수집 목적, 저장 위치, 삭제 시점, 외부 처리자와 탈퇴 정책을 스토어 고지와 일치시킨다.
- [ ] [#71](https://github.com/byeok99/LingKo/issues/71)의 실제 AWS Lifecycle·Versioning·탈퇴 삭제 E2E를 완료한다.

완료 조건:

- 앱과 스토어 심사 정보에서 동일한 정책 URL에 접근할 수 있다.
- 테스트 계정 탈퇴 후 DB 데이터와 S3 current object/version/delete marker가 남지 않는다.
- 삭제 실패 시 사용자 응답과 운영자 재처리 절차가 검증되어 있다.

완료 기록: 미작성

### 4. 플랫폼 인증과 소셜 로그인 완결

- [ ] Android 운영 OAuth client와 패키지 서명 fingerprint를 연결한다.
- [ ] iOS를 포함한다면 Apple 로그인 앱·백엔드 연동을 구현한다.
- [ ] Google/Apple 로그인 취소, 네트워크 실패, 잘못된 token, 탈퇴 계정 재로그인을 검증한다.
- [ ] [#60](https://github.com/byeok99/LingKo/issues/60) 실제 token 만료·회전·세션 복원 E2E를 완료한다.
- [ ] [#62](https://github.com/byeok99/LingKo/issues/62) Refresh Token 동시 갱신 부하를 검증한다.

완료 조건:

- 선택한 모든 출시 플랫폼의 로그인과 세션 복원이 실기기에서 동작한다.
- 로그에 OAuth credential과 token이 남지 않는다.
- 만료·재사용·로그아웃 이후 폐기된 token으로 보호 API를 호출할 수 없다.

완료 기록: 미작성

### 5. 운영 빌드와 환경 설정

- [ ] Android debug signing을 운영 keystore로 교체하고 CI Secret으로 주입한다.
- [ ] iOS를 포함한다면 distribution certificate와 provisioning을 구성한다.
- [ ] `LINGKO_API_BASE_URL`을 운영 HTTPS 주소로 고정한다.
- [ ] release 빌드에서 localhost 또는 HTTP 주소가 선택되면 실패하도록 검증한다.
- [ ] 앱 이름, 설명, 버전, 빌드 번호, 아이콘, 스플래시와 Bundle ID를 확정한다.
- [ ] 운영 환경의 로그 레벨, CORS, S3 bucket, DB, Azure, Replicate 설정을 점검한다.

완료 조건:

- source와 image에 Secret이 없는 서명된 release artifact가 생성된다.
- artifact가 운영 HTTPS backend에만 연결된다.
- Android/iOS 선택 플랫폼의 실기기 설치와 시작이 성공한다.

완료 기록: 미작성

### 6. 핵심 학습 흐름 실환경 E2E

- [ ] 추천 문장과 자유 문장 각각 첫 평가를 완료한다.
- [ ] 마이크 권한 거부·영구 거부·재허용을 검증한다.
- [ ] 녹음 중 전화, 백그라운드, 취소, 재시작과 앱 종료를 검증한다.
- [ ] 업로드 단절 후 재시도해도 평가·차감·저장이 한 번인지 확인한다.
- [ ] 사용자 A의 기록·저장 문장을 사용자 B가 조회할 수 없는지 확인한다.
- [ ] 1시간 자연 충전, 최대 5회, 쿼터 예약·확정·복구를 서버 시각 기준으로 검증한다.
- [ ] 앱 종료와 Worker 강제 종료 후 평가 상태 복구를 실제 MySQL에서 확인한다.

완료 조건:

- 인증 → 녹음 → 업로드 → 평가 → 결과 → 기록 → 재연습이 실기기에서 성공한다.
- 재시도·동시 요청에서 중복 차감·중복 저장이 0건이다.
- 실패 경로마다 사용자가 이해할 수 있는 안내와 안전한 재시도 경로가 있다.

완료 기록: 미작성

### 7. 외부 서비스 장애 복원력

- [ ] [#44](https://github.com/byeok99/LingKo/issues/44)의 Azure·S3·Replicate timeout을 호출 목적별로 확정한다.
- [ ] 재시도 가능 오류, 즉시 실패 오류, 쿼터 복구 여부를 표로 정의한다.
- [ ] 429 `Retry-After`, 지수 백오프, Jitter와 Circuit Breaker를 적용한다.
- [ ] 외부 서비스 지연 시 thread, connection, Worker가 고갈되지 않는지 검증한다.

완료 조건:

- 외부 서비스 timeout·429·5xx 상황에서 API 전체가 고갈되지 않는다.
- 최종 실패 시 원본 음성과 쿼터가 정책대로 정리된다.
- 내부 오류·URL·credential이 앱 응답에 노출되지 않는다.

완료 기록: 미작성

### 8. 관측성과 Crash reporting

- [ ] [#48](https://github.com/byeok99/LingKo/issues/48)에 따라 readiness/liveness를 제공한다.
- [ ] API와 Worker 로그에 Request ID와 evaluation job ID를 연결한다.
- [ ] 핵심 지표와 alert threshold를 대시보드에 구성한다.
- [ ] Flutter crash reporting을 활성화하고 개인정보 필드를 제외한다.
- [ ] 비용 급증, 평가 실패율, Worker backlog, DB·S3 용량 alert를 검증한다.

완료 조건:

- 테스트 장애를 발생시켜 앱 crash와 backend 오류를 추적할 수 있다.
- 한 평가 요청을 앱 오류부터 Worker·외부 API 결과까지 연결해 조사할 수 있다.
- alert가 실제 운영자 채널에 전달된다.

완료 기록: 미작성

### 9. CI/CD, 배포와 롤백

- [ ] [#49](https://github.com/byeok99/LingKo/issues/49)에 따라 PR에 Backend·Flutter 검증을 필수화한다.
- [ ] migration 검토, secret scan, release build 검증을 pipeline에 포함한다.
- [ ] 동일한 backend image를 검증 환경에서 운영으로 승격한다.
- [ ] 이전 앱/backend 버전과 DB migration 호환성을 확인한다.
- [ ] 장애 시 backend와 Worker를 이전 버전으로 되돌리는 절차를 연습한다.

완료 조건:

- 검증 실패 PR은 병합할 수 없다.
- 수동 수정 없이 반복 가능한 배포가 가능하다.
- 실제 rollback 연습 결과와 소요 시간이 Runbook에 기록되어 있다.

완료 기록: 미작성

### 10. 백업·복구와 최소 부하 검증

- [ ] [#50](https://github.com/byeok99/LingKo/issues/50)의 DB 백업 주기, 보존 기간, RPO/RTO를 확정한다.
- [ ] 새 환경에 DB를 복원하고 사용자·평가·쿼터 정합성을 확인한다.
- [ ] [#52](https://github.com/byeok99/LingKo/issues/52)의 10~30명 초대 규모 부하 시나리오를 실행한다.
- [ ] 로그인, 업로드, 평가 조회의 p95와 오류율을 측정한다.
- [ ] 사용자당 Azure·Replicate·S3 비용과 일일 상한을 계산한다.

완료 조건:

- 백업 파일이 존재하는 것뿐 아니라 실제 복원에 성공한다.
- 초대 규모에서 목표 SLO를 만족하거나 명시적으로 승인된 제한이 있다.
- 비용 또는 queue가 한도를 넘을 때 신규 평가를 안전하게 제한할 수 있다.

완료 기록: 미작성

### 11. 초대 베타 Go/No-Go

- [ ] 선택 플랫폼의 서명된 release artifact를 실기기에 설치한다.
- [ ] 내부 개발 계정으로 24시간 이상 smoke test를 수행한다.
- [ ] 10~30명 초대, 피드백, 장애 연락과 개인정보 문의 경로를 준비한다.
- [ ] 아래 Go/No-Go 조건을 함께 검토한다.

Go 조건:

- [ ] 1~10번의 필수 항목 완료
- [ ] 인증·평가·저장·재연습 E2E 성공
- [ ] 중복 차감·중복 저장 0건
- [ ] 정책·약관·계정 삭제 접근 가능
- [ ] Secret scan, 백업 복구, alert, rollback 검증 완료
- [ ] 선택 플랫폼 release build 실기기 검증 완료
- [ ] 외부 서비스 장애 안내와 쿼터 복구 검증 완료
- [ ] 초대 규모 SLO와 비용 한도 확인

완료 기록: 미작성

## NON-GOALS

초대 MVP에서는 다음 기능을 기본 범위로 두지 않는다. 출시를 막는 근거가 확인되기 전에는 위 작업보다 우선하지 않는다.

- 사용자별 추천 알고리즘 고도화
- 음절별 추정 점수 생성
- 다국어 UI
- Push 또는 local notification
- 소셜·랭킹·연속 학습 보상
- 관리자용 대형 dashboard
- 다중 지역·대규모 queue 확장
- 측정 근거 없는 기록 조회·평가 저장 선행 최적화

## OPEN QUESTIONS

아래 결정은 0번 작업에서 확정한다.

1. 첫 초대 MVP를 Android로 먼저 출시할 것인가?
2. iOS를 동시에 포함한다면 Apple 로그인을 이번 범위에 포함할 것인가?
3. 광고 보상 없이 `+` 버튼을 숨길 것인가, 광고 SDK와 서버 검증까지 구현할 것인가?
4. 가이드 생성 endpoint를 운영에서 비활성화할 것인가, 관리자 인증과 durable job으로 운영할 것인가?
5. 취약 발음의 제품 단위를 `음절`이 아니라 `단어`로 명시할 것인가?
6. 초대 베타의 목표 SLO와 사용자당 외부 서비스 비용 상한은 얼마인가?

## 결정 기록

| 날짜 | 결정 | 이유 | 영향 |
|---|---|---|---|
| - | 미결정 | 0번 작업에서 작성 | - |

## HANDOFF

다음 작업은 **0. 출시 채널과 기능 범위 확정**이다. 결정이 끝나면 **1. Direction A 변경 통합**으로 이동한다. 각 구현 작업은 `tdd-workflow`로 회귀 테스트를 먼저 추가하고, 완료 전 `verification-loop` 기준으로 build·test·lint·security와 문서 동기화를 확인한다.

관련 기준 문서:

- [Direction A 디자인 핸드오프](design_handoff_lingko_direction_a/README.md)
- [MVP 기능 정의서](requirements/functional-requirements.md)
- [MVP 비기능 정의서](requirements/non-functional-requirements.md)
- [출시 로드맵](roadmap/release-roadmap.md)
- [출시·성능 Issue Backlog](roadmap/issue-backlog.md)
- [기술 부채](technical-debt.md)
- [운영 Runbook](operations/operations-runbook.md)
- [보안·개인정보](security/security-and-privacy.md)
