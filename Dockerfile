# Build
FROM gradle:9.7.1-jdk25 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Run
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
