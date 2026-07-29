# Architecture Decision Records

ADR은 중요한 기술 선택의 배경과 결과를 보존합니다.

| ADR | 상태 | 결정 |
|---|---|---|
| [0001](0001-flutter-mobile-client.md) | 승인 | Flutter로 iOS/Android 클라이언트 구성 |
| [0002](0002-google-oauth-and-jwt.md) | 승인 | Google OAuth 신원 확인 후 자체 JWT 발급 |
| [0003](0003-flyway-schema-management.md) | 승인 | Flyway 기반 DB 스키마 버전 관리 |
| [0004](0004-separate-external-integration-tests.md) | 승인 | 외부 서비스 통합 테스트 별도 실행 |
| [0005](0005-branch-strategy.md) | 승인 | `develop` 통합, `main` 릴리스 브랜치 운영 |
| [0006](0006-atomic-practice-quota-transitions.md) | 승인 | 조건부 DB UPDATE와 최초 생성용 짧은 부모 lock으로 쿼터 원자성 보장 |
| [0007](0007-s3-direct-upload-and-db-evaluation-worker.md) | 승인 | 비공개 S3 직접 업로드와 영속 DB Worker로 평가 요청 분리 |
| [0008](0008-sqs-independent-evaluation-workers.md) | 폐기 | SQS 기반 독립 평가 Worker 검토·구현 이력 |
| [0009](0009-independent-db-evaluation-worker.md) | 승인 | Queue 없이 DB polling Worker 한 개를 API와 별도 프로세스로 운영 |

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
