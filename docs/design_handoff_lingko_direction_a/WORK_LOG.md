# 작업 이력

## 2026-09-03 - Legal & privacy 표시 방식 갱신

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 약관·처리방침 행이 공개 URL을 외부 브라우저가 아니라 인앱 WebView로 표시하도록 바뀐 구현을 디자인 인계 문서에 반영했다.
- 검증: Flutter 문서 열기 서비스와 대조
- 리스크: 실제 iPhone의 modal 표시와 긴 문서 스크롤 수동 확인 필요

## 2026-08-12 - Profile 시안 서술을 실제 구현에 맞춤

- 변경 파일: `README.md`
- 내용: Profile Layout이 "About 섹션 2행(Audio & privacy / About LingKo 1.0.0)"으로 남아 있었으나 실제로는 Your content, Legal & privacy 4행, About 1행 구조다. 동작하지 않던 Audio & privacy가 실제 Privacy Policy 행으로 대체된 경위와, 광고·문의가 연결 전이라 비활성인 이유를 적었다. Language preferences 제외 근거에 V16 제거 사실을 덧붙였다.
- 검증: `profile_screen.dart`와 대조
- 리스크: 없음
