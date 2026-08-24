# 빌드 도구를 런타임 이미지에서 제외하기 위해 Java 21 기반 다단계 빌드를 사용한다.
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
# 환경별 값은 런타임 환경변수로 주입하므로 추적된 application.yaml도 source와 함께 복사한다.
COPY src ./src
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
