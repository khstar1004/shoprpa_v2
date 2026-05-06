# 다중항목항목생성 Dockerfile for Java 8 Spring Boot Application

# 항목일항목: 항목생성JAR패키지
FROM maven:3.8.6-jdk-8 AS build

# 항목항목디렉터리
WORKDIR /app

# 복사Maven매칭항목파일
COPY backend/rpa-auth/pom.xml .

# 복사 lib 디렉터리(opensource profile 필요)
COPY backend/rpa-auth/lib ./lib

# 설치 rpa-auth-stubs 의 JAR
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

# 다운로드항목(항목사용Docker항목저장항목)
RUN mvn dependency:go-offline -B -P opensource

# 복사항목코드
COPY backend/rpa-auth/src ./src

# 항목생성항목사용(사용 opensource profile)
RUN mvn clean package -DskipTests -P opensource

# 항목이항목: 항목생성가능실행항목이미지
FROM eclipse-temurin:8-jre AS runtime

# 항목항목디렉터리
WORKDIR /app

# 설치항목필요의도구
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 에서항목생성항목복사JAR파일
COPY --from=build /app/target/*.jar app.jar

# 생성로그디렉터리
RUN mkdir -p /app/logs

# 항목단말항목
EXPOSE 10251

# 항목시항목
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 항목JVM매개변수
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 시작항목사용
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]