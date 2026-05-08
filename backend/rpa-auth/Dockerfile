# Multi-stage Dockerfile for the Java 8 Spring Boot auth service.

FROM maven:3.8.6-jdk-8 AS build

WORKDIR /app

# Copy Maven metadata first for better Docker cache reuse.
COPY backend/rpa-auth/pom.xml .

# Copy the local stubs required by the opensource profile.
COPY backend/rpa-auth/lib ./lib

# Install the local rpa-auth stubs JAR.
RUN if [ -f "./lib/rpa-auth-stubs-1.0.0.jar" ]; then \
    mvn install:install-file \
        -Dfile=./lib/rpa-auth-stubs-1.0.0.jar \
        -DgroupId=com.iflytek.rpa \
        -DartifactId=rpa-auth-stubs \
        -Dversion=1.0.0 \
        -Dpackaging=jar; \
    else \
        echo "Error: missing lib/rpa-auth-stubs-1.0.0.jar"; \
        exit 1; \
    fi

RUN mvn dependency:go-offline -B -P opensource

COPY backend/rpa-auth/src ./src

RUN mvn clean package -DskipTests -P opensource

FROM eclipse-temurin:8-jre AS runtime

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/healthcheck/com/iflytek/rpa/auth/health
COPY --from=build /app/target/classes/com/iflytek/rpa/auth/health/HealthcheckProbe.class /app/healthcheck/com/iflytek/rpa/auth/health/HealthcheckProbe.class
COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/logs

EXPOSE 10251

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD ["java", "-cp", "/app/healthcheck", "com.iflytek.rpa.auth.health.HealthcheckProbe"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
