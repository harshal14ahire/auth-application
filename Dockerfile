# ── Stage 1: Build ──
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app/mfa-backend
COPY mfa-backend/.mvn/ .mvn/
COPY mfa-backend/mvnw mfa-backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -q
COPY mfa-backend/src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Run ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/mfa-backend/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -Dspring.data.mongodb.uri=${MONGODB_URI} -jar app.jar"]
