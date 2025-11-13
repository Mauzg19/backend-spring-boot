# Imagen base compatible con Render (Java 17)
FROM eclipse-temurin:17-jdk

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el archivo JAR al contenedor (asegúrate de que este .jar exista)
COPY target/Restaurant-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto que usa tu backend
EXPOSE 5454

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]

