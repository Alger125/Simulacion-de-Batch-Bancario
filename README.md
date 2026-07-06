# 📦 Jobs_Prueba_1 — Proyecto Spring Batch LOX

## 📋 Tabla de Contenidos

1. [¿Qué hace este proyecto?](#-qué-hace-este-proyecto)
2. [Arquitectura General](#-arquitectura-general)
3. [Estructura de Módulos](#-estructura-de-módulos)
4. [Flujo Completo del Job](#-flujo-completo-del-job)
5. [Descripción de Cada Clase](#-descripción-de-cada-clase)
6. [Configuración XML (Spring Beans)](#-configuración-xml-spring-beans)
7. [Configuración SQL (.properties)](#-configuración-sql-properties)
8. [Cómo Ejecutar el Proyecto](#-cómo-ejecutar-el-proyecto)
9. [Cómo Ejecutar las Pruebas Unitarias](#-cómo-ejecutar-las-pruebas-unitarias)
10. [Cobertura de Código (JaCoCo)](#-cobertura-de-código-jacoco)
11. [Glosario para Juniors](#-glosario-para-juniors)

---

## 🎯 ¿Qué hace este proyecto?

Este proyecto implementa un **Job de procesamiento por lotes (Batch)** para el sistema LOX.

**En palabras simples:** Lee registros de movimientos bancarios desde una base de datos Oracle, aplica lógica de negocio sobre cada uno (consulta el saldo disponible del cliente), y escribe el resultado formateado en un archivo de texto `.txt`.

**Caso de uso real:**
> Cada día, el banco necesita revisar los movimientos del día anterior, validar el saldo de cada cuenta involucrada y generar un reporte. Este Job hace exactamente eso de forma automática y eficiente.

---

## 🏗 Arquitectura General

El proyecto sigue una arquitectura de **capas desacopladas**. Cada módulo tiene una responsabilidad única y clara:

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
│   LOXR174Impl → consulta Oracle vía JdbcTemplate           │
└────────────────────┬────────────────────────────────────────┘
                     │ usa
┌────────────────────▼────────────────────────────────────────┐
│                    loxc001                                  │
│          (Capa Común / DTOs y Mappers)                      │
│   MovimientoDTO, PimientosMapper                            │
└─────────────────────────────────────────────────────────────┘
```

**Regla de oro:** Las dependencias solo van hacia abajo. `loxj001` conoce a `loxbd001` y `loxc001`, pero **nunca al revés**. Esto facilita el mantenimiento: si cambias Oracle por MySQL, solo tocas `loxbd001`.

---

## 📁 Estructura de Módulos

```
Jobs_Prueba_1/
│
├── pom.xml                          ← POM padre: controla versiones globales
│
├── loxc001/                         ← Módulo COMMONS (el diccionario)
│   └── src/main/java/com/prueba/lox/
│       ├── batch/model_1/
│       │   └── MovimientoDTO.java   ← Objeto que representa un movimiento bancario
│       └── batch/mapper/
│           └── PimientosMapper.java ← Convierte fila de BD → MovimientoDTO
│
├── loxbd001/                        ← Módulo PERSISTENCIA (habla con Oracle)
│   └── src/main/java/com/prueba/lox/
│       └── lib/r174/
│           ├── interfaz/LOXR174.java        ← Contrato (interface)
│           ├── impl/LOXR174Abstract.java    ← Setters comunes (JdbcTemplate, SQL)
│           ├── impl/LOXR174Impl.java        ← Lógica real de consulta a Oracle
│           └── Variables/VariablesSQL.java  ← Enum de claves SQL
│
└── loxj001-01-mx/                   ← Módulo BATCH / NEGOCIO
    └── artifact/jobs/LOXJ162-01-MX/src/
        ├── main/java/com/prueba/lox/batch/
        │   ├── Utility/Utility.java  ← Herramientas comunes (fecha, eventos)
        │   ├── Reader.java           ← Lee datos (paginado)
        │   ├── Process.java          ← Procesa cada item
        │   ├── Complete.java         ← Tasklet de éxito
        │   └── Failed.java           ← Tasklet de fallo
        └── main/resources/
            ├── META-INF/spring/batch/beans/lox-beans.xml  ← Configuración de beans
            ├── META-INF/spring/batch/jobs/lox-job.xml     ← Definición del Job
            └── sql-LOXBR001IMP.properties                 ← Queries SQL
```

---

## 🔄 Flujo Completo del Job

El siguiente diagrama muestra, **paso a paso**, qué ocurre desde que el Job arranca hasta que termina:

```
┌─────────────────────────────────────────────────────────────────┐
│  INICIO: Spring lanza el Job "LOXJ162-01-MX" con parámetro     │
│  odate=YYYYMMDD (fecha de proceso, ej: 20260212)               │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                    ┌─────▼──────┐
                    │   step1    │  ← Chunk-oriented (lote de 100 en 100)
                    └─────┬──────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
     ┌────▼────┐    ┌──────▼──────┐   ┌──▼──────┐
     │ READER  │    │  PROCESSOR  │   │ WRITER  │
     │         │    │             │   │         │
     │ Lee     │───▶│ Por cada    │──▶│ Escribe │
     │ bloques │    │ MovimientoDTO│  │ línea   │
     │ de 100  │    │ llama a LOX │   │ en .txt │
     │ desde   │    │ y formatea  │   │ en disco│
     │ Oracle  │    │ la salida   │   │         │
     └─────────┘    └─────────────┘   └─────────┘
          │
     (cuando devuelve null → fin del step1)
          │
          ├── Si COMPLETED ──▶ stepCompleted
          │                        │
          │                   [Complete.java]
          │                   Notifica OK a librería LOX
          │
          └── Si FAILED ──────▶ stepFailed
                                    │
                               [Failed.java]
                               Extrae el error y notifica KO
```

### ¿Qué es un "Chunk"?

Spring Batch procesa los datos en **bloques (chunks)**. En este proyecto el `commit-interval` es **100**, lo que significa:

1. El Reader lee 100 registros.
2. El Processor los procesa uno por uno.
3. El Writer los escribe todos juntos (en un solo commit).
4. Si algo falla en ese bloque, solo se deshace ese bloque (no todo el Job).

---

## 📚 Descripción de Cada Clase

### `loxc001` — Capa Común

---

#### `MovimientoDTO.java`
**¿Qué es?** Un DTO (*Data Transfer Object*). Es una clase simple que solo tiene atributos y sus getters/setters. No tiene lógica de negocio.

**¿Para qué sirve?** Es el "sobre" que transporta los datos de un movimiento bancario entre las diferentes capas del sistema.

```java
// Atributos que contiene:
String nombre;       // Nombre del cliente
String numeroCuenta; // Número de cuenta bancaria
Double monto;        // Monto del movimiento
String estatus;      // Estado del movimiento (ej: "PROCESS", "APROBADO")

// toString() genera: "Juan,123456,5000.0,APROBADO"
// Útil para escribir en el archivo de salida
```

---

#### `PimientosMapper.java`
**¿Qué es?** Implementa la interface `RowMapper<MovimientoDTO>` de Spring.

**¿Para qué sirve?** Cuando Oracle devuelve una fila de datos, este mapper la convierte automáticamente en un objeto `MovimientoDTO`. Es el "traductor" entre el mundo SQL y el mundo Java.

```java
// Oracle devuelve: | NOMBRE | NUMERO_CUENTA | MONTO |
//                  | "Juan" | "123456"      | 5000.0|
//
// PimientosMapper convierte eso en:
MovimientoDTO dto = new MovimientoDTO();
dto.setNombre("Juan");
dto.setNumeroCuenta("123456");
dto.setMonto(5000.0);
```

---

### `loxbd001` — Capa de Persistencia

---

#### `VariablesSQL.java`
**¿Qué es?** Un `enum` (enumeración). En Java, un enum es una lista de constantes nombradas.

**¿Para qué sirve?** Centraliza los nombres de las claves SQL para evitar escribir Strings "mágicos" sueltos por el código.

```java
// En lugar de escribir esto en varios lugares:
properties.get("sql.select.detalle_saldos_cuenta") // ❌ String suelto, propenso a typos

// Se escribe esto:
VariablesSQL.SELECT_DETALLE_SALDOS.getDescription() // ✅ Tipado y seguro
```

| Enum Constante            | Clave en .properties                    |
|---------------------------|-----------------------------------------|
| `SELECT_VERIFICACION_DATOS` | `sql.select.verificacion_de_datos`    |
| `SELECT_DETALLE_SALDOS`     | `sql.select.detalle_saldos_cuenta`    |

---

#### `LOXR174.java` (Interface)
**¿Qué es?** Una interface. Define el "contrato" que cualquier implementación debe cumplir.

**¿Para qué sirve?** Los módulos que usan la librería LOX (`Process`, `Complete`, `Failed`) solo conocen esta interface, **no** la implementación. Esto permite cambiar la implementación sin afectar al resto.

```java
// Los dos métodos que CUALQUIER implementación debe tener:
void executeCreateCreditContract(Map<String, Object> event); // Recibe un evento y actúa
Map<String, Object> consultarDetalleSaldo(String numeroCuenta); // Consulta saldo en Oracle
```

---

#### `LOXR174Abstract.java`
**¿Qué es?** Una clase abstracta intermedia entre la interface y la implementación real.

**¿Para qué sirve?** Guarda los atributos comunes (`jdbcTemplate`, `sqlDetalleSaldos`) y sus setters. Los setters aquí son los que Spring usa para inyectar las dependencias desde el XML.

```java
// Spring inyecta estos valores desde lox-beans.xml:
protected JdbcTemplate jdbcTemplate;    // El objeto que habla con Oracle
protected String sqlDetalleSaldos;      // El SQL a ejecutar
```

---

#### `LOXR174Impl.java`
**¿Qué es?** La implementación concreta de la librería LOX. Es la clase más importante de `loxbd001`.

**¿Para qué sirve?** Recibe eventos del Batch, los procesa y consulta Oracle para obtener el saldo disponible de cada cuenta.

**Flujo interno:**

```
executeCreateCreditContract(event)
│
├─ ¿event es null o STATUS != "PROCESS"? → return (no hace nada)
│
├─ Extrae el MovimientoDTO del mapa event["item"]
│
├─ ¿item es null? → return (no hace nada)
│
└─ processContractLogic(item)
    │
    ├─ consultarDetalleSaldo(item.getNumeroCuenta())
    │   │
    │   └─ jdbcTemplate.queryForMap(sqlDetalleSaldos, numeroCuenta)
    │       → Ejecuta: SELECT SALDO_DISPONIBLE, ... FROM SALDOS WHERE NUMERO_CUENTA = ?
    │       → Devuelve: HashMap con los datos de la fila
    │
    └─ extractSaldo(infoSaldo)
        → Lee "SALDO_DISPONIBLE" del mapa
        → Devuelve el double (0.0 si no existe o es nulo)
```

**Manejo de errores:**
```java
// consultarDetalleSaldo tiene un try-catch:
// Si Oracle falla (timeout, registro no existe, etc.),
// en lugar de trunar el Job completo, devuelve un HashMap vacío.
// Así el Job puede continuar con los demás registros.
```

---

### `loxj001-01-mx` — Capa de Negocio / Batch

---

#### `Utility.java`
**¿Qué es?** Clase base de la que heredan `Reader`, `Process`, `Complete` y `Failed`.

**¿Para qué sirve?** Centraliza herramientas comunes para no repetir código en cada clase del Batch.

**Responsabilidades:**

| Método | ¿Qué hace? |
|--------|------------|
| `getOdate()` | Devuelve la fecha de proceso. Si no fue asignada, genera la de hoy (`yyyyMMdd`) |
| `getFechaFormateadaConDiagonales()` | Transforma `20260212` → `2026/02/12` (solo para archivos de salida) |
| `getMapEvent(status, message)` | Crea el "sobre" de comunicación con la librería LOX |
| `getMapEvent(status, item)` | Igual, pero el contenido es un objeto Java en lugar de un mensaje |
| `complementDataContracts(item)` | Formatea un `MovimientoDTO` como línea de texto para el .txt |
| `complementDataContracts(lista)` | Igual, pero para listas de contratos |
| `obtenerFormatoSalida(data)` | Null-safe toString |

**Importante — Dos formatos de fecha:**
```java
// Para la BD (Oracle entiende este formato en TO_DATE):
getMapEvent("OK", "msg") → ODATE: "20260212"  ← Sin diagonales

// Para el archivo .txt (legible para humanos):
complementDataContracts(dto) → FECHA: "2026/02/12"  ← Con diagonales
```

**¿Cómo se construye el mapa de evento?**
```java
// Evento de control (Complete, Failed):
Map<String, Object> event = {
    "STATUS" → "OK" / "KO",
    "MESSAGE" → "Ejecucion Correcta..." / "JOB_FAILED: ...",
    "ODATE" → "20260212"
}

// Evento de proceso (Process):
Map<String, Object> event = {
    "STATUS" → "PROCESS",
    "item" → <objeto MovimientoDTO>,
    "ODATE" → "20260212"
}
```

---

#### `Reader.java`
**¿Qué es?** Implementa `ItemReader`. Spring Batch lo llama **repetidamente** hasta que devuelva `null`.

**¿Para qué sirve?** Simula la paginación de datos. Lee bloques de 100 registros por vez hasta completar 1000.

> **Nota para el equipo:** En el ambiente real, este Reader es reemplazado por `JdbcCursorItemReader` configurado en el XML, el cual lee directamente desde Oracle. Esta clase `Reader.java` es una simulación local para pruebas sin BD.

```java
// Algoritmo simplificado:
while (indiceActual < 1000) {
    crea bloque de 100 registros simulados
    indiceActual += 100
    return bloque
}
return null; // Spring Batch entiende null como "ya terminé"
```

---

#### `Process.java`
**¿Qué es?** Implementa `ItemProcessor<MovimientoDTO, String>`. Recibe un DTO, lo procesa y devuelve un String.

**¿Para qué sirve?** Por cada movimiento leído de Oracle, notifica a la librería LOX y formatea la línea que irá al archivo de salida.

```java
// Por cada MovimientoDTO que llega:
1. Crea el evento: { STATUS: "PROCESS", item: dto, ODATE: "20260212" }
2. Llama: loxR174.executeCreateCreditContract(event)
   → Esto dispara la consulta del saldo en Oracle (en LOXR174Impl)
3. Incrementa el contador totalProcesados
4. Cada 1000 registros imprime progreso en consola
5. Devuelve la línea formateada: "NOMBRE: Juan | CUENTA: 123 | MONTO: 5000.00 | FECHA: 2026/02/12"
```

---

#### `Complete.java`
**¿Qué es?** Implementa `Tasklet`. Es un paso que se ejecuta **una sola vez** (no en chunks).

**¿Para qué sirve?** Si el Job terminó exitosamente, este Tasklet notifica a la librería LOX con `STATUS=OK`.

```java
// Se ejecuta cuando step1 termina con COMPLETED:
loxR174.executeCreateCreditContract({STATUS: "OK", MESSAGE: "Ejecucion Correcta del JOB LOX Local"})
// Nota: LOXR174Impl filtra STATUS == "PROCESS", entonces este evento no dispara consulta SQL.
// Solo registra que el Job finalizó bien.
```

---

#### `Failed.java`
**¿Qué es?** Implementa `Tasklet`. Se ejecuta si el Job falló.

**¿Para qué sirve?** Extrae el mensaje de error real de Spring Batch y lo notifica a la librería LOX con `STATUS=KO`.

```java
// Se ejecuta cuando step1 termina con FAILED:
1. Intenta extraer la excepción de chunkContext → jobExecution.getAllFailureExceptions()
2. Si lo logra → STATUS=KO, MESSAGE="JOB_FAILED: [RuntimeException: Error de conexión...]"
3. Si falla la extracción (catch) → STATUS=KO, MESSAGE="JOB_FAILED: Error desconocido..."
```

---

## ⚙️ Configuración XML (Spring Beans)

### `lox-beans.xml` — Los "ingredientes"

Define todos los objetos (beans) que Spring crea y administra:

| Bean ID          | Clase Java                        | ¿Para qué? |
|------------------|-----------------------------------|------------|
| `dataSource`     | `BasicDataSource` (DBCP)          | Conexión pool a Oracle |
| `jdbcTemplate`   | `JdbcTemplate`                    | Ejecuta SQL sobre el dataSource |
| `transactionManager` | `DataSourceTransactionManager` | Maneja transacciones |
| `jobRepository`  | `JobRepositoryFactoryBean`        | Spring Batch guarda estado del Job aquí |
| `jobLauncher`    | `SimpleJobLauncher`               | Lanza el Job |
| `itemReaderJob`  | `JdbcCursorItemReader`            | Lee MovimientoDTOs de Oracle |
| `itemProcessorJob` | `Process`                       | Procesa cada DTO |
| `itemWriterJob`  | `FlatFileItemWriter`              | Escribe resultado en .txt |
| `loxR174Impl`    | `LOXR174Impl`                     | La librería LOX (consulta saldos) |
| `beanCompletedJob` | `Complete`                      | Tasklet de éxito |
| `beanFailedJob`  | `Failed`                          | Tasklet de fallo |

### `lox-job.xml` — La "receta"

Define el flujo del Job:

```xml
LOXJ162-01-MX
    └── step1 (chunk: reader → processor → writer, commit cada 100)
          ├── COMPLETED → stepCompleted (beanCompletedJob)
          └── FAILED    → stepFailed   (beanFailedJob)
```

---

## 🗄 Configuración SQL (.properties)

Archivo: `sql-LOXBR001IMP.properties`

### Query 1 — Lectura principal (`itemReaderJob`)

```sql
SELECT c.NOMBRE, cu.CUENTA_ID as NUMERO_CUENTA, m.MONTO
FROM MOVIMIENTOS_m
JOIN CUENTAS_cu ON m.CLIENTE_ID = cu.CLIENTE_ID
JOIN CLIENTES_c ON cu.CLIENTE_ID = c.CLIENTE_ID
WHERE TRUNC(m.FECHA) = TO_DATE(?, 'YYYYMMDD')
-- El ? se reemplaza con el parámetro odate del Job
```

**¿Qué devuelve?** Un registro por movimiento del día, con nombre del cliente, número de cuenta y monto.

### Query 2 — Detalle de saldo (`LOXR174Impl`)

```sql
SELECT SALDO_DISPONIBLE, FECHA_ULTIMO_MOVIMIENTO, LIMITE_CREDITO
FROM SALDOS
WHERE NUMERO_CUENTA = ?
AND ESTATUS_SALDO = 'VIGENTE'
-- El ? se reemplaza con el numeroCuenta de cada MovimientoDTO
```

**¿Qué devuelve?** El saldo disponible y datos de la cuenta para validar si aplica el proceso de crédito.

---

## ▶️ Cómo Ejecutar el Proyecto

### Prerrequisitos

- Java 1.8 o superior
- Maven 3.6+
- Oracle XE corriendo en `localhost:1521` (o ajustar `lox-beans.xml`)
- Las tablas `MOVIMIENTOS`, `CUENTAS`, `CLIENTES` y `SALDOS` creadas en Oracle

### Paso 1 — Compilar todos los módulos

```bash
# Desde la raíz del proyecto (donde está el pom.xml padre)
mvn clean install -DskipTests
```

Esto compila en orden correcto: `loxc001` → `loxbd001` → `loxj001-01-mx`.

### Paso 2 — Ajustar la ruta del archivo de propiedades

En `lox-beans.xml`, cambiar la ruta del `propertyConfigurer` a la ruta real en tu máquina:

```xml
<property name="location" value="file:C:/TU_RUTA/sql-LOXBR001IMP.properties" />
```

### Paso 3 — Ejecutar el Job manualmente (prueba local)

Existe la clase `LoxJobTestManual.java` que lanza el Job como un test de JUnit:

```bash
mvn test -pl loxj001-01-mx -Dtest=LoxJobTestManual
```

O desde tu IDE (IntelliJ/Eclipse): click derecho sobre `LoxJobTestManual` → Run As → JUnit Test.

El Job se ejecutará con `odate=20260212`. Puedes cambiar ese valor en la clase.

### Paso 4 — Ver el resultado

El archivo de salida se genera en:
```
C:/Users/TU_USUARIO/Desktop/Pruebas/LOX_D02_20260212.txt
```

Ajusta la ruta en `lox-beans.xml` si es necesario.

---

## 🧪 Cómo Ejecutar las Pruebas Unitarias

### Ejecutar todos los tests

```bash
mvn test
```

### Ejecutar tests de un módulo específico

```bash
# Solo commons
mvn test -pl loxc001

# Solo persistencia
mvn test -pl loxbd001

# Solo el batch
mvn test -pl loxj001-01-mx
```

### Ejecutar una sola clase de test

```bash
mvn test -pl loxbd001 -Dtest=LOXR174ImplTest
```

### ¿Qué prueba cada clase de test?

| Clase de Test | Módulo | ¿Qué valida? |
|---------------|--------|--------------|
| `MovimientoDTOTest` | loxc001 | Getters, setters y `toString()` del DTO |
| `PimientosMapperTest` | loxc001 | Que el mapper convierte correctamente un `ResultSet` mockeado |
| `LOXR174ImplTest` | loxbd001 | Lógica de `extractSaldo`, `consultarDetalleSaldo` y `executeCreateCreditContract` |
| `UtilityTest` | loxj001 | Formateo de fechas, construcción de eventos, métodos de Utility |
| `ReaderTest` | loxj001 | Paginación: bloques de 100, IDs correctos, devolución de null al final |
| `ProcessTest` | loxj001 | Que el Processor llama a la librería y actualiza el contador |
| `CompleteTest` | loxj001 | Que Complete llama a la librería con `anyMap()` y devuelve `FINISHED` |
| `FailedTest` | loxj001 | Que Failed maneja errores reales y genéricos correctamente |

---

## 📊 Cobertura de Código (JaCoCo)

El proyecto tiene JaCoCo configurado para medir qué porcentaje del código está cubierto por pruebas.

### Generar el reporte

```bash
mvn clean test
```

### Ver el reporte

Después de correr los tests, el reporte HTML se genera en:

```
loxbd001/target/site/jacoco/index.html
loxj001-01-mx/target/site/jacoco/index.html
```

Ábrelos en tu navegador para ver la cobertura línea por línea.

---

## 📖 Glosario para Juniors

| Término | Definición sencilla |
|---------|---------------------|
| **Spring Batch** | Framework de Java para procesar grandes volúmenes de datos de forma automática y por lotes |
| **Job** | La tarea completa que Spring Batch ejecuta (como un programa con pasos) |
| **Step** | Un paso dentro del Job. Puede ser un chunk o un tasklet |
| **Chunk** | Procesamiento en bloques: leer N → procesar N → escribir N → repetir |
| **Tasklet** | Un paso que ejecuta lógica personalizada una sola vez (sin chunks) |
| **ItemReader** | Lee datos de alguna fuente (BD, archivo, API) de a un elemento por vez |
| **ItemProcessor** | Transforma o valida cada elemento leído |
| **ItemWriter** | Escribe los elementos procesados en algún destino (archivo, BD) |
| **DTO** | *Data Transfer Object*: clase simple que solo carga datos entre capas |
| **RowMapper** | Convierte una fila de base de datos en un objeto Java |
| **JdbcTemplate** | Clase de Spring que simplifica la ejecución de SQL (sin escribir JDBC manual) |
| **Bean** | Un objeto administrado por Spring (Spring lo crea, configura y destruye) |
| **Inyección de Dependencias** | Spring "conecta" automáticamente los objetos entre sí usando el XML o anotaciones |
| **Interface** | Contrato en Java: define QUÉ métodos debe tener una clase, sin decir CÓMO |
| **Clase abstracta** | Como una clase normal, pero no se puede instanciar directamente. Define comportamiento base |
| **Mock** | En pruebas unitarias, un objeto "falso" que simula el comportamiento de una dependencia real |
| **JaCoCo** | Herramienta que mide qué porcentaje del código es ejecutado durante las pruebas |
| **Enum** | Lista de constantes con nombre en Java. Evita usar Strings "mágicos" en el código |
| **pom.xml** | Archivo de Maven que define dependencias, plugins y configuración del proyecto |
| **odate** | *Operational Date*: la fecha de proceso del Job (formato `yyyyMMdd`, ej: `20260212`) |
| **Commit interval** | Cuántos registros procesa Spring Batch antes de hacer commit a la base de datos |

---

## 🌿 Ramas del Repositorio

| Rama | Propósito |
|------|-----------|
| `main` | Código estable y aprobado para producción |
| `fix/Code-smells-sonnar` | Corrección de code smells detectados por SonarQube |

### ¿Qué es un Code Smell?

Un *code smell* es un patrón en el código que, aunque funciona, indica un posible problema de diseño o mantenimiento. SonarQube analiza el código estáticamente y reporta estos problemas. Los más comunes que se atienden en esta rama son: código duplicado, métodos muy largos, variables no usadas y falta de cobertura de pruebas.

---

*Documentación generada para el equipo de desarrollo LOX — Jobs_Prueba_1 v0.1.0-SNAPSHOT*
