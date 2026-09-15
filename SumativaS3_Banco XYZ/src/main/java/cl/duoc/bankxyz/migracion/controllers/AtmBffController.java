package cl.duoc.bankxyz.migracion.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bankxyz.migracion.dtos.RetiroCajeroRequest;
import cl.duoc.bankxyz.migracion.services.bff.AtmBffService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/atm")
@RequiredArgsConstructor
public class AtmBffController {

    private final AtmBffService atmBffService;

    @GetMapping("/cuentas/{cuentaId}")
    public Map<String, Object> cuenta(@PathVariable Long cuentaId) {
        return atmBffService.consultaCuenta(cuentaId);
    }

    @PostMapping("/cuentas/{cuentaId}/retiros")
    public Map<String, Object> retirar(@PathVariable Long cuentaId, @RequestBody RetiroCajeroRequest request) {
        return atmBffService.retirar(cuentaId, request.monto());
    }
}
