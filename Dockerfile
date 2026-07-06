# ==========================================
# ETAPA 1: Construcción (El famoso "builder")
# ==========================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Copiamos todo el código fuente al contenedor
COPY . .

# Compilamos para generar el JAR con el XML adentro
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Ejecución
# ==========================================
# JRE 8 obligatorio: Spring Batch 2.1.7 usa XStream, que hace reflección
# profunda sobre java.util.Properties. En Java 9+ el sistema de módulos
# lo bloquea (InaccessibleObjectException: does not "opens java.util").
# Java 8 no tiene módulos, así que todo el stack legacy funciona nativo.
FROM eclipse-temurin:8-jre
WORKDIR /app

# El shade plugin REEMPLAZA el jar principal (no genera *-shaded.jar porque
# no usamos <shadedArtifactAttached>). El fat JAR es loxj001-01-mx-*.jar y
# el jar flaco queda como original-loxj001-01-mx-*.jar (que este glob NO matchea).
COPY --from=builder /app/loxj001-01-mx/target/loxj001-01-mx-*.jar /app/app.jar

RUN useradd -m myuser
USER myuser

# Ejecución con la ruta absoluta para evitar el "Unable to access jarfile".
# El primer argumento del CommandLineJobRunner es un RECURSO DE CLASSPATH:
# dentro del JAR el XML del job vive en META-INF/spring/batch/jobs/lox-job.xml
# ENTRYPOINT fijo + CMD con los argumentos del job: se puede sobreescribir
# el odate sin reconstruir la imagen, ej:
#   docker run lox-job META-INF/spring/batch/jobs/lox-job.xml LOXJ162-01-MX odate=20260706
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["META-INF/spring/batch/jobs/lox-job.xml", "LOXJ162-01-MX", "odate=20260705"]