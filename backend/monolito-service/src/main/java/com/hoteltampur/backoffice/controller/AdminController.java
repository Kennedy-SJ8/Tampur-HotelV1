package com.hoteltampur.backoffice.controller;

import com.hoteltampur.backoffice.service.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    public record LoginRequest(String usuario, String clave) {
    }

    private final AdminAuthService auth;

    public AdminController(AdminAuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body == null || body.usuario() == null || body.clave() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuario y contraseña son obligatorios."));
        }
        String token = auth.login(body.usuario(), body.clave());
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Usuario o contraseña incorrectos."));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        auth.logout(body.get("token"));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
