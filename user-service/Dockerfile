FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY user-service user-service
COPY common/pom.xml common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY accounting-service/pom.xml accounting-service/pom.xml
RUN chmod +x mvnw && ./mvnw -pl user-service package -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /build/user-service/target/*.jar app.jar
VOLUME /app/logs
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]
