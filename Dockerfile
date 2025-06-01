# Stage 1: Build the project using Maven
FROM maven:3.8.5-openjdk-17 AS builder
# Accept proxy settings as build args
ARG http_proxy
ARG https_proxy

# Set environment variables so Maven uses them
ENV http_proxy=${http_proxy}
ENV https_proxy=${https_proxy}

WORKDIR /app
# Add Maven proxy config
COPY settings.xml /root/.m2/settings.xml

COPY sample-management /app
RUN mvn clean package -Pproduction -DskipTests

# Stage 2: Run the built JAR
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/sample-management-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
