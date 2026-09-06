# 작업 이력

## 2026-09-05 - 운영 서버 `해` 가이드 원본 및 응답 기록 저장

- 변경 파일: `server-hae-mouth.png`, `server-hae-tongue.png`, `server-hae-guide.json`, `WORK_LOG.md`
- 내용: 운영 `/api/pronunciation/prepare` 응답에서 `해`의 실제 mouth/tongue URL을 확인하고 S3 원본 두 장과 확인한 응답 필드를 저장했다.
- 검증: HTTP 응답 `guideStatus=AVAILABLE`, 두 PNG 다운로드 성공 및 시각 확인
- 리스크: 운영 mapping 변경 시 기록과 이미지 동기화 필요

## 2026-09-05 - `해` 입술·혀 가이드 소스 추가

- 변경 파일: `hae-lips-front.png`, `hae-tongue-side.png`, `WORK_LOG.md`
- 내용: V2의 조음 가이드 화면에 사용할 정면 입술 사진과 측면 혀 위치 교육 일러스트를 텍스트 없이 생성했다.
- 검증: 이미지에 문구·로고·워터마크가 없는지 시각 확인
- 리스크: 서버의 실제 `해` 가이드 미디어와 출시 전 최종 대조 필요

## 2026-09-05 - LingKo App Store 배경 비주얼 생성

- 변경 파일: `lingko-key-visual.png`, `WORK_LOG.md`
- 내용: LingKo의 Blue·Mint 팔레트와 발음·음성 흐름을 표현하는 텍스트 없는 추상 배경을 내장 이미지 생성 도구로 제작했다.
- 검증: 텍스트·로고·기기 UI가 없는지 시각 확인, 최종 5개 합성 이미지에 적용
- 리스크: 없음
