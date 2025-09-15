FROM eclipse-temurin:21 AS build

WORKDIR /app
# Copy gradle related files
COPY gradlew /app/
COPY gradle /app/gradle
COPY build.gradle.kts /app/
COPY settings.gradle.kts /app/
# Copy source code
COPY src /app/src
# Build the application
RUN ./gradlew build -x test
# Build stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]