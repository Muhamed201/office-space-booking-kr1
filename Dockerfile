# ---- сборка ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- запуск ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/office-booking.jar ./office-booking.jar
ENTRYPOINT ["java", "-jar", "office-booking.jar"]
