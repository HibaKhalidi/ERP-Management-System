# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 erp
COPY --from=build /app/target/erp-backend-1.0.0.jar app.jar
USER erp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
