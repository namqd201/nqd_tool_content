# Stage 1: Build JAR using Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy gradle wrapper and build scripts
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build executable bootJar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Minimal runtime image using Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create upload directory for media assets
RUN mkdir -p /app/uploads/media

# Copy jar from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port (Render sets $PORT dynamically)
EXPOSE 8080
ENV PORT=8080
ENV TZ="Asia/Ho_Chi_Minh"

# Optimize JVM memory footprint and force IPv4 for Render (512MB RAM) + set VN Timezone
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Ho_Chi_Minh"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
