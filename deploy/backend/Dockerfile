FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S aituan && adduser -S aituan -G aituan

COPY --chown=aituan:aituan artifacts/backend/aituan-backend.jar /app/aituan-backend.jar

USER aituan

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/aituan-backend.jar"]
