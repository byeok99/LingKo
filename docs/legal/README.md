# 서비스 법무 문서

앱과 서버에서 제공하는 이용약관 및 개인정보 처리방침입니다.

| 문서 | 한국어 | English |
|---|---|---|
| 이용약관 | [한국어](terms-of-service.ko.md) | [English](terms-of-service.en.md) |
| 개인정보 처리방침 | [한국어](privacy-policy.ko.md) | [English](privacy-policy.en.md) |

서버는 인증 없이 `GET /legal/{document}` 경로로 법무 문서를 제공합니다. 경로 값과 동의 요청 형식은 [API 레퍼런스](../api/api-reference.md)를 참고합니다.

## 소스와 서비스 응답의 일치

이 디렉터리의 문서와 백엔드 리소스의 제공본은 함께 관리합니다. `LegalDocumentSourceSyncTest`는 두 사본의 일치를 검사합니다. 문서 버전과 필수 동의 정책은 서버의 `LegalConsentPolicy` 및 앱의 서버 응답 처리 계약과 맞아야 합니다.

기술 문서 정리만으로 법무 본문이나 동의 버전을 변경하지 않습니다.

현행 법무·AI 고지 버전은 `2026-09-12`입니다. 일반 약관 확인과 별도로 AI 평가 전
수신자·항목·목적을 고지하고 허용을 받습니다. Profile에서 AI 허용을 철회할 수 있습니다.
공급자 보존 조건은 공식 서비스 문서를 근거로 안내하며 실제 구독 약정·리전·저장소
수명주기 설정의 일치는 배포자가 확인해야 합니다.
