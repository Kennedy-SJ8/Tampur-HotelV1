package com.hoteltampur.backoffice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private final String usuario;
    private final String clave;

    private final Set<String> tokens = ConcurrentHashMap.newKeySet();

    public AdminAuthService(@Value("${admin.usuario}") String usuario,
                            @Value("${admin.clave}") String clave) {
        this.usuario = usuario;
        this.clave = clave;
    }

    public String login(String usuario, String clave) {
        if (usuario != null && clave != null
                && this.usuario.equals(usuario) && this.clave.equals(clave)) {
            String token = UUID.randomUUID().toString();
            tokens.add(token);
            return token;
        }
        return null;
    }

    public void logout(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    public boolean isValid(String token) {
        return token != null && tokens.contains(token);
    }
}
