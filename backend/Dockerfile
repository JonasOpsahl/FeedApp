FROM node:24-alpine AS webbuilder
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
RUN npm run build

FROM gradle:8.14.3-alpine AS builder
WORKDIR /home/gradle
COPY backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/src src
COPY backend/gradle gradle
COPY --from=webbuilder /web/dist ./src/main/resources/static
RUN gradle bootJar
RUN mv build/libs/*.jar app.jar

FROM eclipse-temurin:21-alpine
RUN addgroup -g 1000 app && adduser -G app -D -u 1000 -h /app app
USER app
WORKDIR /app
COPY --from=builder --chown=1000:1000 /home/gradle/app.jar .
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]