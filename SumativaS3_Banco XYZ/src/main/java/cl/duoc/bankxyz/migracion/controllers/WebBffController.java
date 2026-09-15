package cl.duoc.bankxyz.migracion.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bankxyz.migracion.services.bff.WebBffService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/web")
@RequiredArgsConstructor
public class WebBffController {

    private final WebBffService webBffService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return webBffService.dashboard();
    }
}
