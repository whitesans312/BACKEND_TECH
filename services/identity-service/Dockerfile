FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/identity-service ./services/identity-service
COPY parkflow-db-definitivo/identity ./parkflow-db-definitivo/identity
RUN mvn -f services/identity-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/identity-service/target/identity-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
