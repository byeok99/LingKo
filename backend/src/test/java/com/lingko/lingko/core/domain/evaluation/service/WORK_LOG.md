# 작업 이력

## 2026-08-03 - 가이드 DB cache 회귀 테스트

- 변경 파일: `GuideMediaResolverTest.java`, `WORK_LOG.md`
- 내용: 저장된 MP4는 외부 생성을 건너뛰고 새 MP4는 기존 입·혀 URL을 보존하며 저장하는 계약을 추가했다.
- 검증: 대상 테스트와 Backend 전체 단위 테스트 통과
- 리스크: 실제 MySQL upsert 결과는 배포 migration에서 확인 필요

## 2026-07-30 - 가이드 매체 resolver 회귀 테스트

- 변경 파일: `GuideMediaResolverTest.java`, `WORK_LOG.md`
- 내용: `김`의 입·혀 MP4 생성, 단일 프레임 PNG 유지, 영상 생성 실패 시 첫 PNG fallback 계약을 검증했다.
- 검증: 대상 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 실제 Replicate·S3·FFmpeg 호출은 mock 처리
