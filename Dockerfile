### ---- Build Stage ----
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and build files first for better caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy full project
COPY . .

# Build the jar
RUN ./gradlew bootJar --no-daemon

### ---- Runtime Stage ----
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Expose gateway port
EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
