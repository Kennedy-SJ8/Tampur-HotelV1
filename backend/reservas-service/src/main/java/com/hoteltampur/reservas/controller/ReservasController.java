package com.hoteltampur.reservas.controller;

import com.hoteltampur.reservas.model.Habitacion;
import com.hoteltampur.reservas.model.HabitacionEntity;
import com.hoteltampur.reservas.model.Reserva;
import com.hoteltampur.reservas.model.ReservaEntity;
import com.hoteltampur.reservas.model.ReservaRequest;
import com.hoteltampur.reservas.repository.HabitacionRepository;
import com.hoteltampur.reservas.repository.ReservaRepository;
import com.hoteltampur.reservas.service.CorreoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowedHeaders = "*")
public class ReservasController {

    private static final Logger log = LoggerFactory.getLogger(ReservasController.class);
    private static final java.util.concurrent.ExecutorService emailExecutor =
            Executors.newFixedThreadPool(2);

    private static final Pattern PATRON_DNI = Pattern.compile("^[A-Za-z0-9]{6,12}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Map<String, Double> tarifas = Map.of(
            "Simple", 60.0,
            "Matrimonial", 80.0,
            "Queen", 120.0,
            "King", 140.0
    );

    private final HabitacionRepository habitacionRepo;
    private final ReservaRepository reservaRepo;
    private final CorreoService correoService;

    public ReservasController(HabitacionRepository habitacionRepo,
                              ReservaRepository reservaRepo,
                              CorreoService correoService) {
        this.habitacionRepo = habitacionRepo;
        this.reservaRepo = reservaRepo;
        this.correoService = correoService;
    }

    @GetMapping("/habitaciones")
    public List<Habitacion> habitaciones() {
        return habitacionRepo.findAll().stream()
                .map(HabitacionEntity::toRecord)
                .toList();
    }

    @GetMapping("/disponibilidad")
    public List<Habitacion> disponibilidad(@RequestParam LocalDate fechaEntrada,
                                            @RequestParam LocalDate fechaSalida) {
        List<HabitacionEntity> libres = habitacionRepo.findByEstado("Libre");
        return libres.stream()
                .filter(h -> {
                    long ocupadas = reservaRepo.countOcupadasPorTipo(
                            h.getTipo(), fechaEntrada, fechaSalida);
                    long totales = habitacionRepo.countByTipo(h.getTipo());
                    return ocupadas < totales;
                })
                .map(HabitacionEntity::toRecord)
                .toList();
    }

    @PostMapping("/reservas")
    public ResponseEntity<?> crearReserva(@RequestBody ReservaRequest r) {
        List<String> errores = validar(r);
        if (!errores.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errores", errores));
        }

        if (!hayDisponibilidad(r)) {
            return ResponseEntity.badRequest().body(Map.of("errores", List.of(
                    "No hay habitaciones disponibles de tipo " + r.tipoHabitacion() + " para esas fechas.")));
        }

        if (r.idempotencyKey() != null && !r.idempotencyKey().isBlank()) {
            ReservaEntity existente = reservaRepo.findById("KEY-" + r.idempotencyKey()).orElse(null);
            if (existente != null) {
                log.info("Solicitud repetida detectada por idempotencyKey");
                return ResponseEntity.ok(existente.toRecord());
            }
        }

        List<ReservaEntity> duplicadas = reservaRepo.buscarDuplicada(
                r.dni().trim(), r.tipoHabitacion(), r.fechaEntrada(), r.fechaSalida());
        if (!duplicadas.isEmpty()) {
            log.info("Reserva duplicada detectada por contenido");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Ya existe una reserva activa con estos datos.",
                    "reservaExistente", duplicadas.get(0).toRecord()
            ));
        }

        log.info("Guardando reserva...");
        ReservaEntity reserva = registrarReserva(r);
        log.info("Reserva guardada: {}", reserva.getCodigo());

        final Reserva record = reserva.toRecord();
        CompletableFuture.runAsync(() -> {
            try {
                correoService.enviarConfirmacion(record);
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo de {}: {}", record.codigo(), e.getMessage());
            }
        }, emailExecutor);

        return ResponseEntity.status(HttpStatus.CREATED).body(reserva.toRecord());
    }

    private List<String> validar(ReservaRequest r) {
        List<String> errores = new ArrayList<>();

        if (esVacio(r.nombre())) {
            errores.add("El nombre es obligatorio.");
        }
        if (esVacio(r.dni()) || !PATRON_DNI.matcher(r.dni().trim()).matches()) {
            errores.add("El DNI/pasaporte debe tener entre 6 y 12 caracteres alfanuméricos.");
        }
        if (!esVacio(r.correo()) && !PATRON_CORREO.matcher(r.correo().trim()).matches()) {
            errores.add("El correo no tiene un formato válido.");
        }
        if (esVacio(r.tipoHabitacion()) || !tarifas.containsKey(r.tipoHabitacion())) {
            errores.add("El tipo de habitación no es válido.");
        }
        if (r.fechaEntrada() == null || r.fechaSalida() == null) {
            errores.add("Debe indicar fecha de llegada y de salida.");
        } else {
            if (r.fechaEntrada().isBefore(LocalDate.now())) {
                errores.add("La fecha de llegada no puede ser anterior a hoy.");
            }
            if (!r.fechaSalida().isAfter(r.fechaEntrada())) {
                errores.add("La fecha de salida debe ser posterior a la de llegada.");
            }
        }
        return errores;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private boolean hayDisponibilidad(ReservaRequest r) {
        long libres = habitacionRepo.countByTipoAndEstado(r.tipoHabitacion(), "Libre");
        long ocupadas = reservaRepo.countOcupadasPorTipo(
                r.tipoHabitacion(), r.fechaEntrada(), r.fechaSalida());
        return ocupadas < libres;
    }

    private ReservaEntity registrarReserva(ReservaRequest r) {
        double precioNoche = tarifas.getOrDefault(r.tipoHabitacion(), 90.0);
        long noches = ChronoUnit.DAYS.between(r.fechaEntrada(), r.fechaSalida());
        if (noches <= 0) noches = 1;

        String codigo = generarCodigoUnico();
        ReservaEntity entity = new ReservaEntity(
                codigo, r.tipoHabitacion(), r.nombre().trim(), r.dni().trim(),
                r.correo() == null ? null : r.correo().trim(), r.telefono(),
                r.fechaEntrada(), r.fechaSalida(), (int) noches,
                precioNoche * noches, "Pendiente"
        );
        entity.setMetodoPago(r.metodoPago());
        entity.setHuespedes(r.huespedes() != null ? r.huespedes() : 1);
        return reservaRepo.save(entity);
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = "TMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (reservaRepo.existsById(codigo));
        return codigo;
    }

    @GetMapping("/reservas")
    public List<Reserva> listarReservas() {
        return reservaRepo.findAll().stream()
                .map(ReservaEntity::toRecord)
                .toList();
    }

    @GetMapping("/reservas/{codigo}")
    public ResponseEntity<Reserva> obtenerReserva(@PathVariable String codigo) {
        return reservaRepo.findById(codigo)
                .map(ReservaEntity::toRecord)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/reservas/{codigo}/estado")
    public ResponseEntity<Reserva> actualizarEstado(@PathVariable String codigo,
                                                    @RequestBody Map<String, String> body) {
        return reservaRepo.findById(codigo).map(actual -> {
            String nuevoEstado = body.getOrDefault("estado", actual.getEstado());
            actual.setEstado(nuevoEstado);
            reservaRepo.save(actual);
            return ResponseEntity.ok(actual.toRecord());
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/reservas/{codigo}/habitacion")
    public ResponseEntity<?> asignarHabitacion(@PathVariable String codigo,
                                               @RequestBody Map<String, String> body) {
        ReservaEntity actual = reservaRepo.findById(codigo).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        String numero = body.getOrDefault("numeroHabitacion", "");
        if (numero.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Indique el número de habitación."));
        }
        actual.setNumeroHabitacion(numero.trim().toUpperCase());
        reservaRepo.save(actual);
        log.info("Habitación {} asignada a la reserva {}", numero.trim().toUpperCase(), codigo);
        return ResponseEntity.ok(actual.toRecord());
    }

    @DeleteMapping("/reservas/{codigo}")
    public ResponseEntity<Void> eliminarReserva(@PathVariable String codigo) {
        if (!reservaRepo.existsById(codigo)) {
            return ResponseEntity.notFound().build();
        }
        reservaRepo.deleteById(codigo);
        return ResponseEntity.noContent().build();
    }
}
