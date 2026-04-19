FROM maven:3.9.6-eclipse-temurin-21

WORKDIR /app

COPY spring-outbox-example/spring-outbox-traffic-generator ./spring-outbox-traffic-generator

WORKDIR /app/spring-outbox-traffic-generator

ENTRYPOINT ["mvn", "gatling:test"]