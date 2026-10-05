# 1단계: Maven으로 프로젝트 빌드 (Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2단계: 자바 21 실행 환경 구축
FROM eclipse-temurin:21-jre
WORKDIR /app

# [핵심 수정] original- 이 붙지 않은 Shade 결과물 JAR만 정확히 app.jar로 복사
COPY --from=build /app/target/Bot_initial-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]