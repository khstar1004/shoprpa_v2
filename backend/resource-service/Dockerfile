# 다중항목항목생성 Dockerfile for Java 21 Spring Boot Application

# 항목일항목: 항목생성JAR패키지
FROM maven:3.9-eclipse-temurin-21 AS build

# 항목항목디렉터리
WORKDIR /app

# 복사Maven매칭항목파일
COPY backend/resource-service/pom.xml .

# 다운로드항목(항목사용Docker항목저장항목)
RUN mvn dependency:go-offline -B

# 복사항목코드
COPY backend/resource-service/src ./src

# 항목생성항목사용
RUN mvn clean package -DskipTests

# 항목이항목: 항목생성가능실행항목이미지
FROM eclipse-temurin:21-jre AS runtime

# 항목항목디렉터리
WORKDIR /app

# 에서항목생성항목복사JAR파일
COPY --from=build /app/target/*.jar app.jar

# 복사매칭항목파일(예결과저장에서)
# COPY docker/.env ./.env

# 항목단말항목
EXPOSE 8030

# 항목JVM매개변수
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# 시작항목사용
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]