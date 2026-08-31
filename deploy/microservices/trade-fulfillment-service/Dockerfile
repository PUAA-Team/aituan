FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S aituan && adduser -S aituan -G aituan

COPY --chown=aituan:aituan deploy/artifacts/trade-fulfillment-service/aituan-trade-fulfillment-service.jar /app/aituan-trade-fulfillment-service.jar

USER aituan

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/aituan-trade-fulfillment-service.jar"]
