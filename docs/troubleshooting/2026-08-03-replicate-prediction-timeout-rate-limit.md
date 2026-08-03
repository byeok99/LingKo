# Replicate Prediction timeout 이후 429가 연쇄 발생하는 문제

- 상태: 완화
- 최초 발견일: 2026-08-03
- 영향 범위: 평가 완료 화면의 입·혀 전환 영상 생성
- 심각도: SEV-3
- 영역: Backend / External / Operations
- 관련 Issue: [#44](https://github.com/byeok99/LingKo/issues/44)
- 관련 PR: [#77](https://github.com/byeok99/LingKo/pull/77)

## 문제 현상

전체 외부 통합 테스트 9건 중 Replicate Prediction 2건이 `starting` 상태에서 기존 60초 polling 상한을 넘었고, 후속 직접 생성 요청은 HTTP 429를 반환했다. 별도로 `바` 영상은 생성·S3 업로드에 성공했지만 과거 파일명 assertion 때문에 실패로 집계됐다.

## 사용자 또는 운영 영향

가이드 생성 실패는 정적 PNG fallback으로 완화되지만 사용자는 전환 영상을 받지 못한다. timeout된 공급자 작업이 계속 실행되면 비용과 동시 실행 슬롯을 소비해 후속 요청까지 제한할 수 있다.

## 발생 조건

- Replicate cold start 또는 queue 대기가 60초를 초과
- 여러 음절의 Prediction을 연속 생성
- 로컬 polling timeout 후 원격 Prediction 취소 없음
- 생성 요청과 polling의 429·5xx 재시도 없음

## 재현 방법

```bash
cd backend
set -a
source .env
set +a
./gradlew externalIntegrationTest --rerun-tasks
```

## 확인한 증거

- 최초 전체 실행: 9건 중 5건 성공, 4건 실패, 4분 9초
- `강`, `밥`: `starting` 상태가 60초 상한을 초과
- 마지막 직접 생성: `POST /v1/predictions`에서 HTTP 429
- S3: 실제 업로드 성공, AWS credential 오류 재발 없음

## 근본 원인

로컬 polling 상한이 공급자 cold start보다 짧았고 timeout 시 원격 작업을 취소하지 않았다. 또한 429·5xx를 즉시 최종 실패로 처리해 일시적인 공급자 제한을 흡수하지 못했다. 해시 기반 파일명으로 바뀐 뒤 외부 테스트 assertion이 갱신되지 않은 문제도 실패 수를 늘렸다.

## 해결 방법

- 기본 polling 상한을 60초에서 300초로 확대
- Prediction 생성 시 로컬 기한과 같은 `Cancel-After` 전송
- polling timeout·thread interrupt 시 cancel endpoint를 best effort 호출
- 생성·polling의 429와 5xx에 횟수 제한 지수 backoff 적용
- 생성 MP4 URL을 기존 `syllables` 테이블에 upsert하고 DB·S3 순서로 재사용
- 출시 전 검증된 MP4를 repeatable Flyway seed에 누적
- 외부 테스트를 해시 파일명 계약에 맞게 수정

## 선택하지 않은 대안

- 무제한 재시도: Worker와 비용을 제한할 수 없어 제외했다.
- 새 가이드 테이블: 기존 `syllables.mouth_url`, `tongue_url`로 요구사항을 충족해 추가하지 않았다.
- 서버가 migration 파일을 런타임에 수정: 배포 artifact 불변성과 source control을 깨므로 제외했다.

## 검증 방법

```bash
cd backend
./gradlew test integrationTest
set -a && source .env && set +a
./gradlew externalIntegrationTest \
  --tests 'com.lingko.lingko.infra.pronunciation.ReplicateApiClientTest' \
  --tests 'com.lingko.lingko.infra.pronunciation.FrameInterpolationVideoGeneratorTest.video_ba_test' \
  --rerun-tasks
```

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| Replicate 직접 호출 | 429 실패 | 성공 |
| 기존 `바` 영상 | assertion 실패 | S3 cache hit 성공 |
| 외부 대상 검증시간 | 미측정 | 3분 10초 |
| timeout 원격 취소 | 없음 | `Cancel-After` + cancel 호출 |

## 롤백 방법

PR #77을 되돌리고 `REPLICATE_MAX_POLL_ATTEMPTS`를 이전 값으로 복원한다. repeatable seed는 기존 MP4를 덮어쓰지 않으므로 별도 데이터 롤백은 필요하지 않지만, 잘못 추가한 신규 seed 행은 후속 repeatable migration 수정으로 제거하거나 교정한다.

## 재발 방지

- 429 후 성공과 timeout cancel endpoint 호출을 단위 테스트로 고정
- 생성 완료 MP4의 DB 재사용과 seed migration을 회귀 테스트로 고정
- 외부 테스트는 수동·야간·릴리스 전 실행으로 유지

## 모니터링 및 알림

Prediction 생성 HTTP 상태, retry 횟수, `starting`·`processing` 체류시간, cancel 성공 여부와 PNG fallback 비율을 관찰한다. 구조화 metric과 알림은 #44 후속 범위다.

## 남은 위험

- `Retry-After`와 Jitter 미반영
- Azure·S3 타임아웃·재시도 및 Circuit Breaker 미완료
- Guide Generation Job 자체의 DB Worker 영속화는 #42에 남음
- 동일 프레임이라도 음절이 다르면 현재 S3 hash가 달라질 수 있음

## 관련 코드와 문서

- `backend/src/main/java/com/lingko/lingko/infra/pronunciation/ReplicateApiClient.java`
- `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/GuideMediaResolver.java`
- `backend/src/main/resources/db/migration/R__seed_generated_syllable_guides.sql`
- `docs/development/testing-and-troubleshooting.md`
