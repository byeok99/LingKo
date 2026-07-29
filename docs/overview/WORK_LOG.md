# 작업 이력

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
