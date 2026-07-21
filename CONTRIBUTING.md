# Contributing to LingKo

## 작업 기준

현재 문서 업데이트와 최신 기능 검토는 `main` 브랜치를 기준으로 합니다. 저장소 기본 브랜치 `develop`은 최신 `main`과 이력이 갈라져 있으므로 새 기능의 base로 사용하지 않습니다. 브랜치 정리 전까지 PR base를 반드시 확인합니다.

## 브랜치 이름

```text
feat/<topic>
fix/<topic>
test/<topic>
docs/<topic>
chore/<topic>
```

하나의 브랜치는 하나의 변경 목적만 다룹니다.

## 커밋

Conventional Commit 형태를 권장합니다.

```text
feat: add authenticated evaluation persistence
fix: reject malformed wav headers
test: cover quota concurrency
docs: update authentication flow
chore: configure CI cache
```

불필요한 생성 파일, 로그, 로컬 계획 파일은 커밋하지 않습니다.

## PR 작성

PR 본문에는 다음을 포함합니다.

```markdown
## Summary
- 무엇을 왜 변경했는지

## Test plan
- 실제 실행한 명령

## Risks / Follow-up
- 남은 위험과 후속 작업

## Documentation
- 변경한 문서 또는 변경 불필요 이유
```

## 검증

### Backend 변경

```bash
cd backend
./gradlew test
./gradlew integrationTest
```

외부 연동을 변경했다면 필요한 환경에서:

```bash
./gradlew externalIntegrationTest
```

### Flutter 변경

```bash
cd app
flutter analyze
flutter test
```

Android/iOS 권한·플러그인·빌드 설정 변경 시 실제 빌드와 가능하면 실기기 점검을 수행합니다.

## 문서 동기화

다음 변경은 같은 PR에서 문서를 수정해야 합니다.

- API 경로·인증·요청·응답·오류
- DB 테이블·컬럼·제약·마이그레이션
- 환경변수·실행 명령
- 외부 서비스와 아키텍처
- 운영·복구 절차
- 보안·개인정보 처리

## 코드 리뷰 체크

- 인증 사용자 경계가 올바른가
- 외부 입력을 검증하는가
- 트랜잭션·동시성·중복 요청을 고려했는가
- 비밀값과 개인정보가 로그에 노출되지 않는가
- 실패 경로 테스트가 있는가
- 앱과 백엔드 계약이 일치하는가
- 문서가 현재 코드와 일치하는가

## DB 변경

- 기존에 적용된 Flyway 파일을 수정하지 않습니다.
- 신규 버전 파일을 추가합니다.
- 기존 데이터와 롤백 가능한 배포 순서를 검토합니다.

## 비밀정보

`.env`, API 키, 토큰, 비밀번호, OAuth Secret을 커밋하지 않습니다. 노출이 의심되면 커밋 삭제만으로 끝내지 말고 실제 키를 폐기하고 재발급합니다.
