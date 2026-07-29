# 작업 이력

## 2026-07-28 - 앱 아이콘 원본 추가

- 변경 파일: `app_icon.png`, `WORK_LOG.md`
- 내용: 기존 `logo.png` 워드마크를 훼손하지 않고 중앙 정사각형으로 잘라 Android·iOS 앱 아이콘 생성 원본을 추가했다.
- 검증: PNG 1086x1086, RGB 형식과 시각적 중앙 배치 확인
- 리스크: 작은 아이콘에서는 전체 워드마크 가독성이 제한될 수 있어 전용 심볼 제작 시 교체 권장

## 2026-07-20 - 앱 로고와 Google 로그인 로고 이력 이전

- 변경 파일: `logo.png`, `google_g_logo.png`, `WORK_LOG.md`
- 내용: 앱 시작 스플래시에 사용할 LingKo 로고와 Google 브랜드 페이지에서 제공한 full-color G 로그인 로고를 Flutter asset 경로에 추가했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: Google 브랜드 가이드 변경 시 로그인 asset 최신성 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
