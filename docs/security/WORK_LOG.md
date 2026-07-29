# 작업 이력

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
