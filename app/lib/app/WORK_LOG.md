# 작업 이력

## 2026-07-20 - 인증 게이트와 quota 상태 연결 이력 이전

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 앱 시작 시 저장 세션을 복원하고 비로그인 사용자를 로그인 화면으로 제한했다. 로그인 세션과 quota 조회 상태를 앱 화면 흐름에 연결했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 실제 기기의 secure storage 세션 복원 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
