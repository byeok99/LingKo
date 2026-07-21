# Architecture Decision Records

ADR은 중요한 기술 선택의 배경과 결과를 보존합니다.

| ADR | 상태 | 결정 |
|---|---|---|
| [0001](0001-flutter-mobile-client.md) | 승인 | Flutter로 iOS/Android 클라이언트 구성 |
| [0002](0002-google-oauth-and-jwt.md) | 승인 | Google OAuth 신원 확인 후 자체 JWT 발급 |
| [0003](0003-flyway-schema-management.md) | 승인 | Flyway 기반 DB 스키마 버전 관리 |
| [0004](0004-separate-external-integration-tests.md) | 승인 | 외부 서비스 통합 테스트 별도 실행 |
| [0005](0005-branch-strategy.md) | 제안 | 기준 브랜치를 `main`으로 단순화 |

## 작성 형식

```markdown
# ADR-NNNN 제목
- 상태: 제안 / 승인 / 폐기 / 대체
- 날짜

## 배경
## 결정
## 대안
## 결과
## 후속 작업
```
