# Banco XYZ - Backend III Semana 3 y Semana 4

## 1. Objetivo del proyecto

Este proyecto reescribe tres procesos batch legacy del Banco XYZ usando **Spring Boot 3**, **Spring Batch 5** y una base de datos relacional **MySQL**. Ademas, incorpora la implementacion del patron arquitectonico **Backend for Frontend (BFF)** para exponer respuestas especializadas para tres canales: web, mobile y cajero automatico.

Los procesos oficiales de la evaluacion se alimentan desde tres archivos CSV incluidos dentro del proyecto, en `src/main/resources/data`. Esto permite que el profesor pueda clonar o abrir el proyecto y ejecutarlo sin depender de una ruta local del computador del estudiante.

Archivos utilizados:

- `transacciones.csv`
- `intereses.csv`
- `cuentas_anuales.csv`

La aplicacion convierte cada archivo en un Job independiente de Spring Batch. Cada Job lee el CSV, transforma y valida los datos con un `ItemProcessor`, escribe los resultados en MySQL y deja evidencia consultable mediante endpoints REST.

Los tres procesos implementados son:

1. **Reporte de Transacciones Diarias**: procesa transacciones, detecta anomalias y genera un resumen diario.
2. **Calculo de Intereses Mensuales**: aplica tasas de interes sobre cuentas de ahorro, prestamos e hipotecas, y calcula saldo final.
3. **Generacion de Estados de Cuenta Anuales**: consolida movimientos anuales por cuenta para auditoria.

## 2. Tecnologias utilizadas

- Java 17 o superior.
- Spring Boot 3.3.5.
- Spring Batch 5.
- Spring Web.
- Spring JDBC.
- MySQL.
- Patron Backend for Frontend (BFF) aplicado mediante APIs separadas por canal.
- Maven Wrapper para ejecutar el proyecto sin instalar Maven globalmente.
- Docker Compose opcional para levantar MySQL localmente si no se usa una instalacion propia.
- Lombok para reducir codigo repetitivo en DTOs y entidades.

## 3. Estructura del proyecto

```text
BackEnd-main/
├── docker-compose.yml
├── pom.xml
├── README.md
├── semana_3/                 Copia de referencia de los CSV
│   ├── transacciones.csv
│   ├── intereses.csv
│   └── cuentas_anuales.csv
└── src/main/
    ├── java/cl/duoc/bankxyz/migracion/
    │   ├── BankxyzMigracionBatchApplication.java
    │   ├── config/
    │   │   ├── CommonBatchConfig.java
    │   │   ├── TransaccionBatchConfig.java
    │   │   ├── InteresBatchConfig.java
    │   │   └── CuentaAnualBatchConfig.java
    │   ├── controllers/
    │   │   ├── TransaccionJobController.java
    │   │   ├── InteresJobController.java
    │   │   ├── CuentaAnualJobController.java
    │   │   ├── ConsultaResultadosController.java
    │   │   ├── WebBffController.java
    │   │   ├── MobileBffController.java
    │   │   └── AtmBffController.java
    │   ├── dtos/
    │   │   ├── TransaccionDTO.java
    │   │   ├── InteresDTO.java
    │   │   └── CuentaAnualDTO.java
    │   ├── entities/
    │   │   ├── TransaccionEntity.java
    │   │   ├── InteresEntity.java
    │   │   └── CuentaAnualEntity.java
    │   ├── exceptions/
    │   │   └── BatchJobLaunchException.java
    │   ├── listeners/
    │   │   ├── BatchStepListener.java
    │   │   └── BatchRetryListener.java
    │   ├── processors/
    │   │   ├── TransaccionProcessor.java
    │   │   ├── InteresProcessor.java
    │   │   └── CuentaAnualProcessor.java
    │   ├── services/
    │   │   ├── TransaccionJobService.java
    │   │   ├── InteresJobService.java
    │   │   ├── CuentaAnualJobService.java
    │   │   ├── ConsultaResultadosService.java
    │   │   └── bff/
    │   │       ├── BffDataGateway.java
    │   │       ├── WebBffService.java
    │   │       ├── MobileBffService.java
    │   │       └── AtmBffService.java
    │   └── tasklets/
    │       ├── ResumenTransaccionesTasklet.java
    │       └── ResumenAnualTasklet.java
    └── resources/
        ├── application.properties
        ├── schema.sql
        └── data/             CSV oficiales usados por Spring Batch
            ├── transacciones.csv
            ├── intereses.csv
            └── cuentas_anuales.csv
```

Los archivos usados por defecto en la ejecucion evaluada son los incluidos en el classpath del proyecto. Si se desea probar una carpeta externa local, las rutas se pueden sobreescribir al iniciar la aplicacion.

## 4. Arquitectura batch implementada

Cada proceso se implementa como un `Job` independiente:

| Proceso | Job | Step principal | Step de resumen | CSV de entrada | Tabla principal |
|---|---|---|---|---|---|
| Reporte de transacciones diarias | `transaccionesJob` | `transaccionStep` | `resumenTransaccionesStep` | `classpath:data/transacciones.csv` | `transaccion_procesada` |
| Calculo de intereses mensuales | `interesesJob` | `interesStep` | No aplica | `classpath:data/intereses.csv` | `interes_procesado` |
| Estados de cuenta anuales | `cuentasAnualesJob` | `cuentaAnualStep` | `resumenAnualStep` | `classpath:data/cuentas_anuales.csv` | `movimiento_anual_procesado` |

Los steps principales son **chunk-oriented**:

- Reader: `FlatFileItemReader`.
- Processor: `ItemProcessor`.
- Writer: `JdbcBatchItemWriter`.
- Chunk size: `5`.
- Ejecucion paralela: `TaskExecutor` con pool fijo de `3` hilos.
- Tolerancia a fallos: `skip`, `skipLimit`, `retry`, `retryLimit` y listeners.

## 4.1. Arquitectura BFF implementada para Semana 4

Para la actividad de Semana 4 se implemento el patron **Backend for Frontend (BFF)** usando una estrategia de **backends independientes por canal dentro del mismo proyecto Spring Boot**. Cada cliente tiene su propio controller, contrato REST, token de acceso y respuesta optimizada.

Esta estrategia fue elegida porque el sistema actual todavia es un proyecto academico con datos generados por Spring Batch. Separar tres repositorios completos para web, mobile y cajero agregaria complejidad operacional innecesaria para la entrega. En cambio, se mantiene una sola aplicacion desplegable, pero los tres BFF quedan separados logicamente por paquete, endpoint, token y responsabilidad. Si el sistema creciera, esta separacion permitiria extraer cada BFF a un microservicio independiente sin redisenar los contratos.

La implementacion queda separada asi:

| Canal | Controller | Endpoint principal | Necesidad cubierta |
|---|---|---|---|
| Web | `WebBffController` | `/api/bff/web/dashboard` | Entrega una vista completa para analistas: resumen, anomalias, metricas batch, saldos y resumen anual. |
| Mobile | `MobileBffController` | `/api/bff/mobile/resumen` | Entrega una respuesta compacta para pantallas pequenas y menor consumo de datos. |
| Cajero automatico | `AtmBffController` | `/api/bff/atm/cuentas/{cuentaId}` y `/api/bff/atm/cuentas/{cuentaId}/retiros` | Entrega informacion minima y segura para consulta de saldo y retiro. |

Cada canal tiene su propio servicio BFF: `WebBffService`, `MobileBffService` y `AtmBffService`. Estos servicios consultan las tablas generadas por los Jobs batch mediante `BffDataGateway` y transforman la informacion al formato requerido por cada cliente. Con esto se aplica la idea central del patron BFF: evitar que todos los frontends consuman una unica respuesta generica y obligar a cada cliente a filtrar datos que no necesita.

Tambien se aplican ideas asociadas a patrones de diseno:

- **Adapter**: los servicios `WebBffService`, `MobileBffService` y `AtmBffService` adaptan los datos persistidos por Spring Batch al contrato especifico de cada canal.
- **Proxy**: los controladores BFF actuan como punto intermedio entre los frontends y las tablas internas del sistema.
- **Strategy**: cada metodo del servicio representa una estrategia de respuesta distinta segun el tipo de cliente.

### Seguridad aplicada en los BFF

Los endpoints `/api/bff/**` usan autenticacion por token mediante el header `X-BFF-Token`. Cada canal tiene un token distinto, por lo que un token mobile no puede consumir el BFF web ni el BFF ATM.

Configuracion por defecto:

```properties
bankxyz.security.bff.web-token=web-token-banco-xyz
bankxyz.security.bff.mobile-token=mobile-token-banco-xyz
bankxyz.security.bff.atm-token=atm-token-banco-xyz
```

En una ejecucion real estos valores se deben entregar como variables de entorno:

```powershell
$env:BFF_WEB_TOKEN="token-web-seguro"
$env:BFF_MOBILE_TOKEN="token-mobile-seguro"
$env:BFF_ATM_TOKEN="token-atm-seguro"
```

La validacion se implementa con `BffTokenInterceptor`, `BffSecurityProperties` y `BffWebMvcConfig`. Si falta el token, la API responde `401`. Si el token existe pero no corresponde al canal, responde `403`.

### HTTPS y certificado local

Para evidenciar configuracion segura se incluye el perfil `https` en:

```text
src/main/resources/application-https.properties
```

Primero se genera un certificado local de desarrollo:

```powershell
.\generar-certificado-dev.ps1
```

El certificado se crea en `config/bankxyz-dev.p12`. Esa carpeta queda excluida por `.gitignore` para evitar subir certificados al repositorio; cada ambiente debe generar su propio certificado local.

Luego se inicia la aplicacion con HTTPS:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=https"
```

Con ese perfil la aplicacion queda disponible en:

```text
https://localhost:8443
```

## 5. Base de datos

La base de datos elegida para cumplir la pauta es **MySQL**.

La configuracion se encuentra en `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/bankxyz_batch?useSSL=false&serverTimezone=America/Santiago&allowPublicKeyRetrieval=true}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=${DB_USERNAME:bankxyz}
spring.datasource.password=${DB_PASSWORD:bankxyz}
```

El archivo `schema.sql` crea las tablas de negocio y las tablas internas de Spring Batch al iniciar la aplicacion, como `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION` y `BATCH_STEP_EXECUTION`. Esto permite reiniciar la app y repetir benchmarks desde un estado limpio.

Antes de iniciar la aplicacion, la base de datos `bankxyz_batch` debe existir en MySQL. Si quieres usar el usuario documentado en este proyecto, puedes crearlo con:

```sql
CREATE DATABASE IF NOT EXISTS bankxyz_batch
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'bankxyz'@'localhost' IDENTIFIED BY 'bankxyz';
GRANT ALL PRIVILEGES ON bankxyz_batch.* TO 'bankxyz'@'localhost';
FLUSH PRIVILEGES;
```

Si prefieres usar tu usuario propio de MySQL, por ejemplo `root`, puedes mantener el codigo igual y pasar credenciales por variables de entorno antes de ejecutar:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="TU_CLAVE_MYSQL"
```

Tablas de negocio:

- `transaccion_procesada`
- `resumen_transacciones_diarias`
- `interes_procesado`
- `movimiento_anual_procesado`
- `resumen_anual_cuenta`

## 6. Reglas de validacion y transformacion

### 6.1. Transacciones diarias

Archivo: `transacciones.csv`

Processor: `TransaccionProcessor`

Reglas aplicadas:

- Se aceptan fechas en los formatos `yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy` y `dd/MM/yyyy`.
- Se rechazan fechas inexistentes, por ejemplo un mes `13`.
- Se aceptan solo tipos `debito` y `credito`.
- Se rechazan tipos como `invalid` o `desconocido`.
- Se rechazan montos vacios, no numericos, iguales a cero o negativos.
- Se detectan duplicados por combinacion de fecha, monto y tipo.
- Los registros invalidos no se pierden: se guardan con estado `RECHAZADA` y motivo.

Salida principal:

- Registros validos con estado `VALIDA`.
- Registros anomalos con estado `RECHAZADA`.
- Resumen con total de registros, validos, rechazados, creditos y debitos.

### 6.2. Intereses mensuales

Archivo: `intereses.csv`

Processor: `InteresProcessor`

Reglas aplicadas:

- Se aceptan tipos de cuenta `ahorro`, `prestamo` e `hipoteca`.
- Se rechazan tipos como `unknown` o `-1`.
- Se valida edad entre `18` y `120`.
- Se rechazan saldos vacios, no numericos o negativos.
- Se detectan duplicados por nombre, saldo, edad y tipo de cuenta.
- Los registros invalidos quedan persistidos con estado `RECHAZADA`.

Tasas mensuales asumidas:

| Tipo de cuenta | Tasa mensual |
|---|---:|
| ahorro | 0.5% |
| prestamo | 1.5% |
| hipoteca | 1.0% |

Formula aplicada:

```text
interes_calculado = saldo_original * tasa_interes
saldo_final = saldo_original + interes_calculado
```

### 6.3. Estados de cuenta anuales

Archivo: `cuentas_anuales.csv`

Processor: `CuentaAnualProcessor`

Reglas aplicadas:

- Se aceptan fechas en los formatos `yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy` y `dd/MM/yyyy`.
- Se aceptan tipos de movimiento `deposito`, `retiro` y `compra`.
- Se rechazan variantes no normalizadas como `depósito` escrito con tilde en el CSV original o `pago`.
- Se rechazan descripciones vacias.
- Se rechazan montos vacios, no numericos o iguales a cero.
- Se rechaza un `deposito` con monto negativo porque un ingreso debe sumar.
- Los registros invalidos quedan guardados con estado `RECHAZADA`.

El step de resumen anual agrupa por cuenta:

- Total de depositos.
- Total de retiros y compras.
- Saldo neto anual.
- Cantidad de movimientos validos.

## 7. Manejo de errores y tolerancia a fallos

Cada step chunk-oriented esta configurado con:

```java
.faultTolerant()
.skip(FlatFileParseException.class)
.skip(DataAccessException.class)
.skipLimit(20)
.retry(TransientDataAccessException.class)
.retryLimit(3)
```

Esto permite:

- Continuar el Job si una linea del CSV viene mal formada.
- Omitir un registro que falla en lectura o escritura, hasta el limite configurado.
- Reintentar fallos transitorios de base de datos antes de dar el error por definitivo.
- Registrar skips y metricas mediante `BatchStepListener`.
- Registrar reintentos mediante `BatchRetryListener`.

Los errores de negocio no se tratan como excepciones tecnicas. En vez de detener el Job, los processors clasifican el registro como `RECHAZADA` y guardan el motivo en la base de datos. Esto permite auditoria posterior.

## 8. Politica de escalamiento

Se eligio un modelo optimizado con **multithreading por chunks**, en vez de particiones.

La configuracion compartida esta en `CommonBatchConfig`. Los parametros ya no quedan fijos en el codigo: se pueden cambiar desde `application.properties` o desde la linea de comandos para comparar ejecuciones reales.

```java
executor.setCorePoolSize(threadCount);
executor.setMaxPoolSize(threadCount);
executor.setQueueCapacity(queueCapacity);
executor.setThreadNamePrefix("bankxyz-batch-");
```

Parametros por defecto:

- Chunk size: `5`.
- Hilos minimos: `3`.
- Hilos maximos: `3`.
- Cola: `0`.
- Politica de rechazo: `CallerRunsPolicy`.
- Etiqueta de configuracion: `hilos-3-chunk-5`.

Justificacion:

- Los CSV son archivos planos de tamano acotado para una evaluacion academica.
- El procesamiento por chunk permite confirmar lectura, procesamiento y escritura por bloques.
- Tres hilos son suficientes para demostrar escalamiento sin sobrecargar el equipo local.
- `SynchronizedItemStreamReader` protege la lectura del CSV, porque `FlatFileItemReader` no es thread-safe.
- Los processors usan sets concurrentes para detectar duplicados de forma segura durante la ejecucion paralela.

### 8.1. Comparacion cuantitativa de configuraciones

Para responder al criterio de comparacion solicitado por el profesor, el aplicativo registra metricas reales por cada Step ejecutado. El listener `BatchStepListener` imprime en consola una linea con el marcador:

```text
[EVIDENCIA_BATCH]
```

Tambien escribe un archivo CSV acumulativo en:

```text
target/evidencias/benchmark-steps.csv
```

Ese archivo contiene:

- Configuracion usada.
- Cantidad de hilos.
- Tamano de chunk.
- Job y Step ejecutado.
- Duracion en milisegundos.
- Items procesados por segundo.
- Registros leidos y escritos.
- Skips de lectura, proceso y escritura.
- Commits y rollbacks.

Ejemplos de ejecucion para comparar configuraciones:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--bankxyz.batch.thread-count=1 --bankxyz.batch.chunk-size=5 --bankxyz.batch.config-label=hilos-1-chunk-5"
```

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--bankxyz.batch.thread-count=2 --bankxyz.batch.chunk-size=5 --bankxyz.batch.config-label=hilos-2-chunk-5"
```

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--bankxyz.batch.thread-count=3 --bankxyz.batch.chunk-size=5 --bankxyz.batch.config-label=hilos-3-chunk-5"
```

En cada configuracion se deben ejecutar los tres Jobs y capturar las salidas de `/api/resultados/evidencia/jobs`, `/api/resultados/evidencia/steps` y el archivo `target/evidencias/benchmark-steps.csv`.

Para ejecutar los tres Jobs y consultar las metricas en una sola pasada, tambien se incluye:

```powershell
.\ejecutar-evidencia.ps1
```

El script guarda una copia JSON de la evidencia en `target/evidencias`.

La guia detallada de capturas y comparacion esta en:

```text
docs/evidencia-benchmark.md
```

## 9. Prerrequisitos

Antes de ejecutar, instalar o tener disponible:

1. Java 17 o superior.
2. Maven Wrapper completo en la raiz del proyecto (`mvnw.cmd`, `mvnw` y `.mvn/wrapper`).
3. MySQL instalado y en ejecucion.
4. Postman, Insomnia o PowerShell para llamar los endpoints.
5. Git y una cuenta propia de GitHub para versionar la entrega.
6. Docker Desktop opcional, solo si prefieres levantar MySQL con `docker-compose.yml`.

Verificar versiones:

```powershell
java -version
.\mvnw.cmd -version
mysql --version
git --version
```

## 10. Ejecucion local paso a paso

Abrir PowerShell en la raiz del proyecto:

```powershell
cd "C:\Users\Marialex\Desktop\DUOC UC\8vo\Backend III\S5 sumativa bk\SumativaS3_Banco XYZ"
```

Crear la base de datos y usuario en MySQL. Entrar a MySQL con un usuario administrador, por ejemplo:

```powershell
mysql -u root -p
```

Ejecutar:

```sql
CREATE DATABASE IF NOT EXISTS bankxyz_batch
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'bankxyz'@'localhost' IDENTIFIED BY 'bankxyz';
GRANT ALL PRIVILEGES ON bankxyz_batch.* TO 'bankxyz'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Si no quieres crear el usuario `bankxyz`, puedes usar tu usuario propio mediante variables de entorno:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="TU_CLAVE_MYSQL"
```

Como alternativa opcional, si quieres levantar una instancia limpia de MySQL con Docker:

```powershell
docker compose up -d
docker ps
```

Ejecutar la aplicacion:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicacion queda disponible en:

```text
http://localhost:8081
```

Los Jobs no se ejecutan automaticamente al iniciar. Se disparan manualmente mediante HTTP porque `spring.batch.job.enabled=false`.

## 11. Endpoints para ejecutar Jobs

### Job 1: Reporte de transacciones diarias

```http
POST http://localhost:8081/api/transacciones/procesar
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/transacciones/procesar"
```

Respuesta esperada:

```json
{
  "jobExecutionId": 1,
  "estado": "COMPLETED",
  "exitStatus": "COMPLETED"
}
```

### Job 2: Calculo de intereses mensuales

```http
POST http://localhost:8081/api/intereses/procesar
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/intereses/procesar"
```

Respuesta esperada:

```json
{
  "jobExecutionId": 2,
  "estado": "COMPLETED",
  "exitStatus": "COMPLETED"
}
```

### Job 3: Estados de cuenta anuales

```http
POST http://localhost:8081/api/cuentas-anuales/procesar
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/cuentas-anuales/procesar"
```

Respuesta esperada:

```json
{
  "jobExecutionId": 3,
  "estado": "COMPLETED",
  "exitStatus": "COMPLETED"
}
```

## 12. Endpoints para consultar resultados

### Transacciones procesadas

```http
GET http://localhost:8081/api/resultados/transacciones
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/transacciones" | ConvertTo-Json -Depth 5
```

### Resumen de transacciones

```http
GET http://localhost:8081/api/resultados/transacciones/resumen
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/transacciones/resumen" | ConvertTo-Json -Depth 5
```

### Intereses procesados

```http
GET http://localhost:8081/api/resultados/intereses
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/intereses" | ConvertTo-Json -Depth 5
```

### Movimientos anuales procesados

```http
GET http://localhost:8081/api/resultados/cuentas-anuales
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/cuentas-anuales" | ConvertTo-Json -Depth 5
```

### Resumen anual por cuenta

```http
GET http://localhost:8081/api/resultados/cuentas-anuales/resumen
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/cuentas-anuales/resumen" | ConvertTo-Json -Depth 5
```

### Evidencia tecnica de ejecucion batch

Estos endpoints fueron agregados para documentar la ejecucion real de los tres Jobs sobre los datos oficiales y obtener mediciones cuantitativas:

```http
GET http://localhost:8081/api/resultados/evidencia/jobs
GET http://localhost:8081/api/resultados/evidencia/steps
GET http://localhost:8081/api/resultados/evidencia/resultados-oficiales
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/evidencia/jobs" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/evidencia/steps" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/evidencia/resultados-oficiales" | ConvertTo-Json -Depth 5
```

Que debe observarse:

- `evidencia/jobs`: nombre del Job, estado, exit status, fecha de inicio, fecha de termino y duracion en milisegundos.
- `evidencia/steps`: nombre del Step, leidos, escritos, skips, commits, rollbacks y duracion en milisegundos.
- `evidencia/resultados-oficiales`: total de registros procesados, validos y rechazados en cada tabla de negocio.

### APIs BFF para Semana 4

Antes de consultar estas APIs se deben ejecutar los tres Jobs batch, porque los BFF consumen la informacion ya procesada y almacenada en MySQL.

#### BFF Web: dashboard completo

```http
GET http://localhost:8081/api/bff/web/dashboard
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/web/dashboard" -Headers @{"X-BFF-Token"="web-token-banco-xyz"} | ConvertTo-Json -Depth 8
```

Esta respuesta esta pensada para un frontend web de analistas o auditoria. Incluye metricas de ejecucion, resultados oficiales, anomalias recientes, saldos calculados y resumen anual.

#### BFF Mobile: resumen compacto

```http
GET http://localhost:8081/api/bff/mobile/resumen
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/mobile/resumen" -Headers @{"X-BFF-Token"="mobile-token-banco-xyz"} | ConvertTo-Json -Depth 8
```

Esta respuesta reduce el volumen de datos y entrega solo resumen, alertas, cuentas destacadas y ultimos resumenes anuales.

#### BFF Cajero automatico: consulta por cuenta

```http
GET http://localhost:8081/api/bff/atm/cuentas/103
```

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/atm/cuentas/103" -Headers @{"X-BFF-Token"="atm-token-banco-xyz"} | ConvertTo-Json -Depth 8
```

Esta respuesta entrega solo informacion necesaria para un cajero automatico: cuenta consultada, saldo, resumen anual y ultimos movimientos.

Operacion de retiro por cajero:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/bff/atm/cuentas/103/retiros" -Headers @{"X-BFF-Token"="atm-token-banco-xyz"} -ContentType "application/json" -Body '{"monto":10000}' | ConvertTo-Json -Depth 8
```

Que debe observarse:

- El campo `canal` cambia entre `WEB`, `MOBILE` y `ATM`.
- La respuesta web es mas completa y orientada a auditoria.
- La respuesta mobile es mas breve y orientada a resumen.
- La respuesta ATM exige un `cuentaId` y retorna datos minimos de una cuenta.
- El retiro ATM valida monto, limite por cajero y saldo disponible antes de aprobar o rechazar la operacion.
- Los tres endpoints consumen las mismas tablas MySQL, pero no entregan el mismo contrato al frontend.

La guia detallada de capturas para esta parte esta en:

```text
docs/evidencia-bff.md
```

Para medir tiempos y tamanos reales de respuesta por canal se incluye:

```powershell
.\medir-bff.ps1
```

El script ejecuta las APIs BFF con sus tokens, calcula `duracion_ms` y `payload_kb`, muestra una tabla en consola y guarda evidencia en:

```text
target/evidencias/bff-metricas.json
target/evidencias/bff-metricas.csv
```

## 13. Consultas directas en MySQL

Entrar a MySQL usando el usuario del proyecto:

```powershell
mysql -u bankxyz -p bankxyz_batch
```

Cuando pida password, escribir `bankxyz`.

Si estas usando el contenedor opcional del proyecto:

```powershell
docker exec -it bankxyz-mysql mysql -u bankxyz -pbankxyz bankxyz_batch
```

Consultar estados por tabla:

```sql
SELECT estado, COUNT(*) FROM transaccion_procesada GROUP BY estado;
SELECT estado, COUNT(*) FROM interes_procesado GROUP BY estado;
SELECT estado, COUNT(*) FROM movimiento_anual_procesado GROUP BY estado;
```

Consultar resumen de transacciones:

```sql
SELECT * FROM resumen_transacciones_diarias;
```

Consultar resumen anual:

```sql
SELECT * FROM resumen_anual_cuenta ORDER BY cuenta_id;
```

Salir de MySQL:

```text
EXIT;
```

## 14. Evidencia de ejecucion: capturas recomendadas

Para la entrega, se recomienda tomar capturas con `Win + Shift + S` en Windows y guardar las imagenes con nombres ordenados, por ejemplo `01-estructura-proyecto.png`, `02-config-mysql.png`, etc.

### Captura 1: estructura del proyecto

Abrir la carpeta raiz `BackEnd-main` en VS Code o IntelliJ y mostrar:

- `pom.xml`
- `docker-compose.yml`
- `README.md`
- `src/main/java`
- `src/main/resources`
- `src/main/resources/data`
- `semana_3`

Esta captura demuestra que el proyecto esta organizado y contiene los CSV oficiales que se leen durante la ejecucion.

### Captura 2: dependencia MySQL en Maven

Abrir `pom.xml` y capturar:

- `spring-boot-starter-batch`
- `spring-boot-starter-web`
- `spring-boot-starter-jdbc`
- `com.mysql:mysql-connector-j`
- `spring-batch-test`

Esta captura respalda que el proyecto usa Spring Batch y una base relacional valida para la pauta.

### Captura 3: configuracion de base de datos y archivos CSV

Abrir `src/main/resources/application.properties` y capturar:

- `spring.datasource.url=jdbc:mysql://localhost:3306/bankxyz_batch`
- `spring.datasource.username=bankxyz`
- `spring.batch.job.enabled=false`
- `bankxyz.archivo-transacciones=classpath:data/transacciones.csv`
- `bankxyz.archivo-intereses=classpath:data/intereses.csv`
- `bankxyz.archivo-cuentas-anuales=classpath:data/cuentas_anuales.csv`

Esta captura demuestra que los Jobs se ejecutan manualmente y leen los CSV configurados.

### Captura 4: tablas creadas

Abrir `src/main/resources/schema.sql` y capturar:

- `transaccion_procesada`
- `resumen_transacciones_diarias`
- `interes_procesado`
- `movimiento_anual_procesado`
- `resumen_anual_cuenta`

Esta captura demuestra la persistencia de los resultados procesados.

### Captura 5: escalamiento multithread

Abrir `CommonBatchConfig.java` y capturar:

- `ThreadPoolTaskExecutor`
- `@Value("${bankxyz.batch.thread-count:3}")`
- `setCorePoolSize(threadCount)`
- `setMaxPoolSize(threadCount)`
- `setQueueCapacity(queueCapacity)`
- `setThreadNamePrefix("bankxyz-batch-")`

Esta captura responde directamente al requerimiento de politicas de escalamiento y muestra que la cantidad de hilos puede variarse para medir configuraciones distintas.

### Captura 6: Job de transacciones

Abrir `TransaccionBatchConfig.java` y capturar:

- `transaccionesJob`
- `transaccionStep`
- `FlatFileItemReader`
- `TransaccionProcessor`
- `JdbcBatchItemWriter`
- `.taskExecutor(batchTaskExecutor)`
- `.faultTolerant()`
- `.skipLimit(20)`
- `.retryLimit(3)`

Esta captura demuestra lectura, procesamiento, escritura, tolerancia a fallos y escalamiento para el primer proceso.

### Captura 7: Job de intereses

Abrir `InteresBatchConfig.java` y capturar:

- `interesesJob`
- `interesStep`
- `InteresProcessor`
- `JdbcBatchItemWriter`
- `.taskExecutor(batchTaskExecutor)`
- `.faultTolerant()`

Esta captura demuestra el segundo proceso batch.

### Captura 8: Job de cuentas anuales

Abrir `CuentaAnualBatchConfig.java` y capturar:

- `cuentasAnualesJob`
- `cuentaAnualStep`
- `resumenAnualStep`
- `CuentaAnualProcessor`
- `ResumenAnualTasklet`

Esta captura demuestra el tercer proceso y la generacion del resumen anual.

### Captura 9: validaciones de transacciones

Abrir `TransaccionProcessor.java` y capturar:

- Validacion de tipo `debito` / `credito`.
- Parseo de fechas con multiples formatos.
- Validacion de monto mayor que cero.
- Deteccion de duplicados.
- Estado `RECHAZADA` con `motivoRechazo`.

Esta captura demuestra manejo de datos incorrectos y mal clasificados.

### Captura 10: validaciones de intereses

Abrir `InteresProcessor.java` y capturar:

- Mapa de tasas por tipo de cuenta.
- Validacion de edad.
- Validacion de saldo.
- Calculo de `interesCalculado`.
- Calculo de `saldoFinal`.

Esta captura demuestra transformacion de datos.

### Captura 11: validaciones de cuentas anuales

Abrir `CuentaAnualProcessor.java` y capturar:

- Tipos validos `deposito`, `retiro`, `compra`.
- Rechazo de tipos no reconocidos.
- Rechazo de descripcion vacia.
- Rechazo de monto cero o no numerico.
- Rechazo de depositos negativos.

Esta captura demuestra consistencia del informe anual.

### Captura 12: MySQL listo

Si usas tu MySQL local, ejecutar:

```powershell
mysql -u bankxyz -p bankxyz_batch
```

Capturar que puedes entrar a la base `bankxyz_batch`.

Si usas Docker como alternativa opcional, ejecutar:

```powershell
docker compose up -d
docker ps
```

Capturar la terminal mostrando el contenedor `bankxyz-mysql` activo.

### Captura 13: aplicacion iniciada correctamente

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Capturar la terminal cuando aparezca que Spring Boot inicio en el puerto `8081`.

### Captura 14: ejecucion del Job 1

En otra terminal ejecutar:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/transacciones/procesar"
```

Capturar la respuesta con:

- `estado: COMPLETED`
- `exitStatus: COMPLETED`
- `jobExecutionId`

Tambien capturar la consola de la aplicacion mostrando logs del step `transaccionStep`.

### Captura 15: resultados del Job 1

Ejecutar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/transacciones/resumen" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/transacciones" | ConvertTo-Json -Depth 5
```

Capturar registros `VALIDA` y `RECHAZADA`.

### Captura 16: ejecucion del Job 2

Ejecutar:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/intereses/procesar"
```

Capturar `COMPLETED`.

### Captura 17: resultados del Job 2

Ejecutar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/intereses" | ConvertTo-Json -Depth 5
```

Capturar cuentas con:

- `saldoOriginal`
- `tasaInteres`
- `interesCalculado`
- `saldoFinal`
- `estado`
- `motivoRechazo`

### Captura 18: ejecucion del Job 3

Ejecutar:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/cuentas-anuales/procesar"
```

Capturar `COMPLETED`.

### Captura 19: resultados del Job 3

Ejecutar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/cuentas-anuales/resumen" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/cuentas-anuales" | ConvertTo-Json -Depth 5
```

Capturar el resumen por cuenta y algunos registros rechazados.

### Captura 20: evidencia directa en MySQL

Ejecutar con MySQL local:

```powershell
mysql -u bankxyz -p bankxyz_batch
```

O, si usas el contenedor opcional:

```powershell
docker exec -it bankxyz-mysql mysql -u bankxyz -pbankxyz bankxyz_batch
```

Luego:

```sql
SELECT estado, COUNT(*) FROM transaccion_procesada GROUP BY estado;
SELECT estado, COUNT(*) FROM interes_procesado GROUP BY estado;
SELECT estado, COUNT(*) FROM movimiento_anual_procesado GROUP BY estado;
SELECT * FROM resumen_transacciones_diarias;
SELECT * FROM resumen_anual_cuenta ORDER BY cuenta_id;
```

Capturar la salida. Esta es una de las evidencias mas fuertes porque muestra que los datos quedaron persistidos en una base relacional.

### Captura 21: controladores BFF por tipo de cliente

En VS Code abrir la carpeta:

```text
src/main/java/cl/duoc/bankxyz/migracion/controllers
```

Capturar que existen estas clases:

- `WebBffController.java`
- `MobileBffController.java`
- `AtmBffController.java`

En la captura deben verse los `@RequestMapping`:

- `/api/bff/web`
- `/api/bff/mobile`
- `/api/bff/atm`

Esta evidencia demuestra que se implemento un backend especifico por canal.

### Captura 22: servicio BFF que adapta los datos

Abrir:

```text
src/main/java/cl/duoc/bankxyz/migracion/services/bff
```

Capturar los metodos:

- `WebBffService.dashboard()`
- `MobileBffService.resumen()`
- `AtmBffService.consultaCuenta(Long cuentaId)`
- `AtmBffService.retirar(Long cuentaId, BigDecimal monto)`

Capturar las clases `WebBffService`, `MobileBffService`, `AtmBffService` y `BffDataGateway`. Esta evidencia muestra que cada canal recibe una estrategia de respuesta distinta usando los mismos datos procesados en MySQL.

### Captura 22.1: seguridad BFF por token

Abrir:

```text
src/main/java/cl/duoc/bankxyz/migracion/config/BffTokenInterceptor.java
```

Capturar que el interceptor valida el header:

```text
X-BFF-Token
```

Luego abrir:

```text
src/main/resources/application.properties
```

Capturar los tokens configurables:

- `BFF_WEB_TOKEN`
- `BFF_MOBILE_TOKEN`
- `BFF_ATM_TOKEN`

Esta evidencia demuestra autenticacion y autorizacion diferenciada por canal.

### Captura 23: ejecucion API BFF Web

Con la aplicacion corriendo y los tres Jobs ejecutados, abrir una segunda terminal en VS Code y ejecutar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/web/dashboard" -Headers @{"X-BFF-Token"="web-token-banco-xyz"} | ConvertTo-Json -Depth 8
```

Capturar que la respuesta contiene:

- `canal: WEB`
- `resumenTransacciones`
- `resumenResultados`
- `rendimientoBatch`
- `anomaliasRecientes`
- `saldosCalculados`
- `resumenAnual`

### Captura 24: ejecucion API BFF Mobile

Ejecutar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/mobile/resumen" -Headers @{"X-BFF-Token"="mobile-token-banco-xyz"} | ConvertTo-Json -Depth 8
```

Capturar que la respuesta contiene:

- `canal: MOBILE`
- `resumen`
- `alertas`
- `cuentasDestacadas`
- `ultimosResumenesAnuales`

La captura debe permitir explicar que mobile recibe menos informacion que web.

### Captura 25: ejecucion API BFF Cajero automatico

Primero obtener una cuenta valida desde intereses:

```powershell
(Invoke-RestMethod -Uri "http://localhost:8081/api/resultados/intereses")[0]
```

Luego usar el `cuentaId` mostrado en la respuesta. Ejemplo:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/atm/cuentas/103" -Headers @{"X-BFF-Token"="atm-token-banco-xyz"} | ConvertTo-Json -Depth 8
```

Capturar que la respuesta contiene:

- `canal: ATM`
- `cuentaId`
- `saldo`
- `resumenAnual`
- `ultimosMovimientos`

Esta evidencia demuestra que el cajero automatico recibe solo los datos minimos para operar.

### Captura 26: retiro seguro por BFF Cajero automatico

Ejecutar:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/bff/atm/cuentas/103/retiros" -Headers @{"X-BFF-Token"="atm-token-banco-xyz"} -ContentType "application/json" -Body '{"monto":10000}' | ConvertTo-Json -Depth 8
```

Capturar que la respuesta contiene:

- `canal: ATM`
- `operacion: RETIRO`
- `estado: APROBADA` o `RECHAZADA`
- `saldoAnterior`
- `saldoPosterior`
- `motivo`, si el retiro fue rechazado

Esta evidencia demuestra una operacion critica protegida por token y validada por reglas de negocio.

### Captura 27: token incorrecto o faltante

Ejecutar una llamada sin token:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/web/dashboard"
```

Debe responder `401`.

Luego ejecutar con token de otro canal:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/bff/web/dashboard" -Headers @{"X-BFF-Token"="mobile-token-banco-xyz"}
```

Debe responder `403`.

Esta captura respalda autorizacion diferenciada: cada BFF acepta solo su token.

### Captura 28: HTTPS configurado

Ejecutar una vez:

```powershell
.\generar-certificado-dev.ps1
```

Luego iniciar la aplicacion con:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=https"
```

Capturar que la aplicacion inicia en el puerto `8443` con SSL activo.

Probar una API usando HTTPS:

```powershell
Invoke-RestMethod -Uri "https://localhost:8443/api/bff/mobile/resumen" -Headers @{"X-BFF-Token"="mobile-token-banco-xyz"} -SkipCertificateCheck | ConvertTo-Json -Depth 8
```

Esta evidencia demuestra configuracion segura mediante HTTPS y certificado local.

### Captura 29: medicion de tiempos y tamanos BFF

Con la aplicacion corriendo y los tres Jobs ejecutados, ejecutar:

```powershell
.\medir-bff.ps1
```

Capturar la tabla con:

- `canal`
- `duracion_ms`
- `payload_bytes`
- `payload_kb`

Esta evidencia demuestra optimizacion del consumo de recursos por canal. Web debe mostrar un payload mayor porque entrega informacion completa; mobile y ATM deben mostrar respuestas mas pequenas.

## 15. Versionamiento en GitHub

Desde la raiz del proyecto:

```powershell
git init
git status
git add .
git commit -m "Implementa Spring Batch y BFF para Banco XYZ"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
git push -u origin main
```

Si el repositorio ya existe y ya tiene remoto:

```powershell
git remote -v
git add .
git commit -m "Agrega patron BFF para canales Banco XYZ"
git push
```

En la entrega, incluir la URL del repositorio GitHub.

## 16. Problemas comunes

### Maven no se reconoce como comando

Este proyecto usa Maven Wrapper, por lo que no es obligatorio tener Maven instalado globalmente. En Windows se debe ejecutar `.\mvnw.cmd` en vez de `mvn`.

Verificacion:

```powershell
.\mvnw.cmd -version
```

Si aparece que `.\mvnw.cmd` no existe, revisar que en la raiz del proyecto esten los archivos `mvnw.cmd`, `mvnw` y la carpeta `.mvn/wrapper`.

### MySQL no conecta

Confirmar que MySQL esta iniciado. Si usas Docker, confirmar que el contenedor existe:

```powershell
docker compose up -d
docker ps
```

Confirmar que el puerto `3306` no este ocupado por otra instalacion de MySQL.

### El puerto 8081 esta ocupado

Cambiar el puerto en `application.properties`:

```properties
server.port=8082
```

Luego llamar los endpoints usando el nuevo puerto.

### Los resultados se duplican al ejecutar varias veces

El esquema se recrea al iniciar la aplicacion porque `spring.sql.init.mode=always`. Si se ejecutan los Jobs varias veces sin reiniciar la aplicacion, se agregan nuevas filas con nuevos `jobExecutionId`. Para partir limpio, detener y volver a iniciar la aplicacion.

## 17. Como continuar el proyecto de forma escalable

Mejoras posibles:

- Agregar tests unitarios para cada processor.
- Agregar tests de integracion con Testcontainers y MySQL.
- Separar perfiles `dev`, `test` y `prod`.
- Recibir rutas de archivos por parametros del Job.
- Agregar particiones si los CSV crecen a millones de registros.
- Exportar reportes finales a CSV, Excel o PDF.
- Agregar observabilidad con metricas de Spring Actuator.
- Crear un pipeline CI/CD en GitHub Actions para compilar y ejecutar pruebas.
