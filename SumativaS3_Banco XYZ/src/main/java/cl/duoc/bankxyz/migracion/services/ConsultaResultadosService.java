package cl.duoc.bankxyz.migracion.services;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Servicio de solo lectura para exponer, via REST, el contenido de las
 * tablas que dejan los 3 Jobs. Pensado para que la evidencia de ejecucion
 * (capturas de Postman) muestre los datos ya procesados sin depender de la
 * consola de MySQL.
 */
@Service
@RequiredArgsConstructor
public class ConsultaResultadosService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> transaccionesProcesadas() {
        return jdbcTemplate.queryForList("SELECT * FROM transaccion_procesada ORDER BY id");
    }

    public List<Map<String, Object>> resumenTransaccionesDiarias() {
        return jdbcTemplate.queryForList("SELECT * FROM resumen_transacciones_diarias ORDER BY id");
    }

    public List<Map<String, Object>> interesesProcesados() {
        return jdbcTemplate.queryForList("SELECT * FROM interes_procesado ORDER BY id");
    }

    public List<Map<String, Object>> movimientosAnualesProcesados() {
        return jdbcTemplate.queryForList("SELECT * FROM movimiento_anual_procesado ORDER BY id");
    }

    public List<Map<String, Object>> resumenAnualPorCuenta() {
        return jdbcTemplate.queryForList("SELECT * FROM resumen_anual_cuenta ORDER BY id");
    }

    public List<Map<String, Object>> evidenciaJobs() {
        return jdbcTemplate.queryForList("""
                SELECT
                    ji.JOB_NAME AS job,
                    je.JOB_EXECUTION_ID AS job_execution_id,
                    je.STATUS AS estado,
                    je.EXIT_CODE AS exit_status,
                    je.CREATE_TIME AS fecha_creacion,
                    je.START_TIME AS inicio,
                    je.END_TIME AS termino,
                    ROUND(TIMESTAMPDIFF(MICROSECOND, je.START_TIME, je.END_TIME) / 1000, 2) AS duracion_ms
                FROM BATCH_JOB_EXECUTION je
                INNER JOIN BATCH_JOB_INSTANCE ji
                    ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
                ORDER BY je.JOB_EXECUTION_ID DESC
                """);
    }

    public List<Map<String, Object>> evidenciaSteps() {
        return jdbcTemplate.queryForList("""
                SELECT
                    ji.JOB_NAME AS job,
                    se.STEP_NAME AS step,
                    se.JOB_EXECUTION_ID AS job_execution_id,
                    se.STATUS AS estado,
                    se.READ_COUNT AS leidos,
                    se.WRITE_COUNT AS escritos,
                    se.READ_SKIP_COUNT AS omitidos_lectura,
                    se.PROCESS_SKIP_COUNT AS omitidos_proceso,
                    se.WRITE_SKIP_COUNT AS omitidos_escritura,
                    se.COMMIT_COUNT AS commits,
                    se.ROLLBACK_COUNT AS rollbacks,
                    se.START_TIME AS inicio,
                    se.END_TIME AS termino,
                    ROUND(TIMESTAMPDIFF(MICROSECOND, se.START_TIME, se.END_TIME) / 1000, 2) AS duracion_ms
                FROM BATCH_STEP_EXECUTION se
                INNER JOIN BATCH_JOB_EXECUTION je
                    ON je.JOB_EXECUTION_ID = se.JOB_EXECUTION_ID
                INNER JOIN BATCH_JOB_INSTANCE ji
                    ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
                ORDER BY se.JOB_EXECUTION_ID DESC, se.STEP_EXECUTION_ID ASC
                """);
    }

    public List<Map<String, Object>> evidenciaResultadosOficiales() {
        return jdbcTemplate.queryForList("""
                SELECT
                    'transaccionesJob' AS job,
                    'transaccion_procesada' AS tabla,
                    COUNT(*) AS total_registros,
                    SUM(CASE WHEN estado = 'VALIDA' THEN 1 ELSE 0 END) AS total_validos,
                    SUM(CASE WHEN estado = 'RECHAZADA' THEN 1 ELSE 0 END) AS total_rechazados
                FROM transaccion_procesada
                UNION ALL
                SELECT
                    'interesesJob' AS job,
                    'interes_procesado' AS tabla,
                    COUNT(*) AS total_registros,
                    SUM(CASE WHEN estado = 'VALIDA' THEN 1 ELSE 0 END) AS total_validos,
                    SUM(CASE WHEN estado = 'RECHAZADA' THEN 1 ELSE 0 END) AS total_rechazados
                FROM interes_procesado
                UNION ALL
                SELECT
                    'cuentasAnualesJob' AS job,
                    'movimiento_anual_procesado' AS tabla,
                    COUNT(*) AS total_registros,
                    SUM(CASE WHEN estado = 'VALIDA' THEN 1 ELSE 0 END) AS total_validos,
                    SUM(CASE WHEN estado = 'RECHAZADA' THEN 1 ELSE 0 END) AS total_rechazados
                FROM movimiento_anual_procesado
                """);
    }
}
