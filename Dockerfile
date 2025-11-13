# Etapa 1: build con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -e -B dependency:go-offline

COPY src ./src
RUN mvn -e -B clean package -DskipTests

# Etapa 2: Imagen final
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 5454
ENTRYPOINT ["java", "-jar", "app.jar"]


