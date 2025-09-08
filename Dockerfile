# Build stage
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /workspace/app

# Copy only the necessary files for dependency resolution first (for better caching)
COPY pom.xml .
COPY src src

# Build the application
RUN mvn clean package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

# Set timezone
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set non-root user
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --group appuser

# Create app directory
RUN mkdir -p /app
RUN chown -R appuser:appuser /app
USER appuser

# Copy the built application
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build --chown=appuser:appuser ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build --chown=appuser:appuser ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build --chown=appuser:appuser ${DEPENDENCY}/BOOT-INF/classes /app

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:MinRAMPercentage=50.0"

# Set the entry point
ENTRYPOINT ["java","-cp","app:app/lib/*","-Djava.security.egd=file:/dev/./urandom","com.quizapp.aiquizapp.AiQuizApplication"]
