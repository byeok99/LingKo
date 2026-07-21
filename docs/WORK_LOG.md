## 2026-07-21 - 과거 로컬 문서 보존 위치 추가

- 변경 파일: `README.md`, `archive/README.md`, `archive/legacy/*`, `WORK_LOG.md`
- 내용: 최신 기준 문서와 구분하기 위해 과거에 추적 중단된 로컬 기획 및 앱 참고 문서를 `archive/legacy/`로 이동하고 문서 인덱스에 보관 위치를 추가했다.
- 검증: 문서 목록, 내부 상대 링크, `git diff --check` 확인
- 리스크: 보관 문서의 API 및 구현 설명은 현재 코드와 다를 수 있음

## 2026-07-20 - Phase 8.3 완료 상태 반영

- 변경 파일: `task-breakdown.md`, `WORK_LOG.md`
- 내용: Flutter quota UI 연결 작업 완료에 맞춰 Phase 8.3 상태를 `[x]`로 변경하고 실제 변경 파일, 검증 명령, 남은 리스크를 기록했다.
- 검증: 문서 변경으로 별도 빌드/테스트는 실행하지 않음. 앱 변경 검증은 `app/WORK_LOG.md`에 기록함.
- 리스크: 없음
