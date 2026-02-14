# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the parent pom and module poms to cache dependencies
COPY pom.xml .
COPY ticket-common/pom.xml ticket-common/
COPY ticket-api/pom.xml ticket-api/

# Download dependencies using the specific profile
RUN mvn dependency:go-offline -B -Papi

# Copy source code
COPY ticket-common/src ticket-common/src
COPY ticket-api/src ticket-api/src

# Build the specific module and its dependencies
RUN mvn clean package -pl ticket-api -am -DskipTests -Papi

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the executable jar from the build stage
COPY --from=build /app/ticket-api/target/ticket-api-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
