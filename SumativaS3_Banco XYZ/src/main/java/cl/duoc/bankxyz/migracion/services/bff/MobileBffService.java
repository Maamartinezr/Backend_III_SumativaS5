package cl.duoc.bankxyz.migracion.services.bff;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MobileBffService {

    private final BffDataGateway dataGateway;

    public Map<String, Object> resumen() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("canal", "MOBILE");
        response.put("objetivo", "Vista compacta para pantallas pequenas y bajo consumo de datos.");
        response.put("resumen", dataGateway.resumenResultadosOficiales());
        response.put("alertas", dataGateway.consultarLista("""
                SELECT origen, total_rechazados
                FROM (
                    SELECT 'transacciones' AS origen, COUNT(*) AS total_rechazados
                    FROM transaccion_procesada
                    WHERE estado = 'RECHAZADA'
                    UNION ALL
                    SELECT 'intereses' AS origen, COUNT(*) AS total_rechazados
                    FROM interes_procesado
                    WHERE estado = 'RECHAZADA'
                    UNION ALL
                    SELECT 'cuentas_anuales' AS origen, COUNT(*) AS total_rechazados
                    FROM movimiento_anual_procesado
                    WHERE estado = 'RECHAZADA'
                ) alertas
                """));
        response.put("cuentasDestacadas", dataGateway.consultarLista("""
                SELECT cuenta_id, nombre, tipo_cuenta, saldo_final
                FROM interes_procesado
                WHERE estado = 'VALIDA'
                ORDER BY saldo_final DESC
                LIMIT 10
                """));
        response.put("ultimosResumenesAnuales", dataGateway.consultarLista("""
                SELECT cuenta_id, saldo_neto, cantidad_movimientos
                FROM resumen_anual_cuenta
                ORDER BY cuenta_id
                LIMIT 10
                """));
        return response;
    }
}
