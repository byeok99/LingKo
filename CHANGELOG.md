# Changelog

이 파일은 사용자·개발자에게 의미 있는 주요 변경을 기록합니다. 세부 구현은 GitHub PR을 기준으로 확인합니다.

## Unreleased

### Documentation

- 프로젝트 전체 README와 문서 인덱스 추가
- 제품 범위, 아키텍처, 인증·평가 흐름 문서화
- API, 오류 코드, 데이터 모델, 마이그레이션 정책 문서화
- 로컬 개발, 테스트, 운영, 보안 가이드 추가
- 기술 부채와 브랜치 전략 ADR 추가

## 2026-07

### Added

- Google OAuth 로그인과 Access/Refresh JWT
- 사용자 학습 설정 조회·수정
- 로그인 사용자의 연습 기록 조회
- 일일 연습 쿼터 모델과 조회 API
- 발음 가이드 이미지 표시와 비동기 가이드 생성 작업
- 일반 통합 테스트와 외부 서비스 통합 테스트 분리

### Changed

- 연습 기록 조회에서 사용자 ID 쿼리 대신 Bearer JWT 사용
- 발음 API 입력 검증 및 Flutter 실패 상태 테스트 강화

## 2026-06

### Added

- Flutter 추천 문장·자유 문장·녹음·평가 UI
- Spring Boot 추천 문장·발음 준비·평가 API
- Azure Speech 기반 평가
- MySQL/JPA/Flyway 평가 기록 모델
- Android/iOS 마이크 권한과 네이티브 녹음 설정
- S3·Replicate·FFmpeg 기반 가이드 처리 기반

### Changed

- WAV 형식과 크기 검증 강화
- 녹음 수명주기 및 임시 파일 정리 개선
