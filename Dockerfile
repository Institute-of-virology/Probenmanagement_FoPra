# Stage 1: Build the project using Maven
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app
COPY sample-management /app
RUN mvn clean package -Pproduction -DskipTests

# Stage 2: Run the built JAR
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/sample-management-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
