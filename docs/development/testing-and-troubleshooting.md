# 테스트 구성과 재현 범위

## 기본 검증

백엔드 디렉터리:

```bash
./gradlew compileJava
./gradlew test
./gradlew integrationTest
```

앱 디렉터리:

```bash
flutter analyze
flutter test
```

명령 목록은 실행 결과가 아닙니다. 변경마다 실제 수행한 범위와 결과를 구분합니다.

## Backend CI

`develop` 대상 Pull Request를 열거나 새 commit을 push하면 GitHub Actions의 `Backend CI`가 실행됩니다. Java 21 환경에서 단위 test, 통합 test, 합산 line coverage 80% 기준과 `bootJar` 생성을 순서대로 검증합니다.

CI 성공은 운영 배포를 의미하지 않습니다. 현재 workflow는 검증만 수행하며, 외부 서비스 자격증명과 비용이 필요한 `externalIntegrationTest`는 실행하지 않습니다. `Backend test`는 `develop` ruleset의 required status check입니다.

## Flutter CI

`develop` 대상 Pull Request에는 `Flutter CI`도 실행됩니다. 프로젝트 생성 환경과 같은 Flutter 3.29.1을 설치하고 `pubspec.lock`을 강제한 의존성 설치, 정적 분석, 전체 test와 line coverage 80% 기준을 검증합니다.

이 workflow는 Linux에서 실행하는 Dart·Flutter test gate입니다. iOS 서명 build, App Store 배포, 실제 기기 권한과 외부 API 연결은 검증하지 않습니다. `Flutter test`는 `develop` ruleset의 required status check입니다.

## Docker CI

`develop` 대상 Pull Request에는 `Docker CI`도 실행됩니다. placeholder DB 비밀번호와 추적된 `.env.example`로 Compose 설정을 검증하고, Backend Dockerfile로 Registry에 push하지 않는 임시 이미지를 빌드합니다. API, evaluation worker와 guide prewarm은 같은 Dockerfile을 사용하므로 한 번의 image build로 공통 runtime을 검증합니다.

빌드가 끝나면 image 안의 Spring Boot JAR, Java와 FFmpeg 실행 가능 여부를 확인합니다. 이 검증은 컨테이너 기동, MySQL 연결, 운영 Secret, Registry 업로드와 EC2 배포를 포함하지 않습니다. `Docker build`는 `develop` ruleset의 required status check입니다.

## develop 병합 보호

`protect-develop` ruleset은 `develop` 변경에 Pull Request를 요구하고 `Backend test`, `Flutter test`, `Docker build`가 모두 성공해야 병합을 허용합니다. 세 이름은 GitHub required status check가 workflow가 아닌 job 이름을 사용한다는 계약에 맞춰 고유하게 유지합니다.

필수 check는 최신 `develop` 기준으로 다시 검증하며 GitHub Actions가 생성한 결과만 인정합니다. ruleset에는 bypass 사용자를 두지 않고 branch 삭제와 force push를 차단합니다. 개인 프로젝트이므로 다른 사용자의 승인은 요구하지 않지만, 작성자 자신도 PR과 CI 검증은 우회할 수 없습니다. 병합 이후 수동으로 실행하는 `Backend CD`는 required check에 포함하지 않습니다.

Required check 이름 회귀 검증:

```bash
./scripts/tests/ci-required-checks-test.sh
```

## Backend CD

`develop` 병합은 운영 배포를 자동 실행하지 않습니다. 배포할 commit을 CI에서 검증하고 `Actions` → `Backend CD` → `Run workflow`에서 `develop`을 선택한 뒤 운영 배포 확인 항목을 체크해야 `production` Environment 배포가 시작됩니다. 이는 배포 가능한 상태를 계속 유지하되 운영 반영은 사람이 결정하는 Continuous Delivery 정책입니다.

Environment의 deployment branch policy는 `develop`만 허용합니다. workflow도 선택한 ref가 `develop`인지, 명시적 승인 입력이 `true`인지 다시 검사합니다. 동시 배포는 하나로 제한하며 진행 중인 배포를 새 실행이 취소하지 않습니다.

GitHub Actions는 장기 AWS Access Key를 저장하지 않습니다. `id-token: write`로 발급받은 OIDC token을 AWS STS의 단기 자격 증명으로 교환하며, AWS role의 신뢰 조건은 `repo:byeok99/LingKo:environment:production`으로 제한합니다. Environment에는 다음 비밀값이 아닌 설정값이 필요합니다.

- `AWS_REGION`: 현재 운영 리전
- `AWS_DEPLOY_ROLE_ARN`: GitHub OIDC가 assume할 최소 권한 role
- `AWS_EC2_INSTANCE_ID`: SSM managed node로 등록된 배포 대상 하나

배포 role은 대상 instance와 AWS 관리 문서 `AWS-RunShellScript`에 대한 `ssm:SendCommand`, 대상의 Online 여부와 실행 결과를 확인할 최소 조회 권한만 가집니다. 애플리케이션 S3 key나 EC2 instance role을 GitHub에 공유하지 않습니다.

EC2 명령은 원격 `develop`이 workflow의 정확한 commit인지, 서버의 추적 파일이 깨끗한지 확인하고 fast-forward만 허용합니다. 이후 commit SHA로 공용 Backend image를 한 번 빌드해 API와 evaluation worker를 함께 교체합니다. `/legal/terms?lang=en` 응답과 두 container의 실행 상태가 제한 시간 안에 정상이어야 성공합니다.

교체 전 image는 `rollback-<UTC 시각>` tag로 보존합니다. 새 image의 명령 실행 또는 health check가 실패하면 API와 Worker를 직전 image로 되돌리고 workflow를 실패 처리합니다. 이 rollback은 database migration이나 서버 Git checkout을 되돌리지 않으므로, Flyway migration은 항상 이전 애플리케이션과 호환되는 순방향 변경이어야 합니다.

배포 스크립트 회귀 검증:

```bash
./scripts/tests/deploy-backend-test.sh
```

이 테스트는 AWS나 운영 Docker를 호출하지 않고 mock command로 잘못된 SHA·dirty checkout 차단, 정확한 image tag 배포와 health check 실패 rollback을 확인합니다. 실제 OIDC·SSM·운영 네트워크는 첫 production 실행에서 별도로 검증합니다.

## 테스트 경계

- 단위 테스트: 입력 검증, 변환 규칙, 응답 파싱과 서비스 상태 전이
- Spring/JPA 테스트: 작업 생성의 멱등성, 쿼터 경쟁, 결과 저장과 상태 전이
- Flutter 테스트: 화면 동작, API 계약, 세션과 폴링 처리
- 외부 통합 테스트: 실제 외부 서비스와의 연결. 별도 `externalIntegrationTest` 작업을 사용하며 자격증명과 비용을 확인한 뒤 실행합니다.

H2와 외부 서비스 대역을 사용하는 테스트는 MySQL의 실제 잠금·실행 계획이나 Azure·S3 응답시간을 검증한 것으로 해석하지 않습니다. JaCoCo 보고서가 존재하는 것과 커버리지 임계값을 강제하는 것은 별개입니다.

법무 문서를 변경할 때 `LegalDocumentSourceSyncTest`로 문서 원본과 서버 제공 리소스의 일치를 확인합니다. 기기 녹음·Presigned PUT·로그인 capability는 해당 플랫폼에서 별도 실행 확인이 필요합니다.

## 문제를 재현할 때

실패 명령, 재현 조건, 비밀값을 제거한 핵심 오류, 실행 환경을 먼저 구분합니다. 외부 서비스 장애와 입력 오류를 섞지 않고 동일 조건의 회귀 테스트로 확인합니다.

[문제 해결 사례](../engineering/case-studies.md) · [오류 코드](../api/error-codes.md)
