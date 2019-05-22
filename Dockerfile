FROM maven:3.6.1-jdk-8-alpine AS BUILDER

COPY pom.xml /tmp/
COPY src /tmp/src/
COPY resources /tmp/resources
WORKDIR /tmp/
RUN mvn package

FROM openjdk:8-jre-alpine AS KTOR

COPY --from=BUILDER /tmp/target/kotlin-budgety-0.0.1-jar-with-dependencies.jar /app/

COPY wait-for /app

RUN chmod +x /app/wait-for

ENV APPLICATION_USER ktor

RUN adduser -D -g '' $APPLICATION_USER

RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

EXPOSE 8080

WORKDIR /app

CMD ["./wait-for","db:3306", "--","java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "kotlin-budgety-0.0.1-jar-with-dependencies.jar"]