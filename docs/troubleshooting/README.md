# LingKo 트러블슈팅 노트

이 디렉터리는 재발 가능성이 있거나 사용자, 데이터, 비용, 성능, 운영 또는 보안에 의미 있는 영향을 주는 문제의 증거, 원인, 해결 결정과 재발 방지 방법을 보존합니다. 일반적인 실행 방법과 단순 오류 해결은 [`../development/testing-and-troubleshooting.md`](../development/testing-and-troubleshooting.md)에 기록합니다.

## 작성 기준

다음 중 하나 이상을 만족하는 문제를 후보로 보고, 아래 판단 질문 중 두 개 이상이 `예`이면 노트를 작성합니다.

- 실제 또는 명확한 성능·DB·자원 병목
- 동시성, 중복 처리, transaction, lock, cache 불일치
- 데이터 유실·중복·잘못된 사용자 귀속
- 로그인, token, 녹음, 업로드, 평가 등 주요 사용자 흐름 실패
- 외부 서비스, timeout, retry, Rate Limit, 배포, migration, 환경변수, Secret 문제
- 보안·개인정보 문제
- 반복되는 CS 또는 여러 단계의 재현·진단이 필요한 문제
- 복잡한 원인과 다른 작업에 재사용할 수 있는 기술적 교훈

판단 질문:

1. 3개월 후 같은 문제를 만났을 때 이 기록이 도움이 되는가?
2. 다른 개발자가 같은 원인을 다시 조사할 가능성이 있는가?
3. 사용자, 데이터, 비용, 성능 또는 운영에 영향을 주는가?
4. 코드 수정만으로 원인과 예방책이 충분히 드러나지 않는가?

SEV-1·SEV-2, 보안·개인정보 또는 데이터 유실 위험은 답변 수와 관계없이 기록합니다.

## 제외 기준

- 단순 오탈자나 IDE 경고
- 원인이 즉시 명확한 import·compile 오류
- 영향과 반복성이 작은 한 줄 null 검사 누락
- 운영 코드에 영향이 없는 단순 test·mock 실수
- 재현되지 않고 원인도 확인되지 않은 추측
- 기존 노트와 원인 및 해결책이 같은 문제

제외 대상이라도 반복 발생하거나 사용자·운영 영향이 크면 작성합니다. 기존 노트와 같은 원인이면 새 파일 대신 기존 문서에 사례와 검증 결과를 추가합니다.

## 파일과 상태 규칙

- 파일명: `YYYY-MM-DD-<problem-kebab-case>.md`
- 상태: `조사 중`, `완화`, `해결`, `모니터링`
- 관련 Issue와 PR은 필수이며 완료 전 실제 링크나 번호로 교체합니다.
- 측정하지 않은 값은 추정하지 않고 `미측정`과 이유를 적습니다.
- token, Secret, 개인정보, 운영 credential과 긴 로그 전문은 기록하지 않습니다.

## 심각도

| 등급 | 기준 |
|---|---|
| SEV-1 | 서비스 사용 불가, 데이터 유실·노출, 광범위한 사용자 영향 |
| SEV-2 | 핵심 기능 실패, 상당수 사용자 영향, 즉시 대응 필요 |
| SEV-3 | 일부 사용자 또는 특정 조건에서 기능·성능 저하 |
| SEV-4 | 사용자 영향은 작지만 유지보수성과 재발 방지를 위해 기록할 가치가 있음 |

## 문서 인덱스

최신 발견일이 위에 오도록 정렬합니다.

| 날짜 | 심각도 | 영역 | 제목 | 상태 | 관련 Issue |
|---|---|---|---|---|---|
| 2026-08-06 | SEV-2 | Backend / External / Flutter | [가이드 영상이 오류 없이 흰 화면으로만 재생되는 문제](2026-08-06-guide-video-yuv444-white-screen.md) | 해결 | [#93](https://github.com/byeok99/LingKo/issues/93) |
| 2026-08-03 | SEV-3 | Backend / External / Operations | [Replicate Prediction timeout 이후 429가 연쇄 발생하는 문제](2026-08-03-replicate-prediction-timeout-rate-limit.md) | 완화 | [#44](https://github.com/byeok99/LingKo/issues/44) |
| 2026-07-30 | SEV-3 | Backend / DB / Transaction | [평가 작업 terminal 상태가 PROCESSING에 남는 문제](2026-07-29-evaluation-job-status-lost-after-quota-update.md) | 해결 | [#47](https://github.com/byeok99/LingKo/issues/47) |
| 2026-07-29 | SEV-3 | Backend / DB / Concurrency | [평가 재시도에서 작업과 쿼터가 중복 생성될 위험](2026-07-29-evaluation-request-idempotency.md) | 해결 | [#39](https://github.com/byeok99/LingKo/issues/39) |
| 2026-07-26 | SEV-3 | Backend / DB / Concurrency | [동시 연습 요청에서 일일 쿼터가 초과 예약되는 경쟁 조건](2026-07-26-practice-quota-race-condition.md) | 해결 | [#38](https://github.com/byeok99/LingKo/issues/38) |

## 문서 템플릿

```md
# 문제를 구체적으로 설명하는 제목

- 상태: 조사 중 | 완화 | 해결 | 모니터링
- 최초 발견일: YYYY-MM-DD
- 영향 범위: 영향받는 사용자, API, 앱, 데이터 또는 운영 환경
- 심각도: SEV-1 | SEV-2 | SEV-3 | SEV-4
- 영역: Backend / Flutter / DB / External / Operations / Security
- 관련 Issue: #번호 또는 링크
- 관련 PR: #번호 또는 링크

## 문제 현상

관찰된 증상만 기록한다.

## 사용자 또는 운영 영향

사용자가 완료하지 못한 작업, 데이터·비용·성능·운영 영향을 기록한다.

## 발생 조건

환경, 데이터 규모, 요청 순서, 동시성 등 확인된 조건을 기록한다.

## 재현 방법

1. 재현 준비
2. 실행 명령 또는 요청
3. 확인할 결과

## 조사 과정

확인 순서와 가설이 증거로 채택되거나 기각된 이유를 기록한다.

## 확인한 증거

비밀값을 제거한 로그 요약, query 수, metric, 실행 계획, stack trace 위치를 기록한다.

## 근본 원인

확인된 원인만 작성한다. 미확정이면 상태를 `조사 중`으로 유지한다.

## 해결 방법

임시 조치와 근본 해결을 구분하고 선택 근거를 기록한다.

## 선택하지 않은 대안

대안별 장단점과 선택하지 않은 이유를 기록한다.

## 검증 방법

자동 test, 수동 재현, 부하 test와 실행 조건을 기록한다.

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| query 수 | 미측정 | 미측정 |
| 응답시간 | 미측정 | 미측정 |
| 메모리 | 미측정 | 미측정 |

측정하지 않은 값은 성공 수치로 표현하지 않는다.

## 롤백 방법

코드, 설정, migration과 데이터 복구 순서를 기록한다.

## 재발 방지

회귀 test, 설계 규칙, review 항목과 자동화 계획을 기록한다.

## 모니터링 및 알림

관찰할 metric, log, 임계값과 담당 대응 절차를 기록한다.

## 남은 위험

미검증 환경, 운영 전 확인 사항과 후속 Issue를 기록한다.

## 관련 코드와 문서

- `path/to/file`
- 관련 설계·API·운영 문서
```
