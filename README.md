# 📦 Simulación de Batch Bancario — Proyecto Spring Batch LOX

Job de procesamiento por lotes (**Spring Batch 2.1.7** sobre **Java 8**) que lee movimientos bancarios desde **Oracle**, consulta el saldo disponible de cada cuenta y genera un reporte plano `.txt`. Todo el entorno (Oracle XE + el Job) se levanta con **Docker Compose**.

---

## 📋 Tabla de Contenidos

1. [¿Qué hace este proyecto?](#-qué-hace-este-proyecto)
2. [Requisitos previos](#-requisitos-previos)
3. [Descarga del proyecto](#-descarga-del-proyecto)
4. [Configuración inicial (.env)](#-configuración-inicial-env)
5. [Ejecución con Docker (recomendado)](#-ejecución-con-docker-recomendado)
6. [Creación de las tablas en Oracle](#-creación-de-las-tablas-en-oracle)
7. [Ejecución local sin Docker (alternativa)](#-ejecución-local-sin-docker-alternativa)
8. [Cómo probar el proyecto](#-cómo-probar-el-proyecto)
9. [Cobertura de código (JaCoCo)](#-cobertura-de-código-jacoco)
10. [Arquitectura general](#-arquitectura-general)
11. [Estructura de módulos](#-estructura-de-módulos)
12. [Flujo completo del Job](#-flujo-completo-del-job)
13. [Descripción de cada clase](#-descripción-de-cada-clase)
14. [Configuración XML (Spring Beans)](#-configuración-xml-spring-beans)
15. [Configuración SQL (.properties)](#-configuración-sql-properties)
16. [CI/CD con GitHub Actions](#-cicd-con-github-actions)
17. [Despliegue en Kubernetes](#-despliegue-en-kubernetes)
18. [Ramas del repositorio](#-ramas-del-repositorio)
19. [Solución de problemas comunes](#-solución-de-problemas-comunes)
20. [Glosario para juniors](#-glosario-para-juniors)

---

## 🎯 ¿Qué hace este proyecto?

Implementa el Job **`LOXJ162-01-MX`**, un proceso batch bancario.

**En palabras simples:** lee registros de movimientos desde una base de datos Oracle, por cada uno consulta el saldo disponible del cliente (a través de la librería de persistencia `loxbd001`), y escribe el resultado formateado en un archivo de texto `LOX_D02_<odate>.txt`.

**Caso de uso real:**
> Cada día, el banco necesita revisar los movimientos del día anterior, validar el saldo de cada cuenta involucrada y generar un reporte. Este Job hace exactamente eso de forma automática, en bloques (chunks) de 100 registros, con manejo de éxito (`Complete`) y fallo (`Failed`).

**Stack tecnológico:**

| Tecnología | Versión | ¿Para qué? |
|---|---|---|
| Java (runtime) | 8 (JRE) | Requisito obligatorio: Spring Batch 2.1.7 usa XStream con reflexión profunda, que Java 9+ bloquea |
| Spring Framework | 3.0.6.RELEASE | Inyección de dependencias, JdbcTemplate |
| Spring Batch | 2.1.7.RELEASE | Motor del Job (reader/processor/writer) |
| Maven | 3.6+ | Compilación multi-módulo + shade (fat JAR) |
| Oracle XE | 21 (imagen `gvenzl/oracle-xe:21-slim`) | Base de datos |
| JUnit / Mockito | 4.13.2 / 3.12.4 | Pruebas unitarias |
| JaCoCo | 0.8.8 | Cobertura de código |
| Docker + Docker Compose | Docker Desktop reciente | Orquestación del entorno completo |
| GitHub Actions | checkout@v4, setup-java@v4, buildx@v3 | CI/CD: compila con Maven y valida la imagen Docker en cada push/PR a `main` |
| Kubernetes | batch/v1 (kind: Job) | Ejecución del batch como Job en un clúster (`k8s/simulacion-job.yml`) |

---

## ✅ Requisitos previos

### Si vas a usar Docker (camino recomendado)

Solo necesitas:

- **Git** — para clonar el repositorio
- **Docker Desktop** (Windows/Mac) o **Docker Engine + Compose plugin** (Linux)

> No necesitas instalar Java ni Maven en tu máquina: el `Dockerfile` es multi-stage y compila el proyecto dentro del contenedor con Maven 3.9 y luego lo ejecuta sobre un JRE 8.

Verifica que Docker funciona:

```bash
docker --version
docker compose version
```

### Si vas a ejecutar en local (sin Docker)

- **JDK 8** (obligatorio para ejecutar el Job; para compilar puedes usar un JDK más nuevo con target 1.8)
- **Maven 3.6+**
- Una instancia de **Oracle** accesible (puede ser el contenedor `oracle-db` del compose)

Verifica:

```bash
java -version    # debe reportar 1.8.x para ejecutar el Job
mvn -version
```

---

## ⬇️ Descarga del proyecto

### Opción A — Clonar con Git (recomendado)

```bash
git clone https://github.com/Alger125/Simulacion-de-Batch-Bancario.git
cd Simulacion-de-Batch-Bancario
```

### Opción B — Descargar ZIP

1. Entra a `https://github.com/Alger125/Simulacion-de-Batch-Bancario`
2. Botón verde **Code → Download ZIP**
3. Descomprime y abre una terminal dentro de la carpeta descomprimida.

---

## 🔐 Configuración inicial (.env)

Docker Compose lee sus variables desde un archivo **`.env`** en la raíz del proyecto. Crea el tuyo (o edita el existente) con estos valores:

```env
ORACLE_PASSWORD=TuPasswordSeguro123
ODATE=20260706
DESKTOP_PATH=C:\Users\TU_USUARIO\Desktop
```

| Variable | ¿Qué es? | Ejemplo |
|---|---|---|
| `ORACLE_PASSWORD` | Contraseña del usuario `SYSTEM` de Oracle XE. La usa tanto el contenedor de BD como su healthcheck | `TuPasswordSeguro123` |
| `ODATE` | Fecha de proceso del Job en formato `yyyyMMdd` | `20260706` |
| `DESKTOP_PATH` | Carpeta de **tu máquina** donde quieres que aparezca el archivo de salida. Se monta como `/tmp` dentro del contenedor | Windows: `C:\Users\JimzL\Desktop` · Linux/Mac: `/home/usuario/salidas` |

> ⚠️ **Importante — seguridad:** la contraseña también está referenciada en `lox-beans.xml` (bean `dataSource`). Si cambias `ORACLE_PASSWORD` en el `.env`, actualiza también el `password` en `loxj001-01-mx/src/main/resources/META-INF/spring/batch/beans/lox-beans.xml` para que el Job pueda conectarse. Nunca subas un `.env` con contraseñas reales a un repositorio público: agrégalo a `.gitignore`.

---

## 🐳 Ejecución con Docker (recomendado)

### Paso 1 — Levantar todo el entorno

Desde la raíz del proyecto (donde está `docker-compose.yml`):

```bash
docker compose up --build
```

Esto hace, en orden:

1. **Construye la imagen del Job** (`Dockerfile` multi-stage):
   - *Etapa builder:* compila los 3 módulos con `mvn clean package -DskipTests` y genera el fat JAR (`maven-shade-plugin`) con los XML de Spring adentro.
   - *Etapa runtime:* copia el JAR a una imagen ligera con **JRE 8** y lo ejecuta como usuario no-root.
2. **Levanta Oracle XE 21** (`oracle-db`) en el puerto `1521`, con volumen persistente `oracle-data`.
3. **Espera el healthcheck** de Oracle (un `SELECT 1 FROM DUAL` cada 10s). Solo cuando la BD responde, arranca el contenedor `batch-job`.
4. El Job se lanza con:
   ```
   java -jar /app/app.jar META-INF/spring/batch/jobs/lox-job.xml LOXJ162-01-MX odate=20260705
   ```

> La **primera vez** tarda varios minutos: descarga la imagen de Oracle (~2 GB) y las dependencias de Maven.

### Paso 2 — Crear las tablas (solo la primera vez)

El Job necesita tablas que **no se crean automáticamente**. Con Oracle ya corriendo, sigue la sección [Creación de las tablas en Oracle](#-creación-de-las-tablas-en-oracle) y luego relanza el Job:

```bash
docker compose up batch-job
```

### Paso 3 — Ver el resultado

El archivo de salida se escribe en `/tmp` **dentro del contenedor**, que está mapeado a tu `DESKTOP_PATH`. Búscalo en tu máquina:

```
<DESKTOP_PATH>/LOX_D02_<odate>.txt
```

Por ejemplo: `C:\Users\JimzL\Desktop\LOX_D02_20260706.txt`

Cada línea tiene el formato:

```
NOMBRE: Juan | CUENTA: 123456 | MONTO: 5000.00 | FECHA: 2026/07/06
```

### Cambiar la fecha de proceso sin reconstruir

El `ENTRYPOINT` es fijo y los argumentos van en `CMD`, así que puedes sobreescribir el `odate` al vuelo:

```bash
docker compose run --rm batch-job META-INF/spring/batch/jobs/lox-job.xml LOXJ162-01-MX odate=20260707
```

### Comandos útiles

```bash
docker compose logs -f batch-job     # ver logs del Job en vivo
docker compose logs -f oracle-db     # ver logs de Oracle
docker compose down                  # apagar todo (conserva los datos de Oracle)
docker compose down -v               # apagar todo Y borrar el volumen de datos
docker exec -it oracle-db sqlplus system/TuPasswordSeguro123@localhost:1521/XE   # consola SQL
```

---

## 🗄 Creación de las tablas en Oracle

El Job requiere **dos grupos de tablas**:

### 1) Tablas de metadatos de Spring Batch

`JobRepositoryFactoryBean` guarda el estado de cada ejecución en tablas `BATCH_*`. Spring Batch 2.1.7 incluye el script dentro de su propio JAR: `org/springframework/batch/core/schema-oracle10g.sql`. Ejecútalo una vez en el esquema `SYSTEM` (o el que uses). Crea, entre otras: `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION` y sus secuencias.

Conéctate a la BD del contenedor:

```bash
docker exec -it oracle-db sqlplus system/TuPasswordSeguro123@localhost:1521/XE
```

Y pega el contenido del script (puedes extraerlo del JAR de `spring-batch-core-2.1.7.RELEASE` o descargarlo del repositorio de Spring Batch para esa versión).

### 2) Tablas de negocio

Estas son las tablas que consultan las queries del Job:

```sql
CREATE TABLE CLIENTES_BBVA (
    CLIENTE_ID   NUMBER PRIMARY KEY,
    NOMBRE       VARCHAR2(100) NOT NULL
);

CREATE TABLE CUENTAS_BBVA (
    CUENTA_ID    VARCHAR2(20) PRIMARY KEY,
    CLIENTE_ID   NUMBER NOT NULL REFERENCES CLIENTES_BBVA(CLIENTE_ID)
);

CREATE TABLE MOVIMIENTOS_BBVA (
    MOVIMIENTO_ID NUMBER PRIMARY KEY,
    CLIENTE_ID    NUMBER NOT NULL,
    MONTO         NUMBER(12,2) NOT NULL,
    FECHA         DATE NOT NULL
);

CREATE TABLE SALDOS (
    NUMERO_CUENTA           VARCHAR2(20) PRIMARY KEY,
    SALDO_DISPONIBLE        NUMBER(12,2),
    FECHA_ULTIMO_MOVIMIENTO DATE,
    LIMITE_CREDITO          NUMBER(12,2),
    ESTATUS_SALDO           VARCHAR2(20)
);
```

### 3) Datos de prueba (opcional pero recomendado)

```sql
INSERT INTO CLIENTES_BBVA VALUES (1, 'Juan Perez');
INSERT INTO CUENTAS_BBVA  VALUES ('123456', 1);
INSERT INTO MOVIMIENTOS_BBVA VALUES (1, 1, 5000.00, TO_DATE('20260706','YYYYMMDD'));
INSERT INTO SALDOS VALUES ('123456', 15000.00, SYSDATE, 50000.00, 'VIGENTE');
COMMIT;
```

> El `odate` con el que lances el Job debe coincidir con la `FECHA` de los movimientos insertados, de lo contrario el Reader no encontrará registros (el Job terminará `COMPLETED` pero el archivo saldrá vacío).

---

## 💻 Ejecución local sin Docker (alternativa)

Útil para depurar desde el IDE.

### Paso 1 — Compilar todos los módulos

```bash
# Desde la raíz del proyecto (donde está el pom.xml padre)
mvn clean install -DskipTests
```

Maven compila en orden de dependencias: `loxc001` → `loxbd001` → `loxj001-01-mx`.

### Paso 2 — Apuntar el dataSource a tu Oracle local

En `lox-beans.xml` la URL activa apunta al hostname de Docker (`oracle-db`). Para local, usa la línea que está comentada (o `localhost`):

```xml
<property name="url" value="jdbc:oracle:thin:@localhost:1521:XE" />
```

> Tip: puedes levantar **solo** la base de datos con Docker y correr el Job desde tu IDE:
> ```bash
> docker compose up oracle-db
> ```

### Paso 3 — Lanzar el Job

**Opción A — Fat JAR por línea de comandos:**

```bash
java -jar loxj001-01-mx/target/loxj001-01-mx-0.1.0-SNAPSHOT.jar \
     META-INF/spring/batch/jobs/lox-job.xml \
     LOXJ162-01-MX \
     odate=20260706
```

**Opción B — Test manual desde JUnit:**

Existe `LoxJobTestManual.java`, que lanza el Job completo como un test:

```bash
mvn test -pl loxj001-01-mx -Dtest=LoxJobTestManual
```

O desde IntelliJ/Eclipse: clic derecho sobre `LoxJobTestManual` → *Run As → JUnit Test*. El Job se ejecuta con `odate=20260212`; puedes cambiar ese valor dentro de la clase.

### Paso 4 — Ver el resultado

En local, el `FlatFileItemWriter` escribe en:

```
/tmp/LOX_D02_<odate>.txt
```

(En Windows sin Docker eso se resuelve como `C:\tmp\...`; crea la carpeta si no existe o ajusta la ruta del bean `itemWriterJob` en `lox-beans.xml`.)

---

## 🧪 Cómo probar el proyecto

Las pruebas unitarias **no necesitan Oracle ni Docker**: todas las dependencias externas están mockeadas con Mockito.

### Ejecutar todos los tests

```bash
mvn test
```

Deberías ver al final un `BUILD SUCCESS` con el resumen de tests por módulo.

### Ejecutar tests de un módulo específico

```bash
mvn test -pl loxc001          # solo commons
mvn test -pl loxbd001         # solo persistencia
mvn test -pl loxj001-01-mx    # solo el batch
```

### Ejecutar una sola clase de test

```bash
mvn test -pl loxbd001 -Dtest=LOXR174ImplTest
```

### ¿Qué prueba cada clase de test?

| Clase de Test | Módulo | ¿Qué valida? |
|---|---|---|
| `MovimientoDTOTest` | loxc001 | Getters, setters y `toString()` del DTO |
| `PimientosMapperTest` | loxc001 | Que el mapper convierte correctamente un `ResultSet` mockeado |
| `LOXR174ImplTest` | loxbd001 | Lógica de `extractSaldo`, `consultarDetalleSaldo` y `executeCreateCreditContract` |
| `UtilityTest` | loxj001-01-mx | Formateo de fechas, construcción de eventos, métodos de Utility |
| `ReaderTest` | loxj001-01-mx | Paginación simulada: bloques de 100, IDs correctos, `null` al final |
| `ProcessTest` | loxj001-01-mx | Que el Processor llama a la librería LOX y actualiza el contador |
| `CompleteTest` | loxj001-01-mx | Que Complete notifica éxito y devuelve `FINISHED` |
| `FailedTest` | loxj001-01-mx | Que Failed maneja errores reales y genéricos correctamente |
| `LoxJobTestManual` | loxj001-01-mx | ⚠️ Test de **integración manual**: lanza el Job real (requiere Oracle levantado) |

### Prueba end-to-end (integración)

1. Levanta el entorno: `docker compose up --build`
2. Asegúrate de tener tablas + datos de prueba con `FECHA` igual al `ODATE` del `.env`
3. Revisa los logs: `docker compose logs -f batch-job` — deberías ver el progreso del Job y el estado final `COMPLETED`
4. Verifica que existe `<DESKTOP_PATH>/LOX_D02_<odate>.txt` y que contiene una línea por movimiento

---

## 📊 Cobertura de código (JaCoCo)

JaCoCo está configurado en el POM padre, así que se genera para todos los módulos.

### Generar el reporte

```bash
mvn clean test
```

### Ver el reporte

Abre en tu navegador:

```
loxc001/target/site/jacoco/index.html
loxbd001/target/site/jacoco/index.html
loxj001-01-mx/target/site/jacoco/index.html
```

Cada reporte muestra la cobertura línea por línea, por clase y por paquete.

---

## 🏗 Arquitectura general

El proyecto sigue una arquitectura de **capas desacopladas**. Cada módulo tiene una responsabilidad única:

```
┌─────────────────────────────────────────────────────────────┐
│                    loxj001-01-mx                            │
│          (Capa de Negocio / Orquestación del Job)           │
│   Reader → Process → Writer  +  Complete / Failed           │
└────────────────────┬────────────────────────────────────────┘
                     │ usa
┌────────────────────▼────────────────────────────────────────┐
│                    loxbd001                                 │
│          (Capa de Persistencia / Base de Datos)             │
│   LOXR174Impl → consulta Oracle vía JdbcTemplate            │
└────────────────────┬────────────────────────────────────────┘
                     │ usa
┌────────────────────▼────────────────────────────────────────┐
│                    loxc001                                  │
│          (Capa Común / DTOs y Mappers)                      │
│   MovimientoDTO, PimientosMapper                            │
└─────────────────────────────────────────────────────────────┘
```

**Regla de oro:** las dependencias solo van hacia abajo. `loxj001` conoce a `loxbd001` y `loxc001`, pero **nunca al revés**. Si mañana cambias Oracle por MySQL, solo tocas `loxbd001`.

### Arquitectura en Docker

```
┌──────────────────────────── batch-network ────────────────────────────┐
│                                                                        │
│  ┌───────────────────┐  jdbc:oracle:thin:@oracle-db:1521:XE           │
│  │    batch-job      │ ────────────────────────────────►  ┌─────────┐ │
│  │  (JRE 8 + fat JAR)│                                    │oracle-db│ │
│  │  /tmp ◄── volumen │                                    │  XE 21  │ │
│  └────────┬──────────┘                                    └────┬────┘ │
│           │                                                    │      │
└───────────┼────────────────────────────────────────────────────┼──────┘
            ▼                                                    ▼
   <DESKTOP_PATH> en tu PC                              volumen oracle-data
   (aquí aparece el .txt)                               (datos persistentes)
```

---

## 📁 Estructura de módulos

```
Simulacion-de-Batch-Bancario/
│
├── pom.xml                          ← POM padre: módulos, Java 1.8, JaCoCo
├── Dockerfile                       ← Multi-stage: Maven 3.9 (build) → JRE 8 (run)
├── docker-compose.yml               ← Orquesta oracle-db + batch-job
├── .env                             ← Variables: ORACLE_PASSWORD, ODATE, DESKTOP_PATH
│
├── .github/
│   └── workflows/
│       └── ci-cd.yml                ← Pipeline CI/CD: Maven build + validación de imagen Docker
│
├── k8s/
│   └── simulacion-job.yml           ← Manifiesto Kubernetes: Job batch (kind: Job)
│
├── loxc001/                         ← Módulo COMMONS (el diccionario)
│   └── src/main/java/com/prueba/lox/batch/
│       ├── model_1/MovimientoDTO.java    ← Objeto que representa un movimiento
│       └── mapper/PimientosMapper.java   ← Convierte fila de BD → MovimientoDTO
│
├── loxbd001/                        ← Módulo PERSISTENCIA (habla con Oracle)
│   └── src/main/java/com/prueba/lox/lib/r174/
│       ├── interfaz/LOXR174.java         ← Contrato (interface)
│       ├── impl/LOXR174Abstract.java     ← Setters comunes (JdbcTemplate, SQL)
│       ├── impl/LOXR174Impl.java         ← Lógica real de consulta a Oracle
│       └── Variables/VariablesSQL.java   ← Enum de claves SQL
│
└── loxj001-01-mx/                   ← Módulo BATCH / NEGOCIO
    └── src/
        ├── main/java/com/prueba/lox/batch/
        │   ├── Utility/Utility.java  ← Herramientas comunes (fecha, eventos)
        │   ├── Reader.java           ← Reader simulado (paginado, para pruebas)
        │   ├── Process.java          ← Procesa cada item
        │   ├── Complete.java         ← Tasklet de éxito
        │   └── Failed.java           ← Tasklet de fallo
        ├── main/resources/
        │   ├── META-INF/spring/batch/beans/lox-beans.xml  ← Beans
        │   ├── META-INF/spring/batch/jobs/lox-job.xml     ← Definición del Job
        │   └── sql-LOXBR001IMP.properties                 ← Queries SQL
        └── test/java/...             ← Pruebas unitarias + LoxJobTestManual
```

---

## 🔄 Flujo completo del Job

```
┌─────────────────────────────────────────────────────────────────┐
│  INICIO: CommandLineJobRunner lanza "LOXJ162-01-MX" con         │
│  parámetro odate=YYYYMMDD (fecha de proceso, ej: 20260706)      │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                    ┌─────▼──────┐
                    │   step1    │  ← Chunk-oriented (commit cada 100)
                    └─────┬──────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
     ┌────▼────┐    ┌─────▼───────┐   ┌───▼─────┐
     │ READER  │    │  PROCESSOR  │   │ WRITER  │
     │         │    │             │   │         │
     │ Lee de  │───▶│ Por cada    │──▶│ Escribe │
     │ Oracle  │    │ Movimiento  │   │ línea   │
     │ (cursor,│    │ DTO llama a │   │ en el   │
     │ fetch   │    │ LOX y forma-│   │ .txt    │
     │ 1000)   │    │ tea salida  │   │         │
     └─────────┘    └─────────────┘   └─────────┘
          │
     (cuando el reader devuelve null → fin del step1)
          │
          ├── Si COMPLETED ──▶ stepCompleted [Complete.java]
          │                    Notifica OK a la librería LOX
          │
          └── Si FAILED (o *) ─▶ stepFailed  [Failed.java]
                                 Extrae el error real y notifica KO
```

### ¿Qué es un "Chunk"?

Spring Batch procesa los datos en **bloques (chunks)**. Aquí el `commit-interval` es **100**:

1. El Reader lee 100 registros.
2. El Processor los procesa uno por uno.
3. El Writer los escribe todos juntos (un solo commit).
4. Si algo falla en ese bloque, solo se deshace ese bloque, no todo el Job.

---

## 📚 Descripción de cada clase

### `loxc001` — Capa Común

#### `MovimientoDTO.java`
**¿Qué es?** Un DTO (*Data Transfer Object*): clase simple con atributos y getters/setters, sin lógica de negocio.

**¿Para qué sirve?** Es el "sobre" que transporta los datos de un movimiento bancario entre capas.

```java
String nombre;       // Nombre del cliente
String numeroCuenta; // Número de cuenta bancaria
Double monto;        // Monto del movimiento
String estatus;      // Estado del movimiento (ej: "PROCESS", "APROBADO")

// toString() genera: "Juan,123456,5000.0,APROBADO"
```

#### `PimientosMapper.java`
**¿Qué es?** Implementa `RowMapper<MovimientoDTO>` de Spring.

**¿Para qué sirve?** Cuando Oracle devuelve una fila, este mapper la convierte en un `MovimientoDTO`. Es el "traductor" entre SQL y Java.

```java
// Oracle devuelve: | NOMBRE | NUMERO_CUENTA | MONTO |
// PimientosMapper lo convierte en:
MovimientoDTO dto = new MovimientoDTO();
dto.setNombre("Juan");
dto.setNumeroCuenta("123456");
dto.setMonto(5000.0);
```

---

### `loxbd001` — Capa de Persistencia

#### `VariablesSQL.java`
**¿Qué es?** Un `enum` que centraliza las claves SQL para evitar Strings "mágicos".

| Enum Constante | Clave en .properties |
|---|---|
| `SELECT_VERIFICACION_DATOS` | `sql.select.verificacion_de_datos` |
| `SELECT_DETALLE_SALDOS` | `sql.select.detalle_saldos_cuenta` |

#### `LOXR174.java` (Interface)
**¿Qué es?** El "contrato". Los consumidores (`Process`, `Complete`, `Failed`) solo conocen esta interface, nunca la implementación.

```java
void executeCreateCreditContract(Map<String, Object> event); // Recibe un evento y actúa
Map<String, Object> consultarDetalleSaldo(String numeroCuenta); // Consulta saldo en Oracle
```

#### `LOXR174Abstract.java`
**¿Qué es?** Clase abstracta intermedia. Guarda los atributos comunes (`jdbcTemplate`, `sqlDetalleSaldos`) con los setters que Spring usa para inyectar desde el XML.

#### `LOXR174Impl.java`
**¿Qué es?** La implementación concreta; la clase más importante de `loxbd001`.

**Flujo interno:**

```
executeCreateCreditContract(event)
│
├─ ¿event es null o STATUS != "PROCESS"? → return (no hace nada)
├─ Extrae el MovimientoDTO de event["item"]
├─ ¿item es null? → return
└─ processContractLogic(item)
    ├─ consultarDetalleSaldo(item.getNumeroCuenta())
    │   └─ jdbcTemplate.queryForMap(sqlDetalleSaldos, numeroCuenta)
    │       → SELECT SALDO_DISPONIBLE, ... FROM SALDOS WHERE NUMERO_CUENTA = ?
    └─ extractSaldo(infoSaldo)
        → Lee "SALDO_DISPONIBLE" del mapa (0.0 si no existe o es nulo)
```

**Manejo de errores:** `consultarDetalleSaldo` tiene un try-catch: si Oracle falla (timeout, registro inexistente), devuelve un HashMap vacío en lugar de tronar el Job. Así el proceso continúa con los demás registros.

---

### `loxj001-01-mx` — Capa de Negocio / Batch

#### `Utility.java`
Clase base de la que heredan `Reader`, `Process`, `Complete` y `Failed`. Centraliza herramientas comunes:

| Método | ¿Qué hace? |
|---|---|
| `getOdate()` | Devuelve la fecha de proceso; si no fue asignada, genera la de hoy (`yyyyMMdd`) |
| `getFechaFormateadaConDiagonales()` | Transforma `20260706` → `2026/07/06` (para archivos de salida) |
| `getMapEvent(status, message)` | Crea el "sobre" de comunicación con la librería LOX |
| `getMapEvent(status, item)` | Igual, pero el contenido es un objeto Java |
| `complementDataContracts(item)` | Formatea un `MovimientoDTO` como línea de texto |
| `obtenerFormatoSalida(data)` | toString null-safe |

**Importante — dos formatos de fecha:**

```java
// Para la BD (Oracle lo entiende en TO_DATE):
getMapEvent("OK", "msg") → ODATE: "20260706"   // sin diagonales

// Para el archivo .txt (legible para humanos):
complementDataContracts(dto) → FECHA: "2026/07/06"  // con diagonales
```

**Estructura del mapa de evento:**

```java
// Evento de control (Complete, Failed):
{ "STATUS": "OK"/"KO", "MESSAGE": "...", "ODATE": "20260706" }

// Evento de proceso (Process):
{ "STATUS": "PROCESS", "item": <MovimientoDTO>, "ODATE": "20260706" }
```

#### `Reader.java`
Implementa `ItemReader`. Spring Batch lo llama repetidamente hasta que devuelva `null`.

> **Nota:** en la ejecución real, el reader activo es el **`JdbcCursorItemReader`** configurado en `lox-beans.xml`, que lee directo de Oracle. Esta clase `Reader.java` es una **simulación local** (páginas de 100 hasta 1000 registros) para poder probar sin BD.

#### `Process.java`
Implementa `ItemProcessor<MovimientoDTO, String>`. Por cada DTO:

1. Crea el evento `{ STATUS: "PROCESS", item: dto, ODATE }`
2. Llama `loxR174.executeCreateCreditContract(event)` → dispara la consulta de saldo en Oracle
3. Incrementa `totalProcesados` (imprime progreso cada 1000)
4. Devuelve la línea formateada: `NOMBRE: Juan | CUENTA: 123 | MONTO: 5000.00 | FECHA: 2026/07/06`

#### `Complete.java`
Implementa `Tasklet` (se ejecuta una sola vez). Si el Job terminó `COMPLETED`, notifica a LOX con `STATUS=OK`. Como `LOXR174Impl` solo procesa `STATUS == "PROCESS"`, este evento no dispara SQL: solo registra el cierre exitoso.

#### `Failed.java`
Implementa `Tasklet`. Si el Job terminó `FAILED`:

1. Extrae la excepción real de `chunkContext → jobExecution.getAllFailureExceptions()`
2. Si lo logra → `STATUS=KO`, `MESSAGE="JOB_FAILED: [detalle del error]"`
3. Si la extracción falla → `STATUS=KO`, `MESSAGE="JOB_FAILED: Error desconocido..."`

---

## ⚙️ Configuración XML (Spring Beans)

### `lox-beans.xml` — los "ingredientes"

| Bean ID | Clase | ¿Para qué? |
|---|---|---|
| `dataSource` | `BasicDataSource` (DBCP) | Pool de conexiones a Oracle (`jdbc:oracle:thin:@oracle-db:1521:XE`) |
| `jdbcTemplate` | `JdbcTemplate` | Ejecuta SQL sobre el dataSource |
| `transactionManager` | `DataSourceTransactionManager` | Maneja transacciones |
| `jobRepository` | `JobRepositoryFactoryBean` | Spring Batch guarda el estado del Job (tablas `BATCH_*`) |
| `jobLauncher` | `SimpleJobLauncher` | Lanza el Job |
| `propertyConfigurer` | `PropertyPlaceholderConfigurer` | Carga `classpath:sql-LOXBR001IMP.properties` (va dentro del fat JAR) |
| `itemReaderJob` | `JdbcCursorItemReader` (scope=step) | Lee movimientos de Oracle; recibe `#{jobParameters['odate']}`, fetch 1000 |
| `itemProcessorJob` | `Process` | Procesa cada DTO |
| `itemWriterJob` | `FlatFileItemWriter` | Escribe en `/tmp/LOX_D02_<odate>.txt` (si no hay odate, usa la fecha de hoy) |
| `loxR174Impl` | `LOXR174Impl` | La librería LOX (consulta saldos) |
| `beanCompletedJob` | `Complete` | Tasklet de éxito |
| `beanFailedJob` | `Failed` | Tasklet de fallo |

### `lox-job.xml` — la "receta"

```
LOXJ162-01-MX
    └── step1 (chunk: reader → processor → writer, commit cada 100)
          ├── COMPLETED → stepCompleted (beanCompletedJob)
          ├── FAILED    → stepFailed   (beanFailedJob)
          └── *         → stepFailed   (cualquier otro estado también falla)
```

---

## 🗄 Configuración SQL (.properties)

Archivo: `loxj001-01-mx/src/main/resources/sql-LOXBR001IMP.properties` (es la **fuente de verdad**: viaja dentro del fat JAR y se carga por classpath).

### Query 1 — Lectura principal (`itemReaderJob`)

```sql
SELECT c.NOMBRE, cu.CUENTA_ID as NUMERO_CUENTA, m.MONTO
FROM MOVIMIENTOS_BBVA m
JOIN CUENTAS_BBVA cu ON m.CLIENTE_ID = cu.CLIENTE_ID
JOIN CLIENTES_BBVA c ON cu.CLIENTE_ID = c.CLIENTE_ID
WHERE TRUNC(m.FECHA) = TO_DATE(?, 'YYYYMMDD')
-- El ? se reemplaza con el parámetro odate del Job
```

**Devuelve:** un registro por movimiento del día, con nombre del cliente, número de cuenta y monto.

### Query 2 — Detalle de saldo (`LOXR174Impl`)

```sql
SELECT SALDO_DISPONIBLE, FECHA_ULTIMO_MOVIMIENTO, LIMITE_CREDITO
FROM SALDOS
WHERE NUMERO_CUENTA = ?
AND ESTATUS_SALDO = 'VIGENTE'
-- El ? se reemplaza con el numeroCuenta de cada MovimientoDTO
```

**Devuelve:** el saldo disponible y datos de la cuenta para validar si aplica el proceso.

---

## 🚀 CI/CD con GitHub Actions

El repositorio incluye un pipeline de integración continua en `.github/workflows/ci-cd.yml` llamado **"CI/CD Pipeline - Simulación Bancaria"**.

### ¿Cuándo se ejecuta?

| Evento | Rama | ¿Qué significa? |
|---|---|---|
| `push` | `main` | Cada vez que se sube un commit directo a `main` |
| `pull_request` | `main` | Cada vez que se abre o actualiza un PR hacia `main` |

> Esto garantiza que **ningún código roto llegue a `main`**: si el build de Maven o la imagen Docker fallan, el PR aparece con una ❌ roja y no debería mergearse.

### ¿Qué hace el pipeline? (job `build-and-test`)

Corre sobre `ubuntu-latest` y ejecuta 5 pasos en orden:

```
┌────────────────────────────────────────────────────────────┐
│ 1. actions/checkout@v4                                     │
│    Descarga el código del repositorio al runner            │
├────────────────────────────────────────────────────────────┤
│ 2. actions/setup-java@v4                                   │
│    Instala JDK 17 (Temurin) + activa caché de Maven        │
│    (la caché acelera builds posteriores: no re-descarga    │
│    todas las dependencias cada vez)                        │
├────────────────────────────────────────────────────────────┤
│ 3. mvn -B package --file pom.xml                           │
│    Compila los 3 módulos, ejecuta los tests unitarios      │
│    y genera el fat JAR (-B = modo batch, sin colores       │
│    ni prompts interactivos)                                │
├────────────────────────────────────────────────────────────┤
│ 4. docker/setup-buildx-action@v3                           │
│    Prepara Docker Buildx (motor de builds avanzado)        │
├────────────────────────────────────────────────────────────┤
│ 5. docker/build-push-action@v5  (push: false)              │
│    Construye la imagen con el Dockerfile multi-stage       │
│    SOLO para validar que compila — no la publica en        │
│    ningún registry                                         │
└────────────────────────────────────────────────────────────┘
```

**Detalles importantes:**

- El pipeline compila con **JDK 17** (target `1.8` definido en el POM), pero la **ejecución** del Job sigue siendo sobre **JRE 8** dentro del contenedor. Compilar con un JDK moderno con target 1.8 es válido; ejecutar con Java 9+ no lo es (por XStream/reflexión).
- `push: false` significa que la imagen **no se sube** a Docker Hub ni a GHCR: el paso existe únicamente como *smoke test* del `Dockerfile`. Si en el futuro quieres publicar la imagen, cambia a `push: true` y agrega un paso de `docker/login-action` con secretos (`DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`).
- Puedes ver las ejecuciones en la pestaña **Actions** del repositorio en GitHub.

### Ver el estado del pipeline

```
https://github.com/Alger125/Simulacion-de-Batch-Bancario/actions
```

---

## ☸️ Despliegue en Kubernetes

Además de Docker Compose, el proyecto incluye un manifiesto para correr el proceso como un **Job de Kubernetes**: `k8s/simulacion-job.yml`.

### ¿Por qué `kind: Job` y no `Deployment`?

Un batch bancario es un proceso que **inicia, procesa y termina**. Un `Deployment` mantendría el pod vivo y lo reiniciaría al terminar (¡en bucle infinito!). Un `Job` en cambio:

- Ejecuta el pod **una vez** hasta completarse
- Marca el Job como `Complete` si el contenedor sale con código 0
- Con `restartPolicy: Never`, si falla **no reintenta** (comportamiento controlado)

### Anatomía del manifiesto

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: batch-bancario-job
spec:
  template:
    spec:
      hostAliases:                          # ①
      - ip: "192.168.1.215"
        hostnames:
        - "oracle-db"
      containers:
        - name: simulacion-container
          image: simulacion-bancaria:local  # ②
          imagePullPolicy: Never            # ③
          args: ["META-INF/spring/batch/jobs/lox-job.xml", "LOXJ162-01-MX", "odate=20260706"]  # ④
      restartPolicy: Never                  # ⑤
```

| # | Elemento | Explicación |
|---|---|---|
| ① | `hostAliases` | Inyecta una entrada en el `/etc/hosts` del pod: el hostname `oracle-db` (el mismo que usa `lox-beans.xml` en la URL JDBC) resuelve a la IP de la máquina donde corre Oracle. Así **no hay que tocar el XML** para desplegar en K8s. ⚠️ Ajusta la IP `192.168.1.215` a la de tu máquina/servidor de BD |
| ② | `image: simulacion-bancaria:local` | Nombre de la imagen local. Debes construirla antes con ese tag |
| ③ | `imagePullPolicy: Never` | Kubernetes **no intenta descargar** la imagen de ningún registry: usa la que ya existe en el nodo. Ideal para clústeres locales (Docker Desktop, minikube, kind) |
| ④ | `args` | Sobrescribe el `CMD` del Dockerfile (el `ENTRYPOINT` `java -jar /app/app.jar` se conserva). Aquí defines qué job y qué `odate` correr, **sin reconstruir la imagen** |
| ⑤ | `restartPolicy: Never` | Si el Job falla, el pod queda en `Failed` para que inspecciones los logs, en vez de reintentar a ciegas |

### Paso a paso para desplegar

```bash
# 1. Construir la imagen con el tag que espera el manifiesto
docker build -t simulacion-bancaria:local .

# (Si usas minikube, carga la imagen en su daemon:)
minikube image load simulacion-bancaria:local

# 2. Asegúrate de que Oracle esté corriendo y accesible en la IP del hostAlias
docker compose up -d oracle-db

# 3. Editar la IP del hostAlias si es necesario (la de TU máquina)
#    k8s/simulacion-job.yml → hostAliases.ip

# 4. Aplicar el manifiesto
kubectl apply -f k8s/simulacion-job.yml

# 5. Monitorear
kubectl get jobs                              # estado del Job
kubectl get pods                              # pod creado por el Job
kubectl logs -f job/batch-bancario-job        # logs en vivo

# 6. Re-ejecutar (los Jobs son inmutables: hay que borrarlo primero)
kubectl delete job batch-bancario-job
kubectl apply -f k8s/simulacion-job.yml
```

### Cambiar el `odate` en Kubernetes

Edita la línea `args` del manifiesto y vuelve a aplicar:

```yaml
args: ["META-INF/spring/batch/jobs/lox-job.xml", "LOXJ162-01-MX", "odate=20260708"]
```

> **Nota:** en este manifiesto el archivo de salida se escribe en el `/tmp` **del contenedor** y se pierde al terminar el pod. Para conservarlo, agrega un `volumeMount` con un `hostPath` o `PersistentVolumeClaim` apuntando a `/tmp`.

---

## 🌿 Ramas del repositorio

El repositorio sigue un flujo tipo **GitHub Flow**: las funcionalidades se desarrollan en ramas `feature/*` y se integran a `main` mediante Pull Requests.

### Ramas activas

| Rama | Rol | Último commit | Contenido |
|---|---|---|---|
| `main` | 🟢 **Rama principal y estable.** Es la rama por defecto y la más actualizada; contiene TODO el proyecto: los 3 módulos Maven, Docker, el pipeline CI/CD **y** los manifiestos de Kubernetes | `638babd` — *feat: agrega manifiestos de Kubernetes para jobs batch y actualiza Dockerfile* | Todo lo descrito en este README |
| `feature/pipeline-github` | 🔧 **Rama de feature** donde se desarrolló el pipeline de GitHub Actions (`.github/workflows/ci-cd.yml`). Fue integrada a `main` vía **PR #1** y luego sincronizada de vuelta con **PR #2** | `d0ae648` — *Merge pull request #2 from Alger125/main* | Igual a `main` **excepto**: no incluye la carpeta `k8s/` ni la última versión del `Dockerfile` |

### Diferencia actual entre ramas

`main` está **1 commit adelante** de `feature/pipeline-github`:

```
main  vs  feature/pipeline-github
──────────────────────────────────
 Dockerfile             | 25 líneas modificadas (refactor ENTRYPOINT/CMD)
 k8s/simulacion-job.yml | 18 líneas nuevas (manifiesto Kubernetes)
```

### Historial de commits (grafo)

```
* 638babd (main) feat: agrega manifiestos de Kubernetes para jobs batch y actualiza Dockerfile
| *   d0ae648 (feature/pipeline-github) Merge pull request #2 from Alger125/main
| |\
| |/
|/|
* | 1fb73a9 Merge pull request #1 from Alger125/feature/pipeline-github
|\|
| * 460cb15 feat: agrega pipeline de CI/CD para compilar con Maven y Docker
* | 016034b Update print statement from 'Hello' to 'Goodbye'
|/
* e32f6ab Configuración inicial de la simulación de batch bancario
```

**Lectura del historial, de abajo hacia arriba:**

1. `e32f6ab` — Configuración inicial: se sube toda la simulación del batch bancario (módulos Maven, Docker Compose, tests).
2. `016034b` — Ajuste menor en `main`.
3. `460cb15` — En la rama `feature/pipeline-github` se crea el workflow de CI/CD.
4. `1fb73a9` — **PR #1**: el pipeline se mergea a `main`. Desde aquí, cada push/PR a `main` compila y valida la imagen Docker automáticamente.
5. `d0ae648` — **PR #2**: `main` se mergea de vuelta a `feature/pipeline-github` para sincronizarla.
6. `638babd` — Se agregan los manifiestos de Kubernetes (`k8s/`) y se refactoriza el `Dockerfile` (separación `ENTRYPOINT`/`CMD` para poder sobrescribir argumentos desde K8s).

### Cómo trabajar con las ramas

```bash
# Ver todas las ramas (locales y remotas)
git branch -a

# Cambiar a la rama de feature
git checkout feature/pipeline-github

# Volver a main
git checkout main

# Crear una nueva feature siguiendo la convención del repo
git checkout main
git pull origin main
git checkout -b feature/nombre-de-tu-feature

# Al terminar: push y abrir Pull Request hacia main
git push -u origin feature/nombre-de-tu-feature
```

> **Convención sugerida:** ramas `feature/<descripcion>` para funcionalidades nuevas, commits con prefijos tipo *Conventional Commits* (`feat:`, `fix:`, `docs:`, `refactor:`), y todo cambio a `main` pasa por Pull Request para que el pipeline de CI valide el build antes del merge.

---

## 🔧 Solución de problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `batch-job` nunca arranca | Oracle sigue inicializando (el healthcheck no pasa) | Espera; la primera vez Oracle tarda 1-3 min. Revisa `docker compose logs oracle-db` |
| `ORA-00942: table or view does not exist` | Faltan las tablas de negocio o las `BATCH_*` de Spring Batch | Ejecuta los scripts de la sección [Creación de las tablas](#-creación-de-las-tablas-en-oracle) |
| `ORA-01017: invalid username/password` | El password del `.env` no coincide con el de `lox-beans.xml` | Sincroniza ambos valores |
| `InaccessibleObjectException ... does not "opens java.util"` | Ejecutaste el JAR con Java 9+ | Usa **JRE/JDK 8** (el `Dockerfile` ya lo garantiza) |
| El Job termina `COMPLETED` pero el `.txt` sale vacío | No hay movimientos con `FECHA` igual al `odate` | Inserta datos de prueba con esa fecha o cambia el `odate` |
| `Unable to access jarfile` | Ruta relativa al JAR | Usa la ruta absoluta (el `ENTRYPOINT` del Dockerfile ya la usa) |
| No aparece el `.txt` en mi Desktop | `DESKTOP_PATH` mal escrito o Docker sin permiso para montar esa carpeta | Verifica la ruta en el `.env` y el *File Sharing* de Docker Desktop |
| `Connection refused` corriendo local | El `dataSource` apunta a `oracle-db` (hostname de Docker) | Cambia la URL a `localhost` en `lox-beans.xml` |
| Un Job con el mismo `odate` no vuelve a ejecutarse | Spring Batch no relanza una `JobInstance` ya `COMPLETED` con los mismos parámetros | Usa otro `odate` o limpia las tablas `BATCH_*` |

---

## 📖 Glosario para juniors

| Término | Definición sencilla |
|---|---|
| **Spring Batch** | Framework de Java para procesar grandes volúmenes de datos por lotes |
| **Job** | La tarea completa que Spring Batch ejecuta (un programa con pasos) |
| **Step** | Un paso dentro del Job. Puede ser un chunk o un tasklet |
| **Chunk** | Procesamiento en bloques: leer N → procesar N → escribir N → repetir |
| **Tasklet** | Un paso que ejecuta lógica personalizada una sola vez (sin chunks) |
| **ItemReader** | Lee datos de alguna fuente (BD, archivo, API) de a un elemento por vez |
| **ItemProcessor** | Transforma o valida cada elemento leído |
| **ItemWriter** | Escribe los elementos procesados en algún destino (archivo, BD) |
| **DTO** | *Data Transfer Object*: clase simple que solo carga datos entre capas |
| **RowMapper** | Convierte una fila de base de datos en un objeto Java |
| **JdbcTemplate** | Clase de Spring que simplifica la ejecución de SQL |
| **Bean** | Un objeto administrado por Spring (Spring lo crea, configura y destruye) |
| **Inyección de Dependencias** | Spring "conecta" los objetos entre sí usando el XML o anotaciones |
| **Interface** | Contrato en Java: define QUÉ métodos debe tener una clase, sin decir CÓMO |
| **Clase abstracta** | Clase que no se instancia directamente; define comportamiento base |
| **Mock** | En pruebas, un objeto "falso" que simula una dependencia real |
| **JaCoCo** | Herramienta que mide qué porcentaje del código ejecutan las pruebas |
| **Enum** | Lista de constantes con nombre; evita Strings "mágicos" |
| **pom.xml** | Archivo de Maven con dependencias, plugins y configuración |
| **odate** | *Operational Date*: fecha de proceso del Job (`yyyyMMdd`, ej: `20260706`) |
| **Commit interval** | Cuántos registros procesa Spring Batch antes de hacer commit |
| **Fat JAR (shade)** | Un JAR que incluye todas las dependencias adentro; se ejecuta con `java -jar` sin más |
| **Multi-stage build** | Dockerfile con dos etapas: una compila (pesada) y otra ejecuta (ligera) |
| **Healthcheck** | Comando que Docker ejecuta para saber si un contenedor está "sano" antes de arrancar otros |
| **CI/CD** | *Integración/Entrega Continua*: automatizar compilación, pruebas y validación en cada cambio |
| **GitHub Actions** | Servicio de GitHub que ejecuta pipelines (workflows) automáticamente ante eventos como push o PR |
| **Workflow** | Archivo YAML en `.github/workflows/` que describe qué hace el pipeline y cuándo se dispara |
| **Runner** | Máquina (aquí `ubuntu-latest`) donde GitHub ejecuta los pasos del workflow |
| **Kubernetes (K8s)** | Orquestador de contenedores: decide dónde y cómo corren los pods en un clúster |
| **Job (K8s)** | Recurso de Kubernetes para procesos que corren una vez y terminan (ideal para batch) |
| **Pod** | La unidad mínima de ejecución en Kubernetes; envuelve uno o más contenedores |
| **Manifiesto** | Archivo YAML que describe un recurso de Kubernetes (`kubectl apply -f ...`) |
| **hostAliases** | Entradas extra en el `/etc/hosts` del pod para resolver hostnames a IPs fijas |
| **Rama (branch)** | Línea de desarrollo paralela en Git; aquí: `main` y `feature/pipeline-github` |
| **Pull Request (PR)** | Solicitud de integrar una rama en otra, con revisión y validación de CI incluidas |
| **Merge** | Acción de integrar los commits de una rama dentro de otra |

---

*Documentación del equipo de desarrollo LOX — Simulación de Batch Bancario v0.1.0-SNAPSHOT*
