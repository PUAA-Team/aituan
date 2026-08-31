FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S aituan && adduser -S aituan -G aituan

COPY --chown=aituan:aituan services/engagement-platform-service/target/engagement-platform-service-0.0.1-SNAPSHOT.jar /app/engagement-platform-service.jar

USER aituan

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/engagement-platform-service.jar"]
