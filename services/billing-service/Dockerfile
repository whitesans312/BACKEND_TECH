FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY services/billing-service/pom.xml ./services/billing-service/pom.xml
RUN mvn -f services/billing-service/pom.xml -B -q dependency:resolve dependency:resolve-plugins || true
COPY services/billing-service/src ./services/billing-service/src
COPY parkflow-db-definitivo/billing ./parkflow-db-definitivo/billing
RUN mvn -f services/billing-service/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/services/billing-service/target/billing-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
