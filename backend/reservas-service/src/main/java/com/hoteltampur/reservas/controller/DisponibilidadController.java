package com.hoteltampur.reservas.controller;

import com.hoteltampur.reservas.model.DisponibilidadEntity;
import com.hoteltampur.reservas.repository.DisponibilidadRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/disponibilidad")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowedHeaders = "*")
public class DisponibilidadController {

    private final DisponibilidadRepository repo;

    public DisponibilidadController(DisponibilidadRepository repo) {
        this.repo = repo;
    }

    /** Obtener disponibilidad de un mes completo. */
    @GetMapping
    public List<Map<String, Object>> obtener(@RequestParam(required = false) Integer anio,
                                              @RequestParam(required = false) Integer mes) {
        YearMonth ym = (anio != null && mes != null)
                ? YearMonth.of(anio, mes)
                : YearMonth.now();
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        return repo.findByFechaBetween(inicio, fin).stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getId());
                    m.put("habitacion", d.getNumeroHabitacion());
                    m.put("fecha", d.getFecha().toString());
                    m.put("estado", d.getEstado());
                    m.put("reserva", d.getCodigoReserva());
                    return m;
                })
                .collect(Collectors.toList());
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
            // Si ya existe, eliminar (desbloquear)
            repo.deleteById(id);
            return ResponseEntity.ok(Map.of("accion", "liberada", "habitacion", habitacion, "fecha", fecha));
        } else {
            // Crear bloqueo manual
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
