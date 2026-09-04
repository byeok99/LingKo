# 작업 이력

## 2026-08-12 - 로그인 화면 Apple 상태 동기화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: iOS placeholder를 활성 버튼으로 갱신하고 Android 미노출 범위를 명시했다.
- 검증: 로그인 widget 구현·테스트와 대조
- 리스크: 실기기 시각·접근성 확인 필요

## 2026-08-07 - 새 06 Result 구현 계약 동기화

- 변경 파일: `README.md`
- 내용: 사용자가 갱신한 `LingKo Blue Merged.dc.html`을 원본 그대로 유지하고, 활성 구현 기준에 80+/60+/60 미만 점수색과 compact 발음 카드·세로 어절 목록·하단 고정 CTA를 기록했다.
- 검증: HTML과 Flutter Result 구조 대조, `flutter analyze`, `flutter test --coverage` 90개 통과(라인 80.40%)
- 리스크: 브라우저 시안과 실제 기기 렌더링의 최종 육안 비교 필요

## 2026-08-07 - Home 취약 발음 타일 색상 기준 변경

- 변경 파일: `LingKo Blue Merged.dc.html`, `README.md`
- 내용: 첨부 시안을 새 기준으로 삼아 Home 취약 음절 tile을 테두리 없는 연한 붉은 면과 저채도 붉은 음절로 갱신했다. 작은 보조 글자는 시안과 가까운 범위에서 접근성 대비를 확보한 값으로 기록했다.
- 검증: HTML의 기존 Home 파란 tile 색상 잔존 여부 검색, `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.49%)
- 리스크: 브라우저·실기기 색상 프로필에 따른 미세한 차이는 수동 확인 필요

## 2026-08-06 - LingKo Blue 시안 분석과 구현 기준 정리

- 변경 파일: `LingKo Blue Merged.dc.html`, `README.md`, `WORK_LOG.md`
- 내용: 11개 화면의 색·타이포·곡률·그림자·정보 구조를 현재 Flutter 기능과 대조하고, 구현한 공통 계약과 데이터 경계 및 의도적으로 제외한 미연결 동작을 기록했다.
- 검증: HTML 전체 구조·렌더링 이미지와 Flutter 구현 대조, `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.64%)
- 리스크: 실제 기기의 light/dark 렌더링, 녹음·TTS·Google 로그인·guide media 재생은 수동 확인 필요
