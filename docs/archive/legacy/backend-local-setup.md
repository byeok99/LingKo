# Backend Local Setup

작성 기준: 2026-05-27

이 문서는 LingKo 백엔드를 로컬에서 실행하기 위한 참고 문서다. `docs/`는 Git 추적에서 제외되어 있으므로 로컬 참고용으로만 관리한다.

## 1. 파일 역할

백엔드 설정 파일은 역할이 다르다.

```text
backend/application.example.yaml
  -> Git에 올라가는 설정 예시

backend/src/main/resources/application.yaml
  -> Spring Boot가 읽는 로컬 설정 파일
  -> Git 제외
  -> ${DB_HOST} 같은 환경변수 placeholder 사용

backend/.env
  -> Docker Compose가 읽는 실제 로컬 환경변수 파일
  -> Git 제외
  -> 실제 secret 값 입력

backend/.env.example
  -> Git에 올라가는 환경변수 예시
  -> 실제 secret 값 없음
```

## 2. Docker Compose로 실행

로컬 Mac에 MySQL이나 FFmpeg를 직접 설치하지 않고 실행하려면 Docker Compose를 사용한다.

```bash
cd backend
cp .env.example .env
```

그다음 `backend/.env`에 실제 값을 채운다.

필수로 채워야 할 값:

```text
DB_PASSWORD
```

외부 연동 기능을 사용할 때 필요한 값:

```text
AZURE_SECRET_KEY
AZURE_REGION
REPLICATE_API_KEY
REPLICATE_VERSION
AWS_S3_BUCKET
AWS_S3_REGION
AWS_ACCESS_KEY
AWS_SECRET_KEY
```

실행:

```bash
docker compose up --build
```

백그라운드 실행:

```bash
docker compose up --build -d
```

상태 확인:

```bash
docker compose ps
```

로그 확인:

```bash
docker compose logs -f backend
```

중지:

```bash
docker compose down
```

DB 볼륨까지 삭제:

```bash
docker compose down -v
```

## 3. FFmpeg

Docker 실행 시에는 로컬 Mac에 FFmpeg를 설치할 필요가 없다.

`backend/Dockerfile`에서 컨테이너 내부에 FFmpeg를 설치한다.

```dockerfile
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
```

환경변수는 다음 값이면 된다.

```text
FFMPEG_PATH=ffmpeg
```

컨테이너 내부 경로 확인:

```bash
docker compose exec backend which ffmpeg
```

예상 결과:

```text
/usr/bin/ffmpeg
```

## 4. Spring 설정 흐름

Docker 실행 시 설정 흐름은 다음과 같다.

```text
backend/.env
  -> docker compose가 읽음
      -> backend 컨테이너 환경변수로 주입
          -> application.yaml의 ${DB_HOST} 등이 Spring Boot에서 해석됨
```

예:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:lingko}
    username: ${DB_USER:lingko}
    password: ${DB_PASSWORD:}
```

## 5. Docker 없이 직접 실행

Docker 없이 직접 실행하려면 로컬에 다음이 필요하다.

```text
Java 17
MySQL
FFmpeg
```

직접 실행:

```bash
cd backend
./gradlew bootRun
```

주의:

- Spring Boot는 `.env` 파일을 자동으로 읽지 않는다.
- 직접 실행할 때는 터미널 환경변수로 export하거나 `backend/src/main/resources/application.yaml`에 로컬 값을 둔다.
- 이 파일은 Git에서 제외되어 있으므로 로컬 값이 커밋되지 않는다.

## 6. 현재 공개 API 확인

현재 바로 확인 가능한 API는 표준 발음 변환이다.

```bash
curl -i -X POST http://localhost:8080/api/pronunciation/convert \
  -H 'Content-Type: application/json' \
  -d '{"text":"맛있겠다."}'
```

예상 응답 형태:

```json
{
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싯게따."
}
```

## 7. 검증 명령

컴파일 확인:

```bash
./gradlew compileJava
```

테스트:

```bash
./gradlew test
```

현재 테스트는 레거시 미추적 테스트와 외부 API 연동 테스트 정리가 필요하므로, 전체 테스트 복구는 별도 작업으로 진행한다.
