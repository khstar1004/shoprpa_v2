# Multi-stage Dockerfile for the Java 8 Spring Boot robot service.

FROM maven:3.8.6-jdk-8 AS build

WORKDIR /app

# Copy Maven metadata first for better Docker cache reuse.
COPY backend/robot-service/pom.xml .

RUN mvn dependency:go-offline -B

COPY backend/robot-service/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:8-jre AS runtime

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/healthcheck/com/iflytek/rpa/common/health
COPY --from=build /app/target/classes/com/iflytek/rpa/common/health/HealthcheckProbe.class /app/healthcheck/com/iflytek/rpa/common/health/HealthcheckProbe.class
COPY --from=build /app/target/*.jar app.jar

# Optional environment file for local debugging.
# COPY docker/.env ./.env

RUN mkdir -p /app/logs

EXPOSE 8040

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD ["java", "-cp", "/app/healthcheck", "com.iflytek.rpa.common.health.HealthcheckProbe"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
