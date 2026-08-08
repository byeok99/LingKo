# 작업 이력

## 2026-08-07 - 법무 동의 보안 경계 문서화

- 변경 파일: `security-and-privacy.md`
- 내용: Bearer Token 사용자 귀속, 서버 기록 시각, 버전별 이력, fail-closed gate와 탈퇴 cascade를 기록했다.
- 검증: migration·service·Flutter shell과 대조
- 리스크: 인증 endpoint 공통 rate limit은 운영 전 과제

## 2026-08-07 - 외부 서비스 데이터 전달 항목의 처리방침 반영 상태 갱신

- 변경 파일: `security-and-privacy.md`, `WORK_LOG.md`
- 내용: "운영 전에 처리방침·약관에 반영해야 한다"고만 적혀 있던 항목을 `docs/legal/` 초안 반영 완료로 바꾸고, 초안 상태에서 남은 확인 항목(리전 확정, Azure 학습 이용 여부, DPA 체결, EU Representative, 변호사 검토)을 분리해 기록했다.
- 검증: `docs/legal/` 4개 문서 내용과 대조, 상대 링크 확인
- 리스크: 음성 학습 미사용은 아직 계약·설정으로 확인되지 않았고 문서에는 단언으로 기재되어 있다

## 2026-07-29 - 음성 1일 보존·회원 탈퇴 정책 확정

- 변경 파일: `security-and-privacy.md`, `WORK_LOG.md`
- 내용: 평가 종료 즉시 삭제, 미제출 객체 1일 만료와 S3 version 우선 삭제 후 DB transaction 정책을 명시했다.
- 검증: 저장소·탈퇴 서비스·Lifecycle 파일과 대조
- 리스크: 실제 AWS 적용은 #71, 개인정보처리방침 반영은 출시 작업에서 추적

## 2026-07-27 - 음성 직접 업로드 보안 정책 갱신

- 변경 파일: `security-and-privacy.md`, `WORK_LOG.md`
- 내용: 사용자별 S3 key, 제한 시간 URL, HEAD·WAV 이중 검증, 성공·최종 실패 삭제와 Lifecycle 필요성을 반영했다.
- 검증: S3 저장 경계·Worker 구현과 대조
- 리스크: AWS Bucket CORS·Lifecycle·공개 차단은 운영 설정 필요

## 2026-07-23 - Refresh Token 보안 정책 추가

- 변경 파일: `security-and-privacy.md`, `WORK_LOG.md`
- 내용: 원문 비저장, 회전, 재사용 탐지, 절대 만료, 단일 갱신과 폐기 세션 기반 Access Token 차단 정책을 명시했다.
- 검증: 인증 구현과 저장·폐기 정책 대조
- 리스크: JWT 서명 키 자동 회전과 전체 기기 로그아웃은 후속 작업
## 2026-08-03 - 광고 보상 신뢰 경계 명시

- 변경 파일: `security-and-privacy.md`
- 내용: UI callback이 임의로 쿼터를 지급하지 않고 서버 재조회만 수행하도록 기록했다.
- 검증: app callback 흐름 대조
- 리스크: 실제 보상 endpoint 구현 시 서버 측 검증 필요
