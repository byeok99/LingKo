## 2026-07-20 - Google OAuth backend env 설정

- 변경 파일: `.env`, `WORK_LOG.md`
- 내용: 백엔드 Google ID token 검증에 사용할 Web OAuth Client ID를 `GOOGLE_CLIENT_ID`에 설정했다. 현재 앱 로그인 방식에서는 `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`를 사용하지 않는다.
- 검증: 값 노출 없이 `.env`의 `GOOGLE_CLIENT_ID` 설정 여부를 확인함
- 리스크: `JWT_SECRET_KEY`가 아직 비어 있어 실제 로그인 테스트 전에 32자 이상의 secret을 설정해야 함
