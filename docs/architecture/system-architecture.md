# 시스템 아키텍처

## 전체 구성

```mermaid
flowchart LR
    subgraph Mobile[Flutter App]
      UI[Home / Practice / Result / Profile]
      API[API Clients]
      AUTH[Google Identity + Secure Storage]
      REC[Audio Recorder]
    end

    subgraph Backend[Spring Boot]
      CTRL[REST Controllers]
      DOM[Domain Services]
      JPA[JPA Repositories]
      JOB[Guide Job Service]
      EXT[External Adapters]
    end

    DB[(MySQL 8)]
    GOOGLE[Google OAuth]
    AZURE[Azure Speech]
    REP[Replicate]
    S3[AWS S3]
    FFMPEG[FFmpeg]

    UI --> API --> CTRL --> DOM --> JPA --> DB
    AUTH --> GOOGLE
    AUTH --> API
    REC --> API
    DOM --> EXT
    JOB --> EXT
    EXT --> AZURE
    EXT --> REP
    EXT --> S3
    EXT --> FFMPEG
```

## 컴포넌트 책임

| 컴포넌트 | 책임 |
|---|---|
| Flutter UI | 사용자 흐름, 로딩·오류 상태, 녹음 제어, 결과 표시 |
| Flutter API 계층 | JSON·multipart 요청, 응답 모델 변환, Bearer 토큰 전달 |
| 인증 서비스 | Google ID Token 획득, 백엔드 로그인, 세션 저장·삭제 |
| REST Controller | HTTP 계약·입력 검증·인증 토큰 해석 |
| Domain Service | 표준 발음, 평가, 기록, 설정, 쿼터, 작업 규칙 |
| JPA/Flyway | 영속 모델과 스키마 버전 관리 |
| 외부 어댑터 | Azure Speech, Replicate, S3, FFmpeg 호출 |

## 배포 단위

현재 저장소 기준 배포 단위는 두 개입니다.

1. Flutter 앱: Android/iOS 빌드 산출물
2. Spring Boot 백엔드: Java 17 JAR 또는 Docker 이미지

백엔드 Docker 이미지는 빌드 단계와 실행 단계를 분리하며 실행 이미지에 FFmpeg를 설치합니다. 로컬 Docker Compose는 MySQL과 백엔드를 함께 실행합니다.

## 동기·비동기 경계

### 동기

- 로그인
- 추천 문장 조회
- 자유 문장 준비
- 발음 평가
- 기록·설정·쿼터 조회

발음 평가는 외부 Azure 호출을 포함하므로 HTTP 요청 시간이 길어질 수 있습니다. 운영 전 타임아웃, 취소, 비동기 평가 전환 여부를 결정해야 합니다.

### 비동기

- 가이드 영상 생성 작업

현재 `ConcurrentHashMap`과 프로세스 내 Executor를 사용합니다. 서버 재시작 시 작업 상태가 사라지고 다중 인스턴스 간 공유되지 않습니다.

## 신뢰 경계

- 모바일에서 전달된 Google ID Token은 백엔드가 검증한 후 자체 JWT를 발급합니다.
- Bearer JWT가 필요한 API는 기록, 사용자 설정, 쿼터 조회입니다.
- 평가 업로드와 가이드 작업 API는 현재 인증 경계가 충분하지 않습니다.
- 외부 URL과 업로드 파일은 서버에서 형식과 크기를 검증해야 합니다.

## 주요 실패 지점

| 지점 | 영향 | 현재 대응 | 운영 전 보완 |
|---|---|---|---|
| MySQL 연결 실패 | 대부분 API 실패 | Docker healthcheck | 재시도, 알림, 백업 |
| Azure 실패 | 평가 실패 | 502 응답 | 타임아웃·재시도·회로 차단기 |
| Replicate 실패 | 영상 생성 실패 | 작업 FAILED | 영속 큐·백오프 |
| S3 실패 | 미디어 저장 실패 | 예외 처리 | 재시도·수명주기 정책 |
| FFmpeg 실패 | 영상 합성 실패 | 작업 FAILED | 리소스 제한·관측성 |
| 서버 재시작 | 인메모리 작업 소실 | 없음 | DB/큐 기반 작업 저장 |
| JWT 키 변경 | 기존 토큰 무효 | 수동 | 키 회전 절차 |

## 확장 방향

운영 단계에서는 다음 순서를 권장합니다.

1. 평가·쿼터·영속화 트랜잭션 연결
2. 인증·인가 일관화
3. 가이드 작업을 DB 또는 메시지 큐로 이전
4. 외부 호출 타임아웃·재시도·메트릭 도입
5. CI/CD와 환경별 설정 분리
6. 로그·메트릭·트레이싱과 장애 알림 추가
