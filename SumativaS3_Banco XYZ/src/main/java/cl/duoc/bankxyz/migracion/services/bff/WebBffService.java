package cl.duoc.bankxyz.migracion.services.bff;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebBffService {

    private final BffDataGateway dataGateway;

    public Map<String, Object> dashboard() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("canal", "WEB");
        response.put("objetivo", "Dashboard operativo completo para analistas y auditoria.");
        response.put("resumenTransacciones", dataGateway.consultarPrimero("""
                SELECT *
                FROM resumen_transacciones_diarias
                ORDER BY id DESC
                LIMIT 1
                """));
        response.put("resumenResultados", dataGateway.resumenResultadosOficiales());
        response.put("rendimientoBatch", dataGateway.consultarLista("""
                SELECT
                    ji.JOB_NAME AS job,
                    se.STEP_NAME AS step,
                    se.STATUS AS estado,
                    se.READ_COUNT AS leidos,
                    se.WRITE_COUNT AS escritos,
                    se.READ_SKIP_COUNT AS omitidos_lectura,
                    se.PROCESS_SKIP_COUNT AS omitidos_proceso,
                    se.WRITE_SKIP_COUNT AS omitidos_escritura,
                    se.COMMIT_COUNT AS commits,
                    se.ROLLBACK_COUNT AS rollbacks,
                    ROUND(TIMESTAMPDIFF(MICROSECOND, se.START_TIME, se.END_TIME) / 1000, 2) AS duracion_ms
                FROM BATCH_STEP_EXECUTION se
                INNER JOIN BATCH_JOB_EXECUTION je ON je.JOB_EXECUTION_ID = se.JOB_EXECUTION_ID
                INNER JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
                ORDER BY se.JOB_EXECUTION_ID DESC, se.STEP_EXECUTION_ID ASC
                LIMIT 20
                """));
        response.put("anomaliasRecientes", dataGateway.consultarLista("""
                SELECT 'transaccion' AS origen, transaccion_id AS referencia, fecha, tipo, monto, motivo_rechazo
                FROM transaccion_procesada
                WHERE estado = 'RECHAZADA'
                UNION ALL
                SELECT 'interes' AS origen, cuenta_id AS referencia, NULL AS fecha, tipo_cuenta AS tipo,
                    saldo_original AS monto, motivo_rechazo
                FROM interes_procesado
                WHERE estado = 'RECHAZADA'
                UNION ALL
                SELECT 'cuenta_anual' AS origen, cuenta_id AS referencia, fecha, tipo_transaccion AS tipo,
                    monto, motivo_rechazo
                FROM movimiento_anual_procesado
                WHERE estado = 'RECHAZADA'
                LIMIT 15
                """));
        response.put("saldosCalculados", dataGateway.consultarLista("""
                SELECT cuenta_id, nombre, tipo_cuenta, saldo_original, tasa_interes,
                    interes_calculado, saldo_final, estado
                FROM interes_procesado
                ORDER BY id DESC
                LIMIT 20
                """));
        response.put("resumenAnual", dataGateway.consultarLista("""
                SELECT cuenta_id, total_depositos, total_retiros_compras, saldo_neto, cantidad_movimientos
                FROM resumen_anual_cuenta
                ORDER BY cuenta_id
                LIMIT 20
                """));
        return response;
    }
}
