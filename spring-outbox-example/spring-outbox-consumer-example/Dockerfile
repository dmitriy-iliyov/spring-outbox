FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY spring-outbox-core ./spring-outbox-core
RUN mvn -f spring-outbox-core/pom.xml clean install -DskipTests

COPY spring-outbox-example/spring-outbox-shared-contract ./spring-outbox-shared-contract
COPY spring-outbox-example/spring-outbox-consumer-example ./spring-outbox-consumer-example

RUN mvn -f spring-outbox-shared-contract/pom.xml clean install -DskipTests
RUN mvn -f spring-outbox-consumer-example/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=build /app/spring-outbox-consumer-example/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []