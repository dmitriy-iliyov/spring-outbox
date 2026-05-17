FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# build lib
COPY . .
RUN mvn -f spring-outbox-tests/spring-outbox-test-utils/pom.xml clean install -DskipTests
RUN mvn clean install -DskipTests -Dgpg.skip=true

# build producer
RUN mvn -f spring-outbox-example/spring-outbox-shared-contract/pom.xml clean install -DskipTests
RUN mvn -B -e -f spring-outbox-example/spring-outbox-producer-example/pom.xml clean install -DskipTests=true

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/spring-outbox-example/spring-outbox-producer-example/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]