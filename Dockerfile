# =========================================================================
# Stage 1 - BUILD: compile and package the Spring Boot fat jar with Maven.
# Using the Maven image means you do NOT need Maven installed locally.
# =========================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom first so Docker can cache the dependency download layer.
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline

# Now copy the source and build. Tests run as part of the package step.
COPY src ./src
RUN mvn -q -e -B clean package -DskipTests=false

# =========================================================================
# Stage 2 - RUNTIME: small JRE image that only carries the built jar.
# =========================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user for better security.
RUN useradd -r -u 1001 draftly
USER draftly

# Copy the fat jar produced in the build stage.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Sensible container-friendly JVM defaults; override JAVA_OPTS at runtime if needed.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
