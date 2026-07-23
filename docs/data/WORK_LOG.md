# 작업 이력

## 2026-07-23 - Refresh Session 데이터 모델 추가

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: 사용자별 다중 기기 Refresh Session 관계, 해시·절대 만료·폐기 필드와 제약을 문서화했다.
- 검증: Flyway V6 migration 및 JPA entity와 구조 대조
- 리스크: 만료·폐기 세션 정리 배치는 후속 운영 작업
