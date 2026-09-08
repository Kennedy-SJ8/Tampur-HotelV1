package com.hoteltampur.reservas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoteltampur.reservas.model.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final String FROM_EMAIL = "kennedyjacay17@gmail.com";
    private static final String FROM_NAME = "Hotel Támpur";

    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CorreoService(@Value("${SENDGRID_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public void enviarConfirmacion(Reserva r) {
        if (r.correo() == null || r.correo().isBlank()) {
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY no configurado; no se envía correo a {}", r.correo());
            return;
        }
        try {
            enviar(r.correo(), "Confirmación de reserva " + r.codigo() + " · Hotel Támpur", contenidoHtml(r));
            log.info("Correo de confirmación enviado a {}", r.correo());
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de {}: {}", r.codigo(), e.getMessage());
        }
    }

    private void enviar(String para, String asunto, String html) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("personalizations", List.of(Map.of("to", List.of(Map.of("email", para)))));
        body.put("from", Map.of("email", FROM_EMAIL, "name", FROM_NAME));
        body.put("subject", asunto);
        body.put("content", List.of(Map.of("type", "text/html", "value", html)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("SendGrid " + response.statusCode() + ": " + response.body());
        }
    }

    private String contenidoHtml(Reserva r) {
        return """
                <html>
                  <body style="font-family:Arial,sans-serif;color:#24302b;margin:0;padding:0;background:#f6f2ea">
                    <div style="max-width:560px;margin:20px auto;border:1px solid #e3ded2;border-radius:12px;overflow:hidden;background:#fff">
                      <div style="background:#1f3d34;color:#fff;padding:22px">
                        <h1 style="margin:0;font-size:20px">Hotel Támpur</h1>
                        <p style="margin:4px 0 0;opacity:.85;font-size:13px">San Mateo · Huarochirí · Lima</p>
                      </div>
                      <div style="padding:24px">
                        <h2 style="margin:0 0 14px;color:#c96f3b;font-size:17px">¡Tu reserva está confirmada!</h2>
                        <p style="font-size:14px">Hola <b>%s</b>,</p>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px">
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Código</td><td style="padding:8px;border-bottom:1px solid #eee"><b>%s</b></td></tr>
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Habitación</td><td style="padding:8px;border-bottom:1px solid #eee">%s</td></tr>
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Check-in</td><td style="padding:8px;border-bottom:1px solid #eee">%s (1:00 p.m.)</td></tr>
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Check-out</td><td style="padding:8px;border-bottom:1px solid #eee">%s (12:00 p.m.)</td></tr>
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Noches</td><td style="padding:8px;border-bottom:1px solid #eee">%d</td></tr>
                          <tr><td style="padding:8px;color:#6b756f">Total</td><td style="padding:8px"><b>S/ %.2f</b></td></tr>
                        </table>
                        <p style="font-size:13px;color:#6b756f;margin-top:16px">
                          Ubicación: Magnolias 197, San Mateo, Huarochirí, Lima.<br>
                          WhatsApp: 943 370 504
                        </p>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                r.nombre(), r.codigo(), r.tipoHabitacion(),
                r.fechaEntrada(), r.fechaSalida(), r.noches(), r.total());
    }
}
