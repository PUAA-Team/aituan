FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S aituan && adduser -S aituan -G aituan

COPY --chown=aituan:aituan services/trade-fulfillment-service/target/trade-fulfillment-service-0.0.1-SNAPSHOT.jar /app/trade-fulfillment-service.jar

USER aituan

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/trade-fulfillment-service.jar"]
