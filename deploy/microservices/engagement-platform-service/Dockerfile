FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY services ./services
COPY database ./database
RUN mvn -B -f services/pom.xml -pl engagement-platform-service -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN groupadd --gid 10001 aituan \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin aituan \
    && mkdir -p /data/uploads \
    && chown -R aituan:aituan /app /data/uploads

COPY --from=build --chown=aituan:aituan \
  /workspace/services/engagement-platform-service/target/engagement-platform-service.jar /app/app.jar

USER 10001:10001
EXPOSE 8084
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=12 \
  CMD wget -qO- http://127.0.0.1:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
