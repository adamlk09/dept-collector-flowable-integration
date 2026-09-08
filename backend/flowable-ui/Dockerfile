# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first (only re-runs when pom.xml changes).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build the executable jar.
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

# finalName in pom.xml is "flowable-ui", so the jar is deterministic.
COPY --from=build /build/target/flowable-ui.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
