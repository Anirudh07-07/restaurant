FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads

# Copy the JAR
COPY --from=build /app/target/foodnest-1.0.0.jar app.jar

# Run as non-root user for security
RUN addgroup -S foodnest && adduser -S foodnest -G foodnest
RUN chown -R foodnest:foodnest /app
USER foodnest

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
