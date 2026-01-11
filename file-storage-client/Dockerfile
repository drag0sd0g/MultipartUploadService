# Multi-stage build for file-storage-client
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
COPY file-storage-client file-storage-client
COPY file-storage-server file-storage-server

# Build the application
RUN ./gradlew :file-storage-client:build -x test -x checkstyleMain -x checkstyleTest -x pmdMain -x pmdTest -x spotbugsMain -x spotbugsTest

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create user for running the application
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

# Copy the built jar
COPY --from=builder --chown=appuser:appgroup /workspace/file-storage-client/build/libs/file-storage-client-1.0.0-SNAPSHOT.jar /app/app.jar

# Set JVM options for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Default command shows usage
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
