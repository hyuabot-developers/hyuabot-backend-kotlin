FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY build/libs/app.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
