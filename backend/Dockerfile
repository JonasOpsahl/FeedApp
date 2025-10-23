FROM gradle:8.14.3-alpine AS builder
WORKDIR /home/gradle
COPY settings.gradle.kts build.gradle.kts .
COPY src src
COPY gradle gradle
RUN gradle bootJar
RUN mv build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

FROM eclipse-temurin:21-alpine
RUN addgroup -g 1000 app
RUN adduser -G app -D -u 1000 -h /app app
USER app
WORKDIR /app
COPY --from=builder --chown=1000:1000 /home/gradle/app.jar .
COPY --chown=1000:1000 pollingDB.mv.db .
CMD ["java", "-jar", "app.jar"]
