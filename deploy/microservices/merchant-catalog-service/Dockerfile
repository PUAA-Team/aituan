FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S aituan && adduser -S aituan -G aituan

COPY --chown=aituan:aituan services/merchant-catalog-service/target/merchant-catalog-service-0.0.1-SNAPSHOT.jar /app/merchant-catalog-service.jar

USER aituan

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/merchant-catalog-service.jar"]
