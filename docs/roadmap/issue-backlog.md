# 출시·성능 Issue Backlog

이 문서는 출시와 확장성 작업을 GitHub Issue 기준으로 한눈에 확인하기 위한 인덱스입니다. 세부 완료 조건과 구현 범위는 각 Issue 본문을 따릅니다.

## P0: 공개 출시 차단 항목

| 순서 | Issue | 목적 | 주요 의존성 |
|---:|---|---|---|
| 1 | [#36 평가 API JWT 인증](https://github.com/byeok99/LingKo/issues/36) | 사용자 경계 확립 | 없음 |
| 2 | [#37 평가·쿼터·저장 통합](https://github.com/byeok99/LingKo/issues/37) | 핵심 유스케이스 완결 | #36 |
| 3 | [#38 쿼터 동시성](https://github.com/byeok99/LingKo/issues/38) | 초과 사용·유실 업데이트 방지 | #37 |
| 4 | [#39 Idempotency](https://github.com/byeok99/LingKo/issues/39) | 재시도 중복 비용·저장 방지 | #37, #38 |
| 5 | [#40 Refresh Token](https://github.com/byeok99/LingKo/issues/40) | 안전한 세션 갱신과 폐기 | #36 |
| 6 | [#43 음성 보존·삭제·탈퇴](https://github.com/byeok99/LingKo/issues/43) | 개인정보와 스토어 요구사항 충족 | #37 |
| 7 | [#41 가이드 API 접근 제어](https://github.com/byeok99/LingKo/issues/41) | 비용·자원 남용 차단 | #36 |
| 8 | [#51 Flutter 스토어 출시](https://github.com/byeok99/LingKo/issues/51) | Android/iOS 실제 배포 준비 | #40, #43 |

## P1: 안정적인 운영과 초기 확장

| 추천 순서 | Issue | 목적 |
|---:|---|---|
| 1 | [#44 외부 서비스 복원력](https://github.com/byeok99/LingKo/issues/44) | 타임아웃·재시도·장애 전파 차단 |
| 2 | [#48 관측성](https://github.com/byeok99/LingKo/issues/48) | 로그·메트릭·알림·헬스체크 |
| 3 | [#49 CI/CD](https://github.com/byeok99/LingKo/issues/49) | 자동 검증·배포·롤백 |
| 4 | [#50 백업·복구](https://github.com/byeok99/LingKo/issues/50) | DB·S3 데이터 복구 가능성 검증 |
| 5 | [#45 기록 조회 최적화](https://github.com/byeok99/LingKo/issues/45) | N+1 제거와 Cursor 페이지네이션 |
| 6 | [#46 평가 저장 최적화](https://github.com/byeok99/LingKo/issues/46) | 음절 반복 쿼리와 저장 비용 감소 |
| 7 | [#52 SLO·부하 테스트](https://github.com/byeok99/LingKo/issues/52) | 안전 처리량과 Scale-out 기준 확보 |
| 8 | [#42 가이드 Worker](https://github.com/byeok99/LingKo/issues/42) | 작업 영속화와 API 자원 분리 |

## P2: 트래픽 증가 전 구조 확장

| Issue | 목적 | 진행 조건 |
|---|---|---|
| [#47 S3 직접 업로드·비동기 평가](https://github.com/byeok99/LingKo/issues/47) | API 서버에서 큰 파일과 장시간 외부 평가 분리 | #37, #39, #44 완료 후 |

## 한 번에 하나씩 해결하는 방법

각 작업은 다음 작은 단계로 나눕니다.

1. 실패 사례를 재현하는 테스트 작성
2. 정책 또는 설계 선택
3. 최소 구현
4. 동시성·장애·보안 테스트
5. 성능 또는 SQL 측정
6. 관련 문서와 ADR 갱신
7. PR 본문에 남은 위험 기록

## 권장 첫 작업

첫 작업은 [#36](https://github.com/byeok99/LingKo/issues/36)입니다. 인증 사용자 객체가 안정적으로 전달되어야 쿼터, 저장, Idempotency, 탈퇴 기능을 올바른 사용자 기준으로 연결할 수 있습니다.

## 관련 문서

- [출시 로드맵](release-roadmap.md)
- [성능·확장성 계획](../performance/scalability-plan.md)
- [기술 부채](../technical-debt.md)
- [운영 Runbook](../operations/operations-runbook.md)
- [보안·개인정보](../security/security-and-privacy.md)
