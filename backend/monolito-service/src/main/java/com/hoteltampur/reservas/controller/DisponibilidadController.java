package com.hoteltampur.reservas.controller;

import com.hoteltampur.reservas.model.DisponibilidadEntity;
import com.hoteltampur.reservas.model.ReservaEntity;
import com.hoteltampur.reservas.repository.DisponibilidadRepository;
import com.hoteltampur.reservas.repository.ReservaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendario")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowedHeaders = "*")
public class DisponibilidadController {

    private final DisponibilidadRepository repo;
    private final ReservaRepository reservaRepo;

    public DisponibilidadController(DisponibilidadRepository repo, ReservaRepository reservaRepo) {
        this.repo = repo;
        this.reservaRepo = reservaRepo;
    }

    /** Obtener disponibilidad del mes: combina tabla disponibilidad + reservas activas. */
    @GetMapping
    public List<Map<String, Object>> obtener(@RequestParam(required = false) Integer anio,
                                              @RequestParam(required = false) Integer mes) {
        YearMonth ym = (anio != null && mes != null)
                ? YearMonth.of(anio, mes)
                : YearMonth.now();
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        // Mapa de datos de la tabla disponibilidad
        Map<String, Map<String, Object>> mapa = new LinkedHashMap<>();
        repo.findByFechaBetween(inicio, fin).forEach(d -> {
            String clave = d.getNumeroHabitacion() + "_" + d.getFecha().toString();
            Map<String, Object> m = new HashMap<>();
            m.put("habitacion", d.getNumeroHabitacion());
            m.put("fecha", d.getFecha().toString());
            m.put("estado", d.getEstado());
            m.put("reserva", d.getCodigoReserva());
            mapa.put(clave, m);
        });

        // Agregar reservas activas (Pendiente o Confirmada) que NO estén ya en disponibilidad
        try {
            reservaRepo.findAll().stream()
                .filter(r -> r.getNumeroHabitacion() != null && !r.getNumeroHabitacion().isBlank())
                .filter(r -> !"Cancelada".equals(r.getEstado()))
                .filter(r -> r.getFechaEntrada() != null && r.getFechaSalida() != null)
                .filter(r -> !r.getFechaEntrada().isAfter(fin) && !r.getFechaSalida().isBefore(inicio))
                .forEach(r -> {
                    LocalDate entrada = r.getFechaEntrada().isBefore(inicio) ? inicio : r.getFechaEntrada();
                    LocalDate salida = r.getFechaSalida().isAfter(fin.plusDays(1)) ? fin.plusDays(1) : r.getFechaSalida();
                    String numero = r.getNumeroHabitacion();
                    LocalDate f = entrada;
                    while (f.isBefore(salida)) {
                        String clave = numero + "_" + f.toString();
                        if (!mapa.containsKey(clave)) {
                            Map<String, Object> m = new HashMap<>();
                            m.put("habitacion", numero);
                            m.put("fecha", f.toString());
                            m.put("estado", "reservada");
                            m.put("reserva", r.getCodigo());
                            mapa.put(clave, m);
                        }
                        f = f.plusDays(1);
                    }
                });
        } catch (Exception e) {
            // Si falla la query de reservas, solo mostrar datos de disponibilidad
        }

        return new ArrayList<>(mapa.values());
    }

    /** Marcar fechas como reservada (automático al crear reserva). */
    @PostMapping("/reservar")
    public ResponseEntity<?> reservarFechas(@RequestBody Map<String, Object> body) {
        String habitacion = (String) body.get("habitacion");
        String codigoReserva = (String) body.get("codigoReserva");
        List<String> fechas = (List<String>) body.get("fechas");

        if (habitacion == null || fechas == null || fechas.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "habitacion y fechas son requeridos"));
        }

        List<DisponibilidadEntity> entidades = new ArrayList<>();
        for (String f : fechas) {
            LocalDate fecha = LocalDate.parse(f);
            String id = habitacion + "_" + f;
            DisponibilidadEntity existing = repo.findById(id).orElse(null);
            if (existing == null) {
                entidades.add(new DisponibilidadEntity(id, habitacion, fecha, "reservada", codigoReserva));
            }
        }
        repo.saveAll(entidades);
        return ResponseEntity.ok(Map.of("bloqueadas", entidades.size()));
    }

    /** Marcar/desmarcar un día manualmente (mantenimiento/bloqueo admin). */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggle(@RequestBody Map<String, String> body) {
        String habitacion = body.get("habitacion");
        String fecha = body.get("fecha");
        String estado = body.getOrDefault("estado", "mantenimiento");

        if (habitacion == null || fecha == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "habitacion y fecha son requeridos"));
        }

        String id = habitacion + "_" + fecha;
        Optional<DisponibilidadEntity> existing = repo.findById(id);

        if (existing.isPresent()) {
            repo.deleteById(id);
            return ResponseEntity.ok(Map.of("accion", "liberada", "habitacion", habitacion, "fecha", fecha));
        } else {
            LocalDate f = LocalDate.parse(fecha);
            repo.save(new DisponibilidadEntity(id, habitacion, f, estado, null));
            return ResponseEntity.ok(Map.of("accion", "bloqueada", "habitacion", habitacion, "fecha", fecha, "estado", estado));
        }
    }

    /** Liberar fechas de una reserva (al cancelar/eliminar). */
    @DeleteMapping("/reserva/{codigo}")
    public ResponseEntity<?> liberarReserva(@PathVariable String codigo) {
        repo.deleteByCodigoReserva(codigo);
        return ResponseEntity.ok(Map.of("liberada", codigo));
    }
}
