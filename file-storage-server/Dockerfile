# Multi-stage build for file-storage-server
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

# Copy gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY settings.gradle .
COPY gradle.properties .
COPY build.gradle .

# Copy source code
COPY file-storage-server file-storage-server
COPY file-storage-client file-storage-client

# Build the application
RUN ./gradlew :file-storage-server:build -x test -x checkstyleMain -x checkstyleTest -x pmdMain -x pmdTest -x spotbugsMain -x spotbugsTest

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create directory for file uploads
RUN mkdir -p /app/data-server && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

# Copy the built jar (Quarkus creates a -runner.jar)
COPY --from=builder --chown=appuser:appgroup /workspace/file-storage-server/build/file-storage-server-1.0.0-SNAPSHOT-runner.jar /app/app.jar

# Expose the default Quarkus port
EXPOSE 8080

# Set JVM options for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
