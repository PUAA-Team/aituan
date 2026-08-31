FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ARG JAR_FILE=services/merchant-catalog-service/target/merchant-catalog-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENV SERVER_PORT=8082 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
