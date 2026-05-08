# Multi-stage Dockerfile for the Java 21 Spring Boot resource service.

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven metadata first for better Docker cache reuse.
COPY backend/resource-service/pom.xml .

RUN mvn dependency:go-offline -B

COPY backend/resource-service/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN mkdir -p /app/healthcheck/com/iflytek/rpa/resource/common/health
COPY --from=build /app/target/classes/com/iflytek/rpa/resource/common/health/HealthcheckProbe.class /app/healthcheck/com/iflytek/rpa/resource/common/health/HealthcheckProbe.class
COPY --from=build /app/target/*.jar app.jar

# Optional environment file for local debugging.
# COPY docker/.env ./.env

EXPOSE 8030

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD ["java", "-cp", "/app/healthcheck", "com.iflytek.rpa.resource.common.health.HealthcheckProbe"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
