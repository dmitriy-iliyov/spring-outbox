FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY spring-outbox-example/spring-outbox-shared-contract spring-outbox-example/spring-outbox-shared-contract
RUN mvn -f spring-outbox-example/spring-outbox-shared-contract/pom.xml clean install -DskipTests

COPY spring-outbox-example/spring-outbox-consumer-example spring-outbox-example/spring-outbox-consumer-example
RUN mvn -f spring-outbox-example/spring-outbox-consumer-example/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/spring-outbox-example/spring-outbox-consumer-example/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]