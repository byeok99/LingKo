# 보안·개인정보

## 보호 대상

- Google ID Token
- LingKo Access/Refresh JWT
- JWT 서명 키
- Google·Azure·Replicate·AWS 자격증명
- 사용자 이메일·이름·프로필 이미지
- 사용자가 녹음한 음성
- 평가 점수·인식 문장·취약 글자
- S3 미디어 URL

## 인증·토큰

- Google ID Token은 백엔드에서 audience와 유효성을 검증합니다.
- 백엔드는 자체 Access/Refresh JWT를 발급합니다.
- Flutter는 토큰을 `flutter_secure_storage`에 저장합니다.
- Bearer 토큰이 필요한 API는 사용자 ID 쿼리값 대신 JWT subject를 사용합니다.
- Refresh Token 원문은 서버에 저장하지 않고 SHA-256 해시만 저장합니다.
- Refresh Token은 사용할 때마다 회전하며 이전 토큰 재사용 시 해당 기기 세션을 폐기합니다.
- Access Token은 같은 `sid`의 활성 서버 세션을 확인하므로 로그아웃·재사용 탐지 후 즉시 거부됩니다.
- Refresh Token의 절대 만료는 로그인 시점부터 계산하며 회전으로 연장하지 않습니다.
- 앱은 동시 refresh를 하나로 합치고 보호 API 요청을 최대 한 번만 재시도합니다.

## 법무 동의 기록

- 동의 상태 조회·제출은 Bearer Token 인증을 요구하며 사용자 ID를 body에서 받지 않습니다.
- 사용자·문서 버전 조합은 DB 유일 제약으로 중복 기록을 막습니다.
- 앱이 보내는 동의 시각은 참고값으로만 보존하고 감사 기준 시각은 서버에서 기록합니다.
- 세션 복원 시 현재 문서 버전 기록을 먼저 확인하며, 조회 또는 저장 실패는 Home을 열지 않는
  fail-closed 방식으로 처리합니다.
- 문서 버전을 올리면 과거 기록을 삭제하지 않고 새 버전에 대한 재동의를 요구합니다.
- 회원 탈퇴 시 사용자 FK의 `ON DELETE CASCADE`로 동의 기록도 함께 삭제합니다.

### 운영 전 필수

- 키 ID(`kid`)와 JWT 키 회전
- 인증 실패 rate limit
- 계정 탈취 대응과 전체 세션 로그아웃

## 비밀정보

- 실제 값은 `.env`와 배포 환경의 Secret Manager에 저장합니다.
- `.env.example`에는 빈 값 또는 안전한 예시만 둡니다.
- 로그, PR, 이슈, 테스트 fixture, 스크린샷에 비밀값을 넣지 않습니다.
- 모바일 앱에는 Google Client Secret, JWT Secret, AWS Secret을 넣지 않습니다.
- 노출 가능성이 있으면 즉시 키 폐기·재발급·로그 조사합니다.

## 음성 업로드

앱은 인증된 업로드 티켓을 발급받아 사용자별 prefix의 비공개 S3 객체로 WAV를 직접 PUT합니다. AWS 자격증명은 앱에 전달하지 않고, presigned URL은 파일 크기와 `audio/wav` Content-Type을 포함해 제한 시간 동안만 유효합니다.

작업 생성 시 서버는 다음을 검증합니다.

- 최대 10MiB
- `.wav` 확장자와 허용 Content-Type
- 인증 사용자와 `evaluation-audio/{userId}/` object key 소유권
- S3 HEAD 기준 실제 크기와 Content-Type

Worker 다운로드 후에는 실제 바이트를 기준으로 다음을 다시 검증합니다.

- RIFF/WAVE 헤더
- PCM 형식
- mono 채널
- 16-bit
- 48kHz 이하 샘플링 레이트

운영 전 추가할 항목:

- 요청 rate limit
- Worker 임시 파일 디렉터리 용량·삭제 모니터링
- 악성 파일 스캔 필요성 검토
- 중복 업로드와 재생 공격 방지

## 외부 서비스 데이터 전달

발음 평가·가이드 생성 과정에서 텍스트, 음성, 이미지 URL이 외부 서비스로 전달될 수 있습니다.

다음 항목은 [처리방침·이용약관 초안](../legal/README.md)에 반영했습니다.

- 어떤 데이터가 어느 사업자에게 전달되는지
- 처리 목적과 보존 기간
- 국외 이전 여부
- 사용자 동의와 철회 방법
- 외부 서비스 장애·삭제 요청 처리

초안이므로 운영 전에 다음이 남아 있습니다.

- AWS S3·Azure Speech 실제 리전 확정. 리전이 국외이면 국외 이전 조항을 다시 씁니다.
- Azure Speech가 업로드된 음성을 모델 학습에 사용하지 않는지 계약·설정으로 확인. 현재 문서는 학습 미사용을 전제로 기재되어 있습니다.
- 수탁자별 처리위탁 계약과 GDPR Art. 28 DPA 체결
- EU Representative(GDPR Art. 27) 지정 여부 결정
- 한국·EU 양쪽 변호사 검토

## 데이터 보존·삭제

평가 Worker는 성공 또는 최종 실패 시 원본 S3 객체 삭제를 시도하고 로컬 임시 파일은 항상 삭제합니다. 앱이 업로드 후 작업을 제출하지 않거나 개별 삭제가 실패한 객체는 `backend/aws/s3-lifecycle.json`에 따라 `evaluation-audio/` prefix에서 1일 후 만료합니다. S3 Lifecycle 처리는 비동기이므로 정확히 24시간에 삭제된다고 보장하지 않으며 운영 버킷 적용 여부를 별도로 검증합니다.

| 데이터 | 적용 정책 |
|---|---|
| 사용자 계정 | 앱 확인과 현재 Access·Refresh Token 재확인 후 즉시 삭제 |
| 평가 기록 | 회원 탈퇴 시 평가 작업·결과·음절 점수 삭제 |
| 원본 음성 | 평가 성공·최종 실패 후 삭제, 미제출·삭제 실패 객체는 1일 Lifecycle 만료 |
| 생성 가이드 | 공용 콘텐츠 여부와 수명주기 |
| 로그 | 개인정보 마스킹과 제한된 보존 기간 |
| 평가 기회 | 회원 탈퇴 시 삭제 |
| 광고 보상 event receipt | 중복 지급 방지를 위해 보관하고 회원 탈퇴 시 삭제 |
| 약관 동의 기록 | 문서 버전·선택값·서버 기록 시각을 보존하고 회원 탈퇴 시 삭제 |

회원 탈퇴는 `evaluation-audio/{userId}/`의 현재 object, 모든 과거 version과 delete marker를 먼저 삭제합니다. S3 삭제가 실패하면 DB와 인증 세션을 보존하고 재시도 가능한 503을 반환하며, S3 삭제가 끝난 경우에만 하나의 DB transaction으로 Refresh 세션, 평가 작업·기록, 쿼터와 사용자 프로필을 삭제합니다. 공유 음절 기준 데이터는 사용자 개인정보가 아니므로 보존합니다. 실제 AWS Lifecycle·Versioning·탈퇴 E2E는 [#71](https://github.com/byeok99/LingKo/issues/71)에서 검증합니다.

## 로그 정책

로그에 남기지 않는 값:

- Authorization 헤더
- Google ID Token
- Access/Refresh Token
- API 키와 비밀번호
- 전체 음성 파일이나 Base64 데이터
- 불필요한 이메일·이름·인식 문장

권장 로그:

- 요청 ID
- API 경로·상태 코드·처리시간
- 내부 사용자 ID의 비식별 형태
- 외부 서비스 이름·오류 분류·처리시간
- 파일 크기와 검증 결과

## 권한

가이드 작업 생성·조회 HTTP API는 기본 비활성화합니다. 내부 도구가 필요한 환경에서만 32자 이상의 별도 service Secret으로 활성화하며, 앱의 일반 Bearer 사용자는 인증되더라도 `403`으로 거부합니다. 생성 요청은 내부 호출자별 고정 1분 window Rate Limit과 process별 최대 동시 실행 수를 함께 적용합니다.

입력 URL은 실제 생성 job 등록 전에 다음 경계를 통과해야 합니다.

- 항목당 최대 2,048자와 요청당 최대 10쌍
- HTTPS와 설정된 S3 bucket·Replicate delivery host allowlist
- DNS 결과의 loopback·사설·link-local 주소 거부
- redirect 미추적과 다운로드 최대 25MiB

감사 로그에는 caller 종류, job ID, 음절, 가이드 종류, source 개수만 남기고 service token과 외부 URL 원문은 기록하지 않습니다. `lingko.guide.jobs.requests`, `lingko.guide.jobs.active`, `lingko.guide.jobs.completed` Micrometer 지표로 admission 결과와 처리 결과를 집계하며 운영 alert 임계치는 배포 환경에서 별도로 정합니다.

## 광고 보상 신뢰 경계

- 앱은 UMP의 최신 동의 상태에서 `canRequestAds()`가 true일 때만 광고를 요청합니다.
- 앱은 광고 표시 전에 인증된 1회성 session token을 발급받아 Rewarded Ad `customData`에 설정합니다.
- 서버는 활성 Bearer session으로 사용자를 식별하고 수량 입력을 받지 않으며 event 하나당 1회만 지급합니다.
- 서버는 Google rotating public key를 최대 24시간만 cache하고 raw query의 ECDSA-SHA256 서명을 검증합니다. 서명 이후에도 허용 광고 단위, 보상 종류·수량, session 만료와 소유권을 확인합니다.
- Google의 전역 고유 `transaction_id`와 session row lock으로 재전송·동시 callback을 한 번만 지급합니다. 과거 client 직접 지급 endpoint는 `410 Gone`만 반환합니다.

## 보안 점검 체크리스트

- 의존성 취약점 검사
- Secret scan
- HTTPS 강제
- CORS 허용 출처 제한
- 오류 응답에 내부 스택·키·URL 미노출
- SQL injection·SSRF·파일 업로드 점검
- S3 공개 접근과 presigned URL 정책 점검
- JWT 만료·변조·알고리즘 검증 테스트
- 개인정보 삭제 테스트
