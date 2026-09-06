# 작업 이력

## 2026-09-05 - 현재 WordSyllableExplorer가 적용된 Result 원본 재캡처

- 변경 파일: `04-result-current.png`, `WORK_LOG.md`
- 내용: 이전 마케팅 원본의 단일 `구` 칩 대신 현재 앱 구현대로 선택 어절 `한구거를`의 `한·구·거·를` 음절 칩 전체와 선택 행 디자인을 Flutter에서 다시 렌더링했다.
- 검증: 전용 Flutter golden 캡처 테스트 통과, 현재 `ResultScreen`·`WordSyllableExplorer` 소스와 시각 대조 완료
- 리스크: Result UI가 변경되면 다시 캡처해야 함

## 2026-09-05 - 운영 서버 가이드가 적용된 실제 GuideSheet 캡처

- 변경 파일: `05-guide-server.png`, `WORK_LOG.md`
- 내용: 운영 `/api/pronunciation/prepare`가 `해`에 반환한 입·혀 PNG를 실제 Flutter `GuideSheet`에 주입하고 Result 화면 위 bottom sheet 상태로 캡처했다.
- 검증: 전용 Flutter golden 캡처 테스트 통과, 입·혀 두 이미지 및 실제 앱 라벨·note 시각 확인
- 리스크: 서버 mapping 변경 시 다시 캡처해야 함

## 2026-09-05 - App Store용 실제 Flutter 화면 렌더링

- 변경 파일: `01-home.png`, `02-practice.png`, `03-recording.png`, `04-result.png`, `05-guide.png`, `WORK_LOG.md`
- 내용: Home부터 음절 가이드까지 실제 Flutter 위젯을 가상 학습 데이터와 시스템 한글 폰트로 렌더링했다.
- 검증: 전용 widget rendering test 5개 통과, 글꼴·아이콘·레이아웃 시각 확인
- 리스크: 마케팅 샘플 데이터이며 실제 사용자 정보로 교체하면 안 됨
