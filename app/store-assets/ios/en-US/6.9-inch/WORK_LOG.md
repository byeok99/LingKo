# 작업 이력

## 2026-09-05 - 실제 앱 UI·운영 서버 가이드 기반 V3 연결

- 변경 파일: `README.md`, `WORK_LOG.md`, `v3/`
- 내용: 실제 Flutter UI를 크게 배치하고 운영 서버가 반환한 `해` 입·혀 가이드를 사용한 `v3/`를 권장 업로드본으로 변경했다. `v2/`는 업로드 제외 비교본으로 명시했다.
- 검증: `v3/WORK_LOG.md` 참조
- 리스크: 운영 가이드 mapping 변경 시 V3 재생성 필요

## 2026-09-05 - 상위 앱 분석 기반 V2 세트 연결

- 변경 파일: `README.md`, `WORK_LOG.md`, `v2/`
- 내용: 기존 1차 세트를 비교본으로 보존하고, 점수와 조음 가이드를 구체화한 `v2/`를 권장 업로드본으로 연결했다.
- 검증: `v2/`의 별도 검증 이력 참조
- 리스크: 출시 전 실제 서버 가이드 미디어와 V2 교육 이미지의 일치 여부 확인 필요

## 2026-09-05 - iPhone 6.9-inch App Store 이미지 세트 제작

- 변경 파일: `README.md`, `01-speak-with-confidence.png`, `02-hear-the-right-pronunciation.png`, `03-record-speak-improve.png`, `04-know-what-to-fix.png`, `05-master-every-syllable.png`, `WORK_LOG.md`
- 내용: 실제 LingKo 화면과 Blue·Mint 음성 비주얼을 합성해 영문 App Store 스크린샷 5장을 제작했다.
- 검증: 5장 모두 1320×2868 PNG, alpha channel 없음, 시각 검수 및 문구 확인 완료
- 리스크: 앱 UI·기능·스토어 localization이 바뀌면 화면과 카피를 다시 동기화해야 함
