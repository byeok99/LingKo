# 작업 이력

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
