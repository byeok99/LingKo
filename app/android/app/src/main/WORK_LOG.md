# 작업 이력

## 2026-07-28 - Android 앱 이름·아이콘 적용

- 변경 파일: `AndroidManifest.xml`, `res/mipmap-*/ic_launcher.png`, `WORK_LOG.md`
- 내용: 런처 표시명을 `LingKo`로 변경하고 기존 LingKo 로고 기반 아이콘을 Android density별 크기로 교체했다.
- 검증: Manifest 값, mipmap 48·72·96·144·192px 규격, Android Debug 빌드 확인
- 리스크: Adaptive Icon foreground/background 분리는 전용 심볼 확정 후 후속 적용 권장

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
