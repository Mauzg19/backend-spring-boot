# Usar una imagen base de Java
FROM openjdk:17-jdk-slim

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el archivo JAR al contenedor (desde el contexto de build)
COPY target/Restaurant-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto
EXPOSE 5454

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]

