FROM maven:3.9.6-eclipse-temurin-21

WORKDIR /app

COPY oncebox-example/oncebox-traffic-generator ./oncebox-traffic-generator

WORKDIR /app/oncebox-traffic-generator

ENTRYPOINT ["mvn", "gatling:test"]