package com.hoteltampur.reservas.controller;

import com.hoteltampur.reservas.model.Habitacion;
import com.hoteltampur.reservas.model.Reserva;
import com.hoteltampur.reservas.model.ReservaRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Endpoints del motor de reservas del Hotel Támpur.
 * <p>
 * Incluye validación de datos de entrada y dos capas de protección contra
 * reservas duplicadas (por ejemplo, cuando el usuario presiona "Confirmar"
 * varias veces seguidas en el formulario web):
 * <ol>
 *   <li><b>Idempotencia:</b> el frontend genera una clave única por intento
 *       de reserva. Si esa clave ya fue procesada, se devuelve la reserva
 *       existente en vez de crear una nueva.</li>
 *   <li><b>Detección por contenido:</b> aunque no llegue la clave (por
 *       ejemplo, en un cliente distinto), se rechaza cualquier solicitud que
 *       coincida en DNI, habitación y fechas con una reserva activa reciente.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReservasController {

    private static final Logger log = LoggerFactory.getLogger(ReservasController.class);

    /** DNI peruano: 8 dígitos. Se acepta también un pasaporte alfanumérico de 6 a 12 caracteres. */
    private static final Pattern PATRON_DNI = Pattern.compile("^[A-Za-z0-9]{6,12}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Map<String, Double> tarifas = new HashMap<>(Map.of(
            "Simple", 90.0,
            "Doble", 140.0,
            "Matrimonial", 180.0
    ));

    private final List<Habitacion> habitaciones = new ArrayList<>(List.of(
            new Habitacion("S1", "Simple", 90.0, "Libre"),
            new Habitacion("S2", "Simple", 90.0, "Limpieza"),
            new Habitacion("D1", "Doble", 140.0, "Libre"),
            new Habitacion("D2", "Doble", 140.0, "Ocupada"),
            new Habitacion("M1", "Matrimonial", 180.0, "Libre"),
            new Habitacion("M2", "Matrimonial", 180.0, "Limpieza")
    ));

    /** Reservas registradas, indexadas por código. */
    private final Map<String, Reserva> reservas = new ConcurrentHashMap<>();

    /** Claves de idempotencia ya procesadas -> código de reserva generado. */
    private final Map<String, String> solicitudesProcesadas = new ConcurrentHashMap<>();

    private final CorreoService correoService;

    public ReservasController(CorreoService correoService) {
        this.correoService = correoService;
    }

    @GetMapping("/habitaciones")
    public List<Habitacion> habitaciones() {
        return habitaciones;
    }

    @GetMapping("/disponibilidad")
    public List<Habitacion> disponibilidad(@RequestParam LocalDate fechaEntrada,
                                            @RequestParam LocalDate fechaSalida) {
        Map<String, Long> ocupadasPorTipo = reservas.values().stream()
                .filter(r -> !"Cancelada".equalsIgnoreCase(r.estado()))
                .filter(r -> r.fechaSalida().isAfter(fechaEntrada) && r.fechaEntrada().isBefore(fechaSalida))
                .collect(Collectors.groupingBy(Reserva::tipoHabitacion, Collectors.counting()));
        Map<String, Long> totalesPorTipo = habitaciones.stream()
                .collect(Collectors.groupingBy(Habitacion::tipo, Collectors.counting()));
        return habitaciones.stream()
                .filter(h -> "Libre".equals(h.estado()))
                .filter(h -> ocupadasPorTipo.getOrDefault(h.tipo(), 0L) < totalesPorTipo.getOrDefault(h.tipo(), 0L))
                .toList();
    }

    /**
     * Crea una reserva nueva.
     * <p>
     * Responde:
     * <ul>
     *   <li>400 Bad Request si los datos no son válidos.</li>
     *   <li>200 OK con la reserva existente si la misma clave de idempotencia
     *       ya fue procesada (reintento / doble clic detectado por clave).</li>
     *   <li>409 Conflict si ya existe una reserva activa con el mismo DNI,
     *       habitación y fechas (reintento / doble clic detectado por datos).</li>
     *   <li>201 Created con la reserva nueva en el caso normal.</li>
     * </ul>
     */
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

        // 1) Protección por clave de idempotencia (mismo intento reenviado).
        if (r.idempotencyKey() != null && !r.idempotencyKey().isBlank()) {
            String codigoExistente = solicitudesProcesadas.get(r.idempotencyKey());
            if (codigoExistente != null) {
                Reserva existente = reservas.get(codigoExistente);
                if (existente != null) {
                    log.info("Solicitud repetida detectada por idempotencyKey, se devuelve {}", codigoExistente);
                    return ResponseEntity.ok(existente);
                }
            }
        }

        // 2) Protección por contenido (mismo huésped, habitación y fechas ya reservados).
        Reserva duplicada = buscarReservaDuplicada(r);
        if (duplicada != null) {
            log.info("Reserva duplicada detectada por contenido, se rechaza a favor de {}", duplicada.codigo());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Ya existe una reserva activa con estos datos.",
                    "reservaExistente", duplicada
            ));
        }

        Reserva reserva = registrarReserva(r);

        if (r.idempotencyKey() != null && !r.idempotencyKey().isBlank()) {
            solicitudesProcesadas.put(r.idempotencyKey(), reserva.codigo());
        }

        try {
            correoService.enviarConfirmacion(reserva);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de confirmación de {}: {}", reserva.codigo(), e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(reserva);
    }

    /** Valida los campos obligatorios y las reglas de negocio básicas de la solicitud. */
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

    /**
     * Busca una reserva activa (no cancelada) con el mismo DNI, tipo de
     * habitación y rango de fechas. Se usa como red de seguridad adicional
     * a la clave de idempotencia.
     */
    private Reserva buscarReservaDuplicada(ReservaRequest r) {
        return reservas.values().stream()
                .filter(res -> !"Cancelada".equalsIgnoreCase(res.estado()))
                .filter(res -> Objects.equals(res.dni(), r.dni()))
                .filter(res -> Objects.equals(res.tipoHabitacion(), r.tipoHabitacion()))
                .filter(res -> Objects.equals(res.fechaEntrada(), r.fechaEntrada()))
                .filter(res -> Objects.equals(res.fechaSalida(), r.fechaSalida()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Indica si hay al menos una habitación libre del tipo solicitado para el
     * rango de fechas. Las habitaciones "Ocupada", "Limpieza" o "Mantenimiento"
     * no se consideran disponibles.
     */
    private boolean hayDisponibilidad(ReservaRequest r) {
        long libres = habitaciones.stream()
                .filter(h -> "Libre".equals(h.estado()) && r.tipoHabitacion().equals(h.tipo()))
                .count();
        long ocupadas = reservas.values().stream()
                .filter(res -> !"Cancelada".equalsIgnoreCase(res.estado()))
                .filter(res -> r.tipoHabitacion().equals(res.tipoHabitacion()))
                .filter(res -> res.fechaSalida().isAfter(r.fechaEntrada())
                        && res.fechaEntrada().isBefore(r.fechaSalida()))
                .count();
        return ocupadas < libres;
    }

    /** Calcula el total, genera el código y guarda la reserva en memoria. */
    private Reserva registrarReserva(ReservaRequest r) {
        double precioNoche = tarifas.getOrDefault(r.tipoHabitacion(), 90.0);
        long noches = ChronoUnit.DAYS.between(r.fechaEntrada(), r.fechaSalida());
        if (noches <= 0) {
            noches = 1;
        }

        String codigo = generarCodigoUnico();
        Reserva reserva = new Reserva(
                codigo,
                r.tipoHabitacion(),
                r.nombre().trim(),
                r.dni().trim(),
                r.correo() == null ? null : r.correo().trim(),
                r.telefono(),
                r.fechaEntrada(),
                r.fechaSalida(),
                (int) noches,
                precioNoche * noches,
                "Pendiente"
        );
        reservas.put(codigo, reserva);
        return reserva;
    }

    /** Genera un código "TMP-XXXXXX" garantizando que no choque con uno existente. */
    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = "TMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (reservas.containsKey(codigo));
        return codigo;
    }

    @GetMapping("/reservas")
    public Collection<Reserva> listarReservas() {
        return reservas.values();
    }

    @GetMapping("/reservas/{codigo}")
    public ResponseEntity<Reserva> obtenerReserva(@PathVariable String codigo) {
        Reserva reserva = reservas.get(codigo);
        return reserva == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(reserva);
    }

    @PatchMapping("/reservas/{codigo}/estado")
    public ResponseEntity<Reserva> actualizarEstado(@PathVariable String codigo,
                                                    @RequestBody Map<String, String> body) {
        Reserva actual = reservas.get(codigo);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        String nuevoEstado = body.getOrDefault("estado", actual.estado());
        Reserva actualizada = new Reserva(
                actual.codigo(), actual.tipoHabitacion(), actual.nombre(), actual.dni(),
                actual.correo(), actual.telefono(), actual.fechaEntrada(), actual.fechaSalida(),
                actual.noches(), actual.total(), nuevoEstado
        );
        reservas.put(codigo, actualizada);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/reservas/{codigo}")
    public ResponseEntity<Void> eliminarReserva(@PathVariable String codigo) {
        if (reservas.remove(codigo) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
