# Multi-stage Dockerfile for Nirvaha Spring Boot Backend

# Stage 1: Build JAR using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Java 21 JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user security setup
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create default evidence directory and assign permissions
RUN mkdir -p /app/uploads/evidence && chown -R appuser:appgroup /app

# Copy packaged executable JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# Health check using Spring Boot Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
