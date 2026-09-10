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
