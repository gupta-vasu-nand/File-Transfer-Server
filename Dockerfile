# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install tzdata for timezone support
RUN apk add --no-cache tzdata

# Create app directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create storage directory
RUN mkdir -p /app/storage

# Create logs directory
RUN mkdir -p /app/logs

# Create a non-root user to run the application
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

# Expose the application port
EXPOSE 9090

# Set environment variables
ENV SERVER_PORT=9090 \
    FILE_STORAGE_PATH=/app/storage \
    FILE_MAX_SIZE=1073741824 \
    JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]