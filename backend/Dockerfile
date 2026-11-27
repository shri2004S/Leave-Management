# ==============================================================================
# Standalone Backend Dockerfile (Spring Boot 3 + Java 17)
# ==============================================================================

# Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

