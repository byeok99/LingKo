# 앱과 운영 서버의 약관 버전 배포 불일치

- 상태: 모니터링
- 최초 발견일: 2026-09-07
- 영향 범위: 최신 앱 회원가입·약관 동의 저장
- 심각도: SEV-2
- 영역: Backend / Flutter / Operations
- 관련 Issue: [#111](https://github.com/byeok99/LingKo/issues/111)
- 관련 PR: 생성 후 연결

## 문제 현상

사용자가 약관 동의 화면에서 `Could not save your agreement. Please try again.` 문구와 함께 진행 불가를 보고했다. 이 문구는 앱의 동의 저장 흐름에서 여러 실패를 공통 표시하므로 문구만으로 정확한 서버 응답을 단정할 수 없다.

## 사용자 또는 운영 영향

로그인 이후 동의 기록이 완료되지 않으면 Home으로 진행할 수 없다. 영향 사용자 수는 미측정이다.

## 발생 조건과 재현 방법

앱의 `consentDocumentVersion`은 `2026-09-04`인데 이전 서버는 `2026-08-07`을 현재 버전으로 사용한다. `LegalConsentService`는 서로 다른 버전의 제출을 `Unsupported consent document version`으로 거절한다. 격리 환경에서 서버 정책과 다른 버전으로 동의를 제출하면 이 검증 경로를 재현할 수 있다. 실제 사용자 실패 요청의 HTTP 응답은 확보하지 않았다.

## 조사 과정과 확인한 증거

- 앱 동의 저장의 일반 오류 처리와 서버의 버전 일치 검사를 확인했다.
- 운영 공개 영문 약관의 시행일은 배포 전 `2026-08-07`이었다.
- 서버 Git checkout은 `c6cee95`였고, 최신 `develop`은 `f1ca001`이었다.
- 해당 범위의 Backend 차이는 정책 상수와 법무 문서·작업 이력이다. 새로운 migration이나 평가 Worker 코드 변경은 없다.
- 최신 정책 상수와 앱 상수는 모두 `2026-09-04`다.

## 근본 원인

최신 약관 버전을 포함한 소스가 Git에 반영됐지만 운영 서버 checkout·실행 이미지가 갱신되지 않은 배포 불일치를 확인했다. 이는 보고된 증상을 설명하는 원인이지만, 사용자 요청의 실제 오류 응답이 없어 개별 실패 원인의 최종 확인은 남아 있다.

## 해결 방법

기존 API 이미지를 `lingko-backend:rollback-20260907`로 보관하고, 서버 소스를 `f1ca001`로 fast-forward한 뒤 API 이미지를 빌드했다. DB·Worker는 유지하고 API만 교체했다. 2026-09-07 10:05 KST에 API 시작 완료를 확인했다.

## 선택하지 않은 대안

- 서버가 임의의 구버전 동의를 허용하는 방식은 현재 문서 동의 계약을 약화하므로 적용하지 않는다.
- 앱 상수만 과거 버전으로 내리는 방식은 이미 개정된 문서와 다시 충돌하고 설치된 앱도 바로 고칠 수 없다.
- 전체 Compose 종료는 DB와 평가 Worker까지 중단하므로 이번 약관 변경에는 필요하지 않다.

## 검증 방법

- 로컬: `./gradlew test --tests '*LegalConsentServiceTest' --tests '*LegalConsentControllerTest' --tests '*LegalDocumentSourceSyncTest' bootJar` 성공. 9개 테스트, 실패 0개.
- 운영: 이미지 빌드 성공. 공개 영문 약관·처리방침 모두 HTTP 200, 시행일 `2026-09-04` 확인. 추천 문장 API HTTP 200, 미인증 동의 API HTTP 401 확인. API 시작 로그에서 스키마 최신 상태·신규 migration 불필요 확인. DB와 Worker의 기존 실행 유지 확인.
- 실제 앱: 동의 제출 후 `required=false` 및 Home 진입 확인 필요.

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| 서버 checkout | `c6cee95` | `f1ca001` |
| 공개 약관 시행일 | `2026-08-07` | `2026-09-04`, HTTP 200 |
| 실제 사용자 가입 | 진행 불가 보고 | 기기 확인 필요 |
| 응답시간·query 수·메모리 | 미측정 | 미측정: 이번 검증은 기능·배포 상태에 한정 |

## 롤백 방법

서버 Backend 디렉터리에서 `docker tag lingko-backend:rollback-20260907 backend-backend` 후 `docker compose up -d --no-deps backend`를 실행한다. DB 변경은 없으므로 DB rollback은 불필요하다. 이전 약관 버전으로 돌아가면 최신 앱 가입 문제가 재발할 수 있다.

## 재발 방지와 모니터링

- 앱 출시 전에 서버 정책과 공개 법무 문서의 버전을 함께 확인한다.
- Git 병합 완료와 운영 배포 완료를 구분한다.
- `POST /api/legal/consent`의 400·5xx 증가와 버전 불일치를 확인하되 토큰·동의 body를 기록하지 않는다.
- 자동 배포 후 검증과 경보 임계값은 미구현이며 후속 운영 과제로 남긴다.

## 남은 위험

실제 사용자 세션의 동의 저장 성공은 미검증이다. 공개 문서가 최신이어도 네트워크·DB 등 별도 오류 가능성은 남는다.

## 관련 코드와 문서

- `app/lib/app/lingko_app.dart`: 동의 저장·실패 처리
- `app/lib/models/consent_selection.dart`: 앱 문서 버전
- `backend/src/main/java/com/lingko/lingko/core/domain/legal/LegalConsentPolicy.java`: 서버 문서 버전
- `backend/src/main/java/com/lingko/lingko/core/domain/legal/service/LegalConsentService.java`: 버전 검증
- [운영 Runbook](../operations/operations-runbook.md)
