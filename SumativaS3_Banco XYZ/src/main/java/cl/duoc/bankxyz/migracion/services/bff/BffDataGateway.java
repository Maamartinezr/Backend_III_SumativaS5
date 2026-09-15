package cl.duoc.bankxyz.migracion.services.bff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BffDataGateway {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> consultarPrimero(String sql, Object... args) {
        List<Map<String, Object>> resultados = consultarLista(sql, args);
        return resultados.isEmpty() ? Map.of() : resultados.get(0);
    }

    public List<Map<String, Object>> consultarLista(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args);
    }

    public BigDecimal consultarBigDecimal(String sql, Object... args) {
        List<BigDecimal> resultados = jdbcTemplate.queryForList(sql, BigDecimal.class, args);
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    public int actualizar(String sql, Object... args) {
        return jdbcTemplate.update(sql, args);
    }

    public List<Map<String, Object>> resumenResultadosOficiales() {
        return consultarLista("""
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
