FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/identity-service/pom.xml ./services/identity-service/pom.xml
RUN mvn -f services/identity-service/pom.xml -B -q dependency:resolve dependency:resolve-plugins || true
COPY services/identity-service/src ./services/identity-service/src
COPY parkflow-db-definitivo/identity ./parkflow-db-definitivo/identity
RUN mvn -f services/identity-service/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/services/identity-service/target/identity-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
