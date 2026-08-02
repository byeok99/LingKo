# 작업 이력

## 2026-07-30 - Practice 문장 정규화 공통화

- 변경 파일: `practice_sentence_normalizer.dart`, `WORK_LOG.md`
- 내용: Unicode P·S 범주 제거, 연속 공백 축약과 IME caret 보존을 앱 공통 함수·입력 formatter로 분리했다.
- 검증: 구현 전 회귀 테스트 실패 확인, `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 없음
