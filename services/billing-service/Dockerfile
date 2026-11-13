FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/billing-service ./services/billing-service
COPY parkflow-db-definitivo/billing ./parkflow-db-definitivo/billing
RUN mvn -f services/billing-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/billing-service/target/billing-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
