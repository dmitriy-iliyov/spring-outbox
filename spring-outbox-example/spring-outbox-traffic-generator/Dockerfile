FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY spring-outbox-example/spring-outbox-shared-contract ./spring-outbox-shared-contract
COPY spring-outbox-example/spring-outbox-traffic-generator ./spring-outbox-traffic-generator

RUN mvn -f spring-outbox-shared-contract/pom.xml clean install -DskipTests
RUN mvn -f spring-outbox-traffic-generator/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=build /app/spring-outbox-traffic-generator/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []