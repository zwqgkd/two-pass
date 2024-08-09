FROM openjdk:8-jdk-alpine

RUN mkdir -p /deploy

COPY ./target/time-service-0.0.1-SNAPSHOT.jar /deploy

EXPOSE 8281

ENTRYPOINT ["sh", "-c", "exec java -jar /deploy/one-pass-0.0.1-SNAPSHOT.jar"]