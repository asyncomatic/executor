FROM maven:3.9-eclipse-temurin-17 as builder

RUN mkdir -p /tmp/executor
COPY . /tmp/executor
WORKDIR /tmp/executor
RUN mvn clean -U install

FROM maven:3.9-eclipse-temurin-17
LABEL org.opencontainers.image.source=https://github.com/asyncomatic/executor
RUN mkdir -p /opt/app

COPY --from=builder /tmp/executor/target/executor-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
    /opt/app/executor.jar

WORKDIR /opt/app
ENTRYPOINT ["java", "-cp", "\"executor.jar\""]