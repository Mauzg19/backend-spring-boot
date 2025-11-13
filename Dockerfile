# Etapa 1: Construcción con Maven
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY . .

# Dar permisos al wrapper de Maven
RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagen final
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 5454

ENTRYPOINT ["java", "-jar", "app.jar"]



