package cl.duoc.bankxyz.migracion.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bankxyz.migracion.services.bff.MobileBffService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/mobile")
@RequiredArgsConstructor
public class MobileBffController {

    private final MobileBffService mobileBffService;

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        return mobileBffService.resumen();
    }
}
