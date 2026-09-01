FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -pl engagement-platform-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN addgroup -S -g 10001 aituan \
    && adduser -S -D -H -u 10001 -G aituan aituan \
    && mkdir -p /data/uploads \
    && chown -R aituan:aituan /app /data/uploads

COPY --from=build --chown=aituan:aituan \
  /workspace/engagement-platform-service/target/engagement-platform-service.jar \
  /app/engagement-platform-service.jar

USER 10001:10001
EXPOSE 8084

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/engagement-platform-service.jar"]
