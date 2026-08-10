# 작업 이력

## 2026-08-09 - 가이드 생성 기능 범위 보안 상태 반영

- 변경 파일: `product-and-scope.md`, `WORK_LOG.md`
- 내용: 가이드 job이 일반 앱 기능이 아니라 기본 비활성화된 내부 service용이며 memory 상태라는 현재 범위를 명확히 했다.
- 검증: 구현·API 문서와 대조
- 리스크: #42 영속화 전에는 서버 재시작 시 상태 유실

## 2026-08-08 - 광고 보상 구현 범위 갱신

- 변경 파일: `product-and-scope.md`, `WORK_LOG.md`
- 내용: callback UI만 있던 과거 설명을 AdMob·UMP·인증 멱등 지급 완료와 운영 SSV 미구현 상태로 교체했다.
- 검증: 앱·Backend 구현 및 광고 관련 활성 문서와 대조
- 리스크: 운영 광고 활성화 전 Google SSV 검증이 필요하다

## 2026-08-04 - 구현 범위에서 언어 설정 제거

- 변경 파일: `product-and-scope.md`
- 내용: 구현됨 목록에서 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 구현 범위에서 표시 언어 제거

- 변경 파일: `product-and-scope.md`
- 내용: 구현됨 목록의 '표시 언어·모국어 설정'을 '학습자 모국어 설정'으로 정정했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-07-29 - 회원 탈퇴·음성 삭제 구현 범위 반영

- 변경 파일: `product-and-scope.md`, `WORK_LOG.md`
- 내용: 앱 내 회원 탈퇴와 평가 원본 삭제·미제출 객체 Lifecycle을 구현 완료 범위로 이동했다.
- 검증: 앱·Backend 구현과 상태 대조
- 리스크: 실제 AWS 적용은 운영 배포 단계에서 확인 필요

## 2026-07-24 - Refresh Token 구현 범위 정합화

- 변경 파일: `product-and-scope.md`, `WORK_LOG.md`
- 내용: Refresh Token 회전·재사용 탐지·로그아웃·모바일 자동 갱신을 구현 완료 범위로 이동하고 부분 구현 설명을 제거했다.
- 검증: 인증 구현·요구사항·architecture 문서와 상태 대조, `git diff --check`
- 리스크: 전체 기기 로그아웃은 현재 범위에 포함되지 않음
## 2026-08-03 - 제품 범위의 충전 정책 갱신

- 변경 파일: `product-and-scope.md`
- 내용: 자정 5회 초기화를 폐지하고 시간 충전 및 광고 확장 경계를 반영했다.
- 검증: 요구사항 및 구현 대조
- 리스크: 광고 연동은 범위 밖 후속 작업
