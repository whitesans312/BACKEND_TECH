FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/parking-service/pom.xml ./services/parking-service/pom.xml
RUN mvn -f services/parking-service/pom.xml -B -q dependency:resolve dependency:resolve-plugins || true
COPY services/parking-service/src ./services/parking-service/src
COPY parkflow-db-definitivo/parking ./parkflow-db-definitivo/parking
RUN mvn -f services/parking-service/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/services/parking-service/target/parking-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
