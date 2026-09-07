package com.hoteltampur.pagos.controller;

import com.hoteltampur.pagos.model.PagoEntity;
import com.hoteltampur.pagos.repository.PagoRepository;
import com.hoteltampur.pagos.service.CorreoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PagosController {

    public record PagoRequest(String codigoReserva, String metodo, String voucherUrl,
                              String nombre, String correo, Double monto) {
    }

    public record Pago(String id, String codigoReserva, String metodo, String estado, String voucher) {
    }

    private static final Logger log = LoggerFactory.getLogger(PagosController.class);
    private final PagoRepository pagoRepo;
    private final CorreoService correoService;

    public PagosController(PagoRepository pagoRepo, CorreoService correoService) {
        this.pagoRepo = pagoRepo;
        this.correoService = correoService;
    }

    @PostMapping("/pagos")
    public Pago procesarPago(@RequestBody PagoRequest r) {
        String id = UUID.randomUUID().toString();
        boolean digital = r.metodo() != null && (r.metodo().equalsIgnoreCase("tarjeta")
                || r.metodo().equalsIgnoreCase("yape")
                || r.metodo().equalsIgnoreCase("plin"));
        String estado = digital ? "APROBADO" : "PENDIENTE_VOUCHER";
        String voucher = "VCH-" + id.substring(0, 8).toUpperCase();

        PagoEntity entity = new PagoEntity(id, r.codigoReserva(), r.metodo(), estado, voucher);
        pagoRepo.save(entity);

        if ("APROBADO".equals(estado)) {
            try {
                correoService.enviarConfirmacionPago(r.correo(), r.nombre(), r.codigoReserva(),
                        voucher, r.metodo(), r.monto());
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo de pago: {}", e.getMessage());
            }
        }

        return new Pago(entity.getId(), entity.getCodigoReserva(),
                entity.getMetodo(), entity.getEstado(), entity.getVoucher());
    }

    @GetMapping("/pagos/{id}")
    public ResponseEntity<Pago> obtenerPago(@PathVariable String id) {
        return pagoRepo.findById(id)
                .map(e -> new Pago(e.getId(), e.getCodigoReserva(),
                        e.getMetodo(), e.getEstado(), e.getVoucher()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
