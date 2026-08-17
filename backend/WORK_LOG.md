# Work Log

## 2026-08-12 - Apple OAuth 검증 의존성·설정 추가

- 변경 파일: `build.gradle`, `.env.example`, `application.example.yaml`, `README.md`, `WORK_LOG.md`
- 내용: Nimbus JOSE JWT와 공개 Apple App ID 환경변수 계약을 추가하고 운영 설정을 안내했다.
- 검증: Backend 단위 293개·통합 16개 통과, 통합 합산 라인 커버리지 80.20%, OSV 추가 의존성 취약점 0건
- 리스크: Apple authorization code 교환·revocation과 endpoint rate limit은 출시 전 필요

## 2026-08-12 - 로컬 AdMob SSV 광고 단위 설정

- 변경 파일: `.env`, `WORK_LOG.md`
- 내용: 로컬 Backend가 LingKo iOS·Android 보상형 광고의 SSV callback을 허용하도록 숫자 광고 단위 ID와 보상 계약을 설정했다.
- 검증: 비밀값을 출력하지 않고 관련 환경변수 이름과 값 형식을 확인
- 리스크: Google SSV callback은 공개 HTTPS Backend 주소에서 E2E 검증 필요

## 2026-08-12 - Google AdMob SSV 보상 검증 추가

- 변경 파일: `.env.example`, `README.md`, `docker-compose.yml`, `src/`
- 내용: 1회성 session, Google ECDSA 공개키 검증, 광고 정책 allowlist와 전역 transaction 멱등 지급을 추가했다.
- 검증: SSV·quota·migration·계정 삭제 대상 테스트 통과
- 리스크: 운영 callback URL과 실제 광고 단위 E2E는 배포 환경에서 확인 필요

## 2026-08-09 - 가이드 생성 API 비용 경계 설정

- 변경 파일: `build.gradle`, `.env.example`, `application.example.yaml`, `docker-compose.yml`, `Dockerfile`, `README.md`, `WORK_LOG.md`
- 내용: guide-jobs를 기본 비활성화하고 내부 Secret, 분당 생성량, 동시 실행 한도를 환경변수로 구성했으며 Micrometer 지원 의존성을 추가했다. ignore된 로컬 설정 대신 추적 중인 예시 설정을 Docker jar에 포함한다.
- 검증: `./gradlew test integrationTest`, 타깃 보안 테스트, `git diff --check` 통과
- 리스크: 다중 instance의 전역 한도와 작업 영속화는 #42 후속 범위

## 2026-08-07 - 법무 문서 서빙을 위한 Markdown 변환 의존성 추가

- 변경 파일: `build.gradle`
- 내용: 약관·처리방침을 Markdown 원본 그대로 두고 서빙 시점에 HTML로 변환하기 위해 `commonmark`와 GFM table 확장을 추가했다. HTML 사본을 저장소에 따로 두면 `docs/legal` 원본과 어긋나기 쉬워 변환을 택했다.
- 검증: `./gradlew compileJava`, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-03 - Replicate 안정화와 출시 전 가이드 seed 운영

- 변경 파일: `.env.example`, `application.example.yaml`, `README.md`, `WORK_LOG.md`
- 내용: Replicate polling을 5분으로 늘리고 제한 재시도 설정을 추가했으며, 생성 완료 MP4를 repeatable migration에 누적하는 출시 전 운영 절차를 문서화했다.
- 검증: Backend 단위·내부 통합 테스트와 Replicate 직접 호출·S3 cache 외부 테스트 통과
- 리스크: `Retry-After`·Jitter와 모든 외부 서비스 Circuit Breaker는 #44, Guide Job 자체 영속화는 #42에 남음

## 2026-07-30 - 평가 영상·terminal 실패 운영 계약 보완

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 평가 완료 모든 음절의 영상 생성과 누락된 쿼터 예약이 최종 실패 상태 commit을 막지 않는 운영 조건을 안내했다.
- 검증: Backend 단위 201개·통합 11개, Docker 재빌드, API 200과 고착 작업 4건의 `FAILED` 수렴 확인
- 리스크: 실제 Replicate 영상 생성 content는 앱 재평가로 확인 필요

## 2026-07-30 - 평가 결과 영상 가이드 운영 설정

- 변경 파일: `.env.example`, `application.example.yaml`, `README.md`, `WORK_LOG.md`
- 내용: 최초 영상 cache miss의 외부 보간 시간을 수용하도록 Worker lease 기본값을 600초로 늘리고 Replicate·S3·FFmpeg 운영 조건을 안내했다.
- 검증: Backend 단위 199개·통합 11개 통과
- 리스크: 실제 Replicate·S3·FFmpeg E2E와 최초 생성 시간은 운영 환경에서 확인 필요

## 2026-07-30 - 표준 발음 단일 규칙 원칙 문서화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 추천 콘텐츠에는 정답 발음을 저장하지 않고 모든 입력을 현재 음운 규칙으로 변환하며 평가 데이터만 snapshot으로 보존한다고 명시했다.
- 검증: Backend 단위 190개·통합 11개 통과
- 리스크: 실제 MySQL V12 migration은 배포 전 별도 확인 필요

## 2026-07-29 - 회원 탈퇴·음성 보존 정책 안내

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 계정 삭제 API의 S3 우선 삭제와 1일 Lifecycle 설정 파일·수동 적용 책임을 안내했다.
- 검증: Backend 전체 `test integrationTest` 통과
- 리스크: 실제 AWS Lifecycle 적용과 표본 만료 검증은 #71에서 추적

## 2026-07-29 - SQS 제거와 독립 DB Worker 1개 운영

- 변경 파일: `.env.example`, `application.example.yaml`, `build.gradle`, `docker-compose.yml`, `README.md`, `WORK_LOG.md`
- 내용: SQS 의존성·설정을 제거하고 API 내부 Worker를 끈 채 web 없는 DB polling Worker 컨테이너 한 개를 기본 실행하도록 단순화했다.
- 검증: `./gradlew test integrationTest`와 기본 `docker compose config --quiet` 통과
- 리스크: 같은 Docker 호스트의 자원은 공유하며 실제 MySQL lock·처리량은 미측정

## 2026-07-29 - SQS 독립 평가 Worker 실행 구성

- 변경 파일: `.env.example`, `application.example.yaml`, `build.gradle`, `docker-compose.yml`, `README.md`, `WORK_LOG.md`
- 내용: SQS 설정과 의존성을 추가하고 같은 image를 API와 web 없는 평가 Worker로 분리해 replica를 독립 조절하도록 구성했다.
- 검증: `./gradlew test integrationTest`와 Queue profile `docker compose config --quiet` 통과
- 리스크: 실제 AWS SQS·MySQL·Azure 부하와 DLQ 정책은 운영 환경에서 확인 필요

## 2026-07-29 - 평가 Idempotency 정리 설정 예시 추가

- 변경 파일: `application.example.yaml`, `WORK_LOG.md`
- 내용: 완료 작업의 7일 보존, 정리 주기와 batch 크기 환경변수 예시를 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 운영 완료 작업 증가량에 맞춰 batch와 주기를 조정해야 함

## 2026-07-27 - 평가 Worker 운영 설정 예시 추가

- 변경 파일: `application.example.yaml`, `WORK_LOG.md`
- 내용: presigned URL 만료와 DB Worker polling·lease·retry 설정 예시를 추가했다.
- 검증: Backend 테스트 및 integrationTest 통과
- 리스크: 운영 환경에서 Azure 처리시간에 맞춘 lease 조정 필요

## 2026-07-24 - Refresh Token 백엔드 문서 정합화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 갱신·폐기 API가 미구현이라는 과거 주의사항을 현재 구현 상태로 교체하고 운영 전 동시 갱신 부하 검증을 후속 리스크로 분리했다.
- 검증: Refresh Token endpoint·서비스·테스트와 README 대조, `git diff --check`
- 리스크: 실제 MySQL 환경의 동시 DB refresh 부하 테스트 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `Dockerfile`, `application.example.yaml`, `build.gradle`, `docker-compose.yml`, `settings.gradle`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과, `git diff --check` 통과
- 리스크: 동작 변경 없음

## 2026-07-23 - Java 21 toolchain과 Docker 런타임 전환

- 변경 파일: `build.gradle`, `Dockerfile`, `README.md`, `WORK_LOG.md`
- 내용: Gradle Java toolchain을 21로 올리고 Docker 빌드·실행 이미지를 JDK/JRE 21로 통일했다. Virtual Thread 등 Java 21 기능은 별도로 활성화하지 않았다.
- 검증: Java 21 환경의 `./gradlew test integrationTest`와 Docker 이미지 빌드 통과, 런타임 OpenJDK 21.0.11 확인
- 리스크: 로컬 JDK 21이 없는 개발자는 설치하거나 Docker 기반 검증을 사용해야 함

## 2026-07-20 - Google OAuth backend env 설정

- 변경 파일: `.env`, `WORK_LOG.md`
- 내용: 백엔드 Google ID token 검증에 사용할 Web OAuth Client ID를 `GOOGLE_CLIENT_ID`에 설정했다. 현재 앱 로그인 방식에서는 `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`를 사용하지 않는다.
- 검증: 값 노출 없이 `.env`의 `GOOGLE_CLIENT_ID` 설정 여부를 확인함
- 리스크: `JWT_SECRET_KEY`가 아직 비어 있어 실제 로그인 테스트 전에 32자 이상의 secret을 설정해야 함
