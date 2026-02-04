FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY . .

RUN mvn clean install -DskipTests -pl .,spring-outbox-core,spring-outbox-aop,spring-outbox-web,spring-outbox-kafka,spring-outbox-rabbit,spring-outbox-starter
RUN mvn -f spring-outbox-example/spring-outbox-shared-contract/pom.xml clean install -DskipTests
RUN mvn -f spring-outbox-example/spring-outbox-producer-example/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/spring-outbox-example/spring-outbox-producer-example/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]