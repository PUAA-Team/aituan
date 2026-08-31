FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S aituan && adduser -S aituan -G aituan

COPY --chown=aituan:aituan services/identity-asset-service/target/identity-asset-service-0.0.1-SNAPSHOT.jar /app/identity-asset-service.jar

USER aituan

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/identity-asset-service.jar"]
