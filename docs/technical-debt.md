# 기술 부채와 운영 전 위험

이 문서는 코드에서 확인되는 임시 구현과 후속 작업을 추적합니다. 완료 시 코드·테스트·관련 문서를 함께 갱신합니다.

## P0: 운영 전 필수

| 항목 | 현재 상태 | 위험 | 완료 기준 |
|---|---|---|---|
| 가이드 작업 인증 | 생성·조회 API 공개 | 비용 남용 | 관리자/서비스 권한 적용 |
| 가이드 작업 영속화 | `ConcurrentHashMap` 사용 | 재시작·다중 인스턴스에서 유실 | DB/큐 기반 상태·재시도 |

## P1: 안정성

- Azure·Replicate·S3 호출별 타임아웃, 재시도, 지수 백오프
- 외부 서비스 회로 차단기
- 평가 요청 멱등성 및 중복 업로드 처리
- 가이드 작업 worker 동시성·메모리·디스크 제한
- Spring Boot Actuator와 readiness/liveness
- 구조화 로그, 요청 ID, 메트릭, 알림
- DB 자동 백업과 복구 훈련
- API OpenAPI 자동 생성

## P2: 유지보수성

- Controller마다 중복된 Bearer 토큰 해석을 인증 필터·ArgumentResolver로 통합
- Flutter 앱 상태 증가에 따른 상태 관리 방식 재평가
- 오래된 mock 데이터와 미사용 코드 제거
- `description = 'Demo project for Spring Boot'`, Flutter 기본 description 등 프로젝트 메타데이터 교체
- 평가 관련 `VideoGenerationException` 명칭을 실제 책임에 맞게 분리
- `sentence_id`의 명시적 FK 또는 스냅샷 전략 결정
- 테스트 커버리지 기준을 모듈·핵심 도메인별로 설정

## 의사결정이 필요한 항목

1. 생성 가이드를 사용자별로 만들지 공용 캐시로 관리할지
2. 발음 평가를 동기 HTTP로 유지할지 비동기 작업으로 전환할지
## 완료된 의사결정

- 브랜치 전략: [ADR-0005](architecture/adr/0005-branch-strategy.md)에 따라 `develop`을 통합 브랜치, `main`을 릴리스 브랜치로 사용합니다.
- Refresh Token 정책: DB에 현재 토큰의 SHA-256 해시를 저장하고 원자적 회전, 이전 토큰 재사용 시 현재 기기 세션 폐기, 절대 만료, 앱의 401 후 1회 자동 갱신을 적용합니다. 운영 전 [#60](https://github.com/byeok99/LingKo/issues/60) 실제 만료 기반 실기기 E2E와 [#62](https://github.com/byeok99/LingKo/issues/62) 동시 DB 부하 검증을 수행합니다.
- 평가 쿼터 정책: 외부 평가 전에 무료 우선으로 횟수를 예약하고, 성공 시 결과 저장과 함께 사용량으로 확정하며 외부 평가·DB 저장 실패 시 동일 날짜와 종류의 예약을 복구합니다. [ADR-0006](architecture/adr/0006-atomic-practice-quota-transitions.md)에 따라 예약·확정·복구는 조건부 원자 UPDATE로 처리하고, 최초 행 생성만 짧은 사용자 부모 lock으로 직렬화합니다. 비정상 종료로 남은 예약의 회수 정책은 후속 작업입니다.
- 음성 보존·탈퇴 정책: 평가 성공·최종 실패 후 원본을 삭제하고 미제출·삭제 실패 객체는 S3 1일 Lifecycle로 만료합니다. 회원 탈퇴는 현재 Access·Refresh Token을 재확인하고 S3 current object·version·delete marker 삭제가 성공한 뒤 사용자 소유 DB 데이터를 하나의 transaction으로 삭제합니다. 실제 AWS Lifecycle·Versioning·탈퇴 E2E는 [#71](https://github.com/byeok99/LingKo/issues/71)에서 검증합니다.

## 완료 기록 방식

항목 완료 시 다음을 남깁니다.

- 관련 PR
- 선택한 설계와 ADR
- 테스트 명령과 결과
- 마이그레이션 또는 운영 변경
- 남은 위험
