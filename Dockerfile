# ═══════════════════════════════════════════════════════════
# Stage 1: Build the Spring Boot application
# ═══════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first (for dependency caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the fat JAR, skipping tests (tests run in CI, not in Docker build)
RUN ./mvnw package -DskipTests -B

# ═══════════════════════════════════════════════════════════
# Stage 2: Create the minimal runtime image
# ═══════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="himanshu"
LABEL description="Risqué Portfolio Analytics — Spring Boot Backend"

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
