FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/app.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
