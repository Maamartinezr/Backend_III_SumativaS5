package cl.duoc.bankxyz.migracion.services.bff;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtmBffService {

    private static final BigDecimal LIMITE_RETIRO_ATM = new BigDecimal("200000");

    private final BffDataGateway dataGateway;

    public Map<String, Object> consultaCuenta(Long cuentaId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("canal", "ATM");
        response.put("objetivo", "Respuesta minima para cajero automatico: saldo, estado y ultimos movimientos.");
        response.put("cuentaId", cuentaId);
        response.put("saldo", dataGateway.consultarPrimero("""
                SELECT cuenta_id, nombre, tipo_cuenta, saldo_final, estado
                FROM interes_procesado
                WHERE cuenta_id = ? AND estado = 'VALIDA'
                ORDER BY id DESC
                LIMIT 1
                """, cuentaId));
        response.put("resumenAnual", dataGateway.consultarPrimero("""
                SELECT cuenta_id, saldo_neto, cantidad_movimientos
                FROM resumen_anual_cuenta
                WHERE cuenta_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, cuentaId));
        response.put("ultimosMovimientos", dataGateway.consultarLista("""
                SELECT fecha, tipo_transaccion, monto, descripcion
                FROM movimiento_anual_procesado
                WHERE cuenta_id = ? AND estado = 'VALIDA'
                ORDER BY fecha DESC
                LIMIT 5
                """, cuentaId));
        response.put("saldoOperativo", saldoOperativo(cuentaId));
        response.put("operacionesCajero", dataGateway.consultarLista("""
                SELECT fecha_operacion, tipo_operacion, monto, estado, motivo_rechazo
                FROM operacion_cajero
                WHERE cuenta_id = ?
                ORDER BY id DESC
                LIMIT 5
                """, cuentaId));
        response.put("mensaje", "Operacion disponible solo si los Jobs ya fueron ejecutados.");
        return response;
    }

    @Transactional
    public Map<String, Object> retirar(Long cuentaId, BigDecimal monto) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("canal", "ATM");
        response.put("operacion", "RETIRO");
        response.put("cuentaId", cuentaId);
        response.put("montoSolicitado", monto);
        response.put("fechaOperacion", LocalDateTime.now());

        BigDecimal saldoDisponible = saldoDisponible(cuentaId);
        if (saldoDisponible == null) {
            registrarOperacionCajero(cuentaId, monto, "RECHAZADA", "Cuenta no encontrada o sin saldo valido");
            response.put("estado", "RECHAZADA");
            response.put("motivo", "Cuenta no encontrada o sin saldo valido");
            return response;
        }

        String rechazo = validarRetiro(monto, saldoDisponible);
        if (rechazo != null) {
            registrarOperacionCajero(cuentaId, monto, "RECHAZADA", rechazo);
            response.put("estado", "RECHAZADA");
            response.put("motivo", rechazo);
            response.put("saldoDisponible", saldoDisponible);
            return response;
        }

        registrarOperacionCajero(cuentaId, monto, "APROBADA", null);
        response.put("estado", "APROBADA");
        response.put("saldoAnterior", saldoDisponible);
        response.put("saldoPosterior", saldoDisponible.subtract(monto));
        response.put("mensaje", "Retiro aprobado por BFF ATM");
        return response;
    }

    private Map<String, Object> saldoOperativo(Long cuentaId) {
        BigDecimal saldoDisponible = saldoDisponible(cuentaId);
        Map<String, Object> saldo = new LinkedHashMap<>();
        saldo.put("cuentaId", cuentaId);
        saldo.put("saldoDisponible", saldoDisponible);
        saldo.put("limiteRetiro", LIMITE_RETIRO_ATM);
        saldo.put("estado", saldoDisponible == null ? "NO_DISPONIBLE" : "DISPONIBLE");
        return saldo;
    }

    private BigDecimal saldoDisponible(Long cuentaId) {
        BigDecimal saldoBase = dataGateway.consultarBigDecimal("""
                SELECT saldo_final
                FROM interes_procesado
                WHERE cuenta_id = ? AND estado = 'VALIDA'
                ORDER BY id DESC
                LIMIT 1
                """, cuentaId);

        if (saldoBase == null) {
            return null;
        }

        BigDecimal retirosAprobados = dataGateway.consultarBigDecimal("""
                SELECT COALESCE(SUM(monto), 0)
                FROM operacion_cajero
                WHERE cuenta_id = ? AND tipo_operacion = 'RETIRO' AND estado = 'APROBADA'
                """, cuentaId);

        return saldoBase.subtract(retirosAprobados == null ? BigDecimal.ZERO : retirosAprobados);
    }

    private String validarRetiro(BigDecimal monto, BigDecimal saldoDisponible) {
        if (monto == null) {
            return "Monto requerido";
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "El monto debe ser mayor a cero";
        }
        if (monto.compareTo(LIMITE_RETIRO_ATM) > 0) {
            return "El monto supera el limite permitido por cajero";
        }
        if (monto.compareTo(saldoDisponible) > 0) {
            return "Saldo insuficiente";
        }
        return null;
    }

    private void registrarOperacionCajero(Long cuentaId, BigDecimal monto, String estado, String motivoRechazo) {
        dataGateway.actualizar("""
                INSERT INTO operacion_cajero
                    (cuenta_id, fecha_operacion, tipo_operacion, monto, estado, motivo_rechazo)
                VALUES (?, CURRENT_TIMESTAMP, 'RETIRO', ?, ?, ?)
                """, cuentaId, monto, estado, motivoRechazo);
    }
}
