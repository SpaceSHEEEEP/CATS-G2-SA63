# syntax=docker/dockerfile:1.7
FROM node:22-bookworm-slim AS mobile-web
WORKDIR /work/mobile
COPY mobile/package.json mobile/angular.json mobile/tsconfig*.json mobile/capacitor.config.ts ./
RUN npm install --no-audit --no-fund
COPY mobile/src ./src
RUN npm run build:web

FROM eclipse-temurin:21-jdk-jammy AS backend-build
WORKDIR /work
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
COPY --from=mobile-web /work/mobile/dist/web/browser ./src/main/resources/static/mobile/app
RUN chmod +x ./gradlew && ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S cats && adduser -S cats -G cats && mkdir -p /app && chown cats:cats /app
WORKDIR /app
COPY --from=backend-build --chown=cats:cats /work/build/libs/*SNAPSHOT.jar /app/application.jar
USER cats
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
