-- =========================================================================
-- Tablas de negocio para los 3 procesos batch del Banco XYZ.
-- Se recrean cada vez que arranca la aplicacion (spring.sql.init.mode=always).
-- Incluye tambien las tablas internas de Spring Batch para que las pruebas
-- repetidas con distintas configuraciones de hilos partan desde un estado limpio.
-- =========================================================================

-- 0) Metadata interna de Spring Batch ---------------------------------------
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_SEQ;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_SEQ;
DROP TABLE IF EXISTS BATCH_JOB_SEQ;

CREATE TABLE BATCH_JOB_INSTANCE  (
    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION  (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME DATETIME(6) NOT NULL,
    START_TIME DATETIME(6) DEFAULT NULL,
    END_TIME DATETIME(6) DEFAULT NULL,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED DATETIME(6),
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
    REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME VARCHAR(100) NOT NULL,
    PARAMETER_TYPE VARCHAR(100) NOT NULL,
    PARAMETER_VALUE VARCHAR(2500),
    IDENTIFYING CHAR(1) NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
    REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION  (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME DATETIME(6) NOT NULL,
    START_TIME DATETIME(6) DEFAULT NULL,
    END_TIME DATETIME(6) DEFAULT NULL,
    STATUS VARCHAR(10),
    COMMIT_COUNT BIGINT,
    READ_COUNT BIGINT,
    FILTER_COUNT BIGINT,
    WRITE_COUNT BIGINT,
    READ_SKIP_COUNT BIGINT,
    WRITE_SKIP_COUNT BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT BIGINT,
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED DATETIME(6),
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
    REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
    REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
    REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_SEQ (
    ID BIGINT NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    CONSTRAINT BATCH_STEP_EXECUTION_SEQ_UN UNIQUE (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_STEP_EXECUTION_SEQ (ID, UNIQUE_KEY)
SELECT * FROM (SELECT 0 AS ID, '0' AS UNIQUE_KEY) AS tmp
WHERE NOT EXISTS (SELECT * FROM BATCH_STEP_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_EXECUTION_SEQ (
    ID BIGINT NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    CONSTRAINT BATCH_JOB_EXECUTION_SEQ_UN UNIQUE (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_JOB_EXECUTION_SEQ (ID, UNIQUE_KEY)
SELECT * FROM (SELECT 0 AS ID, '0' AS UNIQUE_KEY) AS tmp
WHERE NOT EXISTS (SELECT * FROM BATCH_JOB_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_SEQ (
    ID BIGINT NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    CONSTRAINT BATCH_JOB_SEQ_UN UNIQUE (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_JOB_SEQ (ID, UNIQUE_KEY)
SELECT * FROM (SELECT 0 AS ID, '0' AS UNIQUE_KEY) AS tmp
WHERE NOT EXISTS (SELECT * FROM BATCH_JOB_SEQ);

-- 1) Reporte de Transacciones Diarias --------------------------------------
DROP TABLE IF EXISTS transaccion_procesada;
CREATE TABLE transaccion_procesada (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaccion_id      BIGINT         NOT NULL,
    fecha               DATE,
    monto               DECIMAL(12, 2),
    tipo                VARCHAR(20),
    estado              VARCHAR(20)    NOT NULL,   -- VALIDA / RECHAZADA
    motivo_rechazo      VARCHAR(200)
);

DROP TABLE IF EXISTS resumen_transacciones_diarias;
CREATE TABLE resumen_transacciones_diarias (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_execution_id        BIGINT         NOT NULL,
    fecha_generacion        TIMESTAMP      NOT NULL,
    total_registros         INT            NOT NULL,
    total_validas           INT            NOT NULL,
    total_rechazadas        INT            NOT NULL,
    monto_total_creditos    DECIMAL(14, 2) NOT NULL,
    monto_total_debitos     DECIMAL(14, 2) NOT NULL
);

-- 2) Calculo de Intereses Mensuales ----------------------------------------
DROP TABLE IF EXISTS interes_procesado;
CREATE TABLE interes_procesado (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id           BIGINT         NOT NULL,
    nombre              VARCHAR(100),
    tipo_cuenta         VARCHAR(20),
    edad                INT,
    saldo_original      DECIMAL(12, 2),
    tasa_interes        DECIMAL(6, 4),
    interes_calculado   DECIMAL(12, 2),
    saldo_final         DECIMAL(12, 2),
    estado              VARCHAR(20)    NOT NULL,   -- VALIDA / RECHAZADA
    motivo_rechazo      VARCHAR(200)
);

DROP TABLE IF EXISTS operacion_cajero;
CREATE TABLE operacion_cajero (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id           BIGINT         NOT NULL,
    fecha_operacion     TIMESTAMP      NOT NULL,
    tipo_operacion      VARCHAR(20)    NOT NULL,
    monto               DECIMAL(12, 2),
    estado              VARCHAR(20)    NOT NULL,
    motivo_rechazo      VARCHAR(200)
);

-- 3) Generacion de Estados de Cuenta Anuales -------------------------------
DROP TABLE IF EXISTS movimiento_anual_procesado;
CREATE TABLE movimiento_anual_procesado (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id           BIGINT         NOT NULL,
    fecha               DATE,
    tipo_transaccion    VARCHAR(20),
    monto               DECIMAL(12, 2),
    descripcion         VARCHAR(200),
    estado              VARCHAR(20)    NOT NULL,   -- VALIDA / RECHAZADA
    motivo_rechazo      VARCHAR(200)
);

DROP TABLE IF EXISTS resumen_anual_cuenta;
CREATE TABLE resumen_anual_cuenta (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_execution_id        BIGINT         NOT NULL,
    cuenta_id                BIGINT         NOT NULL,
    total_depositos          DECIMAL(14, 2) NOT NULL,
    total_retiros_compras    DECIMAL(14, 2) NOT NULL,
    saldo_neto               DECIMAL(14, 2) NOT NULL,
    cantidad_movimientos     INT            NOT NULL
);
