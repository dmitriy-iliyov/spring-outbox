FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# until in mvn central upload
COPY spring-outbox-core ./spring-outbox-core
RUN mvn -f spring-outbox-core/pom.xml clean install -DskipTests

COPY spring-outbox-example/spring-outbox-shared-contract ./spring-outbox-shared-contract
COPY spring-outbox-example/spring-outbox-publisher-example ./spring-outbox-publisher-example
RUN mvn -f spring-outbox-shared-contract/pom.xml clean install -DskipTests
RUN mvn -f spring-outbox-publisher-example/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=build /app/spring-outbox-publisher-example/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []