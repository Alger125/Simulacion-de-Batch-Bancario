# ==========================================
# ETAPA 1: Construcción
# ==========================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Ejecución
# ==========================================
FROM eclipse-temurin:8-jre
WORKDIR /app

# Copiamos el fat JAR generado
COPY --from=builder /app/loxj001-01-mx/target/loxj001-01-mx-*.jar /app/app.jar

RUN useradd -m myuser
USER myuser

# ENTRYPOINT: Define el ejecutable base que no cambia.
# El array evita errores de shell y permite que el CMD se pase como argumentos.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# CMD: Define los argumentos por defecto. 
# Si en Kubernetes sobrescribes el 'command', esto se ignora.
CMD ["META-INF/spring/batch/jobs/lox-job.xml", "LOXJ162-01-MX", "odate=20260705"]