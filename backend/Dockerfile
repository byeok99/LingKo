# 빌드 도구를 런타임 이미지에서 제외하기 위해 Java 21 기반 다단계 빌드를 사용한다.
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src
# application.yaml은 로컬 Secret 보호를 위해 ignore하므로 추적 중인 안전한 예시를 jar 설정으로 사용한다.
COPY application.example.yaml ./src/main/resources/application.yaml
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy
# 발음 가이드 영상 병합 기능이 컨테이너 안에서도 동일하게 동작하도록 FFmpeg를 설치한다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
