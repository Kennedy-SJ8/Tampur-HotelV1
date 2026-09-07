package com.hoteltampur.pagos.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private final JavaMailSender mailSender;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarConfirmacionPago(String correo, String nombre, String codigoReserva,
                                       String voucher, String metodo, Double monto) {
        if (correo == null || correo.isBlank()) {
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setTo(correo);
            helper.setSubject("Pago aprobado - Reserva " + codigoReserva + " · Hotel Támpur");
            helper.setText(contenidoHtml(nombre, codigoReserva, voucher, metodo, monto), true);
            mailSender.send(mensaje);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de pago {}: {}", codigoReserva, e.getMessage());
        }
    }

    private String contenidoHtml(String nombre, String codigoReserva, String voucher,
                                 String metodo, Double monto) {
        double total = monto != null ? monto : 0.0;
        String huesped = nombre != null && !nombre.isBlank() ? nombre : "huésped";
        return """
                <html>
                  <body style="font-family:Arial,sans-serif;color:#24302b;margin:0;padding:0;background:#f6f2ea">
                    <div style="max-width:560px;margin:20px auto;border:1px solid #e3ded2;border-radius:12px;overflow:hidden;background:#fff">
                      <div style="background:#c96f3b;color:#fff;padding:22px">
                        <h1 style="margin:0;font-size:20px">Pago aprobado</h1>
                        <p style="margin:4px 0 0;opacity:.9;font-size:13px">Hotel Támpur · San Mateo, Huarochirí</p>
                      </div>
                      <div style="padding:24px">
                        <h2 style="margin:0 0 14px;color:#1f3d34;font-size:17px">Gracias por tu pago</h2>
                        <p style="font-size:14px">Hola <b>%s</b>,</p>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px">
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Código de reserva</td><td style="padding:8px;border-bottom:1px solid #eee"><b>%s</b></td></tr>
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Voucher</td><td style="padding:8px;border-bottom:1px solid #eee"><b>%s</b></td></tr>
                          <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#6b756f">Método</td><td style="padding:8px;border-bottom:1px solid #eee">%s</td></tr>
                          <tr><td style="padding:8px;color:#6b756f">Monto</td><td style="padding:8px"><b>S/ %.2f</b></td></tr>
                        </table>
                        <p style="font-size:13px;color:#6b756f;margin-top:16px">Check-in: 1:00 p.m. · Check-out: 12:00 p.m.<br>WhatsApp: 943 370 504</p>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(huesped, codigoReserva, voucher, metodo, total);
    }
}
