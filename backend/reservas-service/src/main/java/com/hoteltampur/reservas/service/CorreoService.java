package com.hoteltampur.reservas.service;

import com.hoteltampur.reservas.model.Reserva;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

    private final JavaMailSender mailSender;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarConfirmacion(Reserva r) {
        if (r.correo() == null || r.correo().isBlank()) {
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setTo(r.correo());
            helper.setSubject("Confirmación de reserva " + r.codigo() + " · Hotel Támpur");
            helper.setText(contenidoHtml(r), true);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo de confirmación", e);
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
