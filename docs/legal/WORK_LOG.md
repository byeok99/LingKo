# 작업 이력

## 2026-09-04 - Profile 설정 구현 현황 갱신

- 변경 파일: `README.md`, `privacy-policy.ko.md`, `privacy-policy.en.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`, `WORK_LOG.md`
- 내용: Profile에는 약관·처리방침·회원 탈퇴만 남기고, 광고 개인정보 선택은 CMP·기기 OS 설정으로, 문의와 마케팅 동의 변경은 이메일로 안내하도록 한·영 약관과 처리방침을 개정했다.
- 검증: 앱 구현·서빙 사본·동의 버전 대조
- 리스크: 기존 동의 사용자는 개정 문서 재동의 대상이다.

## 2026-09-04 - Apple 로그인 capability 운영 상태 갱신

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: Apple Developer App ID capability와 development provisioning 완료를 반영하고, 실제 로그인 E2E와 탈퇴 revocation을 남은 출시 항목으로 구분했다.
- 검증: 실기기 서명 binary의 Sign in with Apple entitlement 확인
- 리스크: 실제 Apple 승인·Backend 로그인과 authorization code revocation 미검증

## 2026-09-04 - iOS AdMob 운영 설정 상태 갱신

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: iOS 운영 App·Rewarded Ad Unit ID와 Backend SSV allowlist가 설정된 현재 상태를 반영하고, AdMob console callback 및 실기기 E2E를 남은 항목으로 분리했다.
- 검증: 앱 Release 바이너리의 광고 ID 포함과 운영 Backend allowlist 일치 확인
- 리스크: 실제 Google SSV callback 도착·지급은 실기기 광고 완료 전까지 미확인

## 2026-09-03 - 법무 문서 인앱 열람 방식 반영

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 동의 화면과 Profile이 공개 법무 URL을 인앱 WebView로 표시하는 현재 구현을 법무 준비 상태에 반영했다. 스토어 제출용 공개 URL은 그대로 유지한다.
- 검증: Flutter 문서 URL·실행 모드 테스트와 대조
- 리스크: 법무 문서 내용의 기존 미구현 고지 항목은 별도 출시 차단 요인으로 남아 있음

## 2026-08-12 - Apple 로그인 법무 구현 상태 갱신

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: iOS 코드 구현 완료와 authorization code 보관·회원 탈퇴 revocation 미완료를 구분했다.
- 검증: 인증·탈퇴 구현과 대조
- 리스크: Apple 승인 revocation 완료 전 출시 요구사항 미충족

## 2026-08-12 - AdMob SSV 구현 상태 동기화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: client callback 기반 테스트 지급 설명을 1회성 session과 Google SSV 검증 완료 상태로 교체하고 운영 설정·E2E를 후속 위험으로 분리했다.
- 검증: 앱·Backend SSV 구현 및 API 문서와 대조
- 리스크: 운영 공개 HTTPS callback과 실제 광고 단위 E2E 필요

## 2026-08-12 - Azure 리전 확정으로 자리표시자 전량 제거

- 변경 파일: `privacy-policy.ko.md`, `privacy-policy.en.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`, `README.md`
- 내용: Azure Speech 리전을 `koreacentral`(한국 중부)로 확정해 마지막 자리표시자를 채웠다. AWS·Azure가 모두 한국 리전이므로 국외 이전 대상은 Replicate(미국)와 Google(미국) 둘뿐이며 기존 표가 그대로 유효하다. 4개 문서의 자리표시자가 0건이 됐다.
- 문서 머리말의 "검토 대기" 배너를 "기재 값 확정, 변호사 검토는 생략" 상태로 바꿨다. 이 배너는 서빙 시점에 제거되므로 이용자에게는 노출되지 않는다.
- 검증: `./gradlew test --tests '*Legal*'` 통과. `backend/src/main/resources/legal/` 사본 동기화 후 `LegalDocumentSourceSyncTest` 통과
- 리스크: 변호사 검토를 생략하기로 해, README의 「변호사 검토가 필요한 판단」 항목(음성의 생체정보 해당 여부, 무료 서비스 책임 제한 조항의 유효성 등)은 검증 없이 현재 판단대로 게시된다. Azure의 음성 학습 이용 여부는 여전히 미확인이며 문서는 학습 미사용을 전제로 한다

## 2026-08-12 - 개인 운영자 기준으로 사업자 정보 확정

- 변경 파일: `privacy-policy.ko.md`, `privacy-policy.en.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`, `README.md`
- 내용: 운영자가 법인이 아닌 개인으로 확정돼 문서 구조를 바꿨다. 상호·대표자를 이상벽(LEE SANG BYEOK) 실명으로 두고, 사업자등록번호·사업장 주소·전화 항목은 값을 채우는 대신 **삭제**했다. 개인 주소와 번호를 공개하지 않고 `maplebyeok@gmail.com` 단일 창구로 운영하기로 했으며, 그 사실 자체를 문서에 명시해 항목 누락으로 보이지 않게 했다. 개인정보 보호책임자는 「개인정보 보호법」 제31조 제2항에 따라 운영자 본인이 겸임하고, 별도 부서·운영시간 항목은 1인 운영과 맞지 않아 CPO 연락처로 통합했다.
- EU 배포 제외 결정 반영: EU 대리인 절을 삭제하고 "EEA·영국에 배포하지 않으며 대상으로 하지 않는다"는 명시로 바꿨다. GDPR 조문에 의존하던 서술(회신 1개월, 72시간 통지, Art. 4(14)·5(1)(e)·22·45, 적정성 결정)을 「개인정보 보호법」 기준으로 교체했다. 만 16세 기준은 유지하되 근거를 "GDPR 16세"가 아니라 "법정대리인 동의 확인 절차를 갖추지 않은 1인 운영이라 법정 14세보다 엄격한 자체 기준"으로 다시 썼다. 광고 지역별 동의도 EEA 명시 대신 "사전 동의를 요구하는 지역"으로 일반화했다.
- 보관 기간(휴면 12개월·삭제 6개월·로그 3개월·마케팅 3년)과 AWS S3 `ap-northeast-2`는 확정값으로 표기에서 자리표시자를 제거했다.
- 검증: `./gradlew test --tests '*Legal*'` 통과. `backend/src/main/resources/legal/` 사본을 동기화해 `LegalDocumentSourceSyncTest` 통과 확인
- 리스크: **Azure Speech 리전이 유일한 미확정 값**이다. 국외면 국외 이전 표에 Microsoft를 추가해야 한다. Azure의 음성 학습 이용 여부도 미확인 상태에서 문서는 학습 미사용을 전제로 기재돼 있다. 휴면 배치는 여전히 미구현이라 기술 부채 P1에 등록했다

## 2026-08-08 - AdMob·UMP 법무 준비 상태 반영

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 테스트 광고와 UMP 연결 상태, 운영 전 실제 광고 식별자·ATT·처리방침 확정 필요사항을 추가했다.
- 검증: 앱 광고 설정 및 보안 문서와 대조
- 리스크: 법률 검토와 스토어별 동의 문구 확정이 남아 있다

## 2026-08-07 - 동의 저장·복원 세션 재동의 구현 상태 반영

- 변경 파일: `README.md`
- 내용: 동의 저장 API와 현재 버전 gate 구현 완료를 반영하고, 앱·서버 버전 상수 동시 갱신 규칙과 남은 마케팅 철회 기능을 구분했다.
- 검증: Backend·Flutter 구현 및 API 문서와 대조
- 리스크: 연령 확인, 마케팅 철회, 광고 CMP·ATT는 여전히 미구현

## 2026-08-07 - 문서 공개 URL 연결과 사본 관리 절차 기록

- 변경 파일: `README.md`
- 내용: 백엔드가 `/legal/terms`·`/legal/privacy`로 문서를 서빙하고 앱이 이를 브라우저로 연다는 사실을 구현 현황에 반영했다. 원본과 `backend/src/main/resources/legal/` 사본의 이중 관리 이유(Docker 빌드 컨텍스트 제약)와 문서를 고칠 때 밟아야 하는 3단계를 적었다.
- 검증: 백엔드·앱 구현과 대조
- 리스크: 사본 복사를 사람이 해야 한다. 빠뜨리면 sync 테스트가 잡는다

## 2026-08-07 - Profile 설정 항목 구현 상태 반영

- 변경 파일: `README.md`
- 내용: 설정 화면 5개 항목의 행이 Profile 탭에 모두 배치됐고, 약관·처리방침은 문서 열기 처리에 연결됐으며 회원 탈퇴는 기존 버튼이 동작한다는 점을 구현 현황 표에 반영했다. 광고 개인정보 설정과 문의는 기능이 없어 비활성 상태임을 함께 적었다.
- 검증: Profile 화면 구현과 대조
- 리스크: 문서 열람 경로가 없다는 항목은 그대로 남아 있다

## 2026-08-07 - 앱 동의 화면 구현 상태 반영

- 변경 파일: `README.md`
- 내용: 가입 동의 화면이 `app/lib/screens/consent_screen.dart`로 구현된 사실을 구현 현황 표에 반영하고, 아직 남은 항목(전문 열람 경로 없음, 동의 저장 API 없음, 연령 확인 수단 없음)을 분리해 적었다. 문서 시행일을 바꾸면 앱의 `consentDocumentVersion` 상수도 같이 올려야 한다는 연결 관계를 남겼다.
- 검증: 상대 경로와 앱 구현 대조
- 리스크: 전문 열람 경로가 없어 문서를 읽지 못한 채 동의하게 된다. 이 상태로는 출시할 수 없다

## 2026-08-07 - 관할별 3세트 폐기 후 공통 1세트로 재작성

- 변경 파일: `README.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`, `privacy-policy.ko.md`, `privacy-policy.en.md`. 삭제: `eu/`, `jp/`, 기존 한국 전용 2종
- 내용: 사용자가 확정한 사양(16세 이상, Google·Apple 로그인, 음성 최소 기간 보관·AI 학습 미사용, Google AdMob, 가입 시 필수 2건·선택 1건 동의, 설정 화면 5개 항목)으로 관할 구분 없는 공통 문서 1세트를 한국어·영어로 다시 작성했다. 지역별 차이(GDPR 권리와 법적 근거, EEA·영국·스위스 CMP 사전 동의, iOS ATT, 소비자 관할)는 문서 내 조건부 조항으로 흡수했다.
- 이전 세트 대비 주요 변경:
  - **AdMob 도입으로 광고 관련 기재를 전면 교체했다.** 이전 문서는 "광고 SDK 없음·광고식별자 미수집·행태정보 미처리"였으나, 광고식별자(AAID/IDFA)와 광고 상호작용 기록의 수집, Google LLC 위탁과 미국 국외 이전, 개인 맞춤 광고 해제 경로, 지역별 동의 절차를 새로 넣었다.
  - Apple 로그인과 이메일 가리기 릴레이 주소 처리를 추가했다.
  - 마케팅 수신 선택 동의와 철회 경로를 추가했다.
  - 음성 보관 문구를 "평가 종료 즉시 삭제"에서 "평가 수행에 필요한 최소 기간 보관 후 지체 없이 삭제"로 조정했다. 실제 동작(성공·최종 실패 시 삭제, 미제출분 1일 만료)은 그대로 기술했다.
  - 휴면 계정 정책, 업데이트 제공 의무, 경과실 상한 방식의 책임 제한은 이전 세트의 감사 결과를 유지했다.
- 검증: 4개 문서의 조항 번호 연속성, 한국어판·영어판 대응, 상대 링크 확인. 변호사 검토 미실시
- 리스크: **문서에 기재된 Apple 로그인, AdMob, CMP·ATT, 설정 화면 5개 항목, 마케팅 동의, 휴면 배치가 모두 미구현이다.** 이 상태로 공개하면 사실과 다른 고지가 된다. 구현 현황은 `README.md`의 표로 관리한다. 관할별 전용 문서를 없애면서 일본 APPI 第32条 공표 항목과 미국 CCPA 고지 항목은 담기지 않았다
