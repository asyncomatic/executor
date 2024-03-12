FROM maven:3.9-eclipse-temurin-17 as builder

RUN mkdir -p /tmp/executor
COPY . /tmp/executor
WORKDIR /tmp/executor
RUN mvn clean -U install

FROM maven:3.9-eclipse-temurin-17
LABEL org.opencontainers.image.source=https://github.com/asyncomatic/executor

COPY --from=builder /tmp/executor/target/asyncomatic-executor-0.0.1.jar .
COPY --from=builder /tmp/executor/pom.xml .

RUN mvn install:install-file \
    -Dfile=asyncomatic-executor-0.0.1.jar \
    -DpomFile=pom.xml

RUN rm asyncomatic-executor-0.0.1.*
