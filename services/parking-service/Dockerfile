FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/parking-service ./services/parking-service
COPY parkflow-db-definitivo/parking ./parkflow-db-definitivo/parking
RUN mvn -f services/parking-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/parking-service/target/parking-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
