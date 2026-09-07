package com.hoteltampur.backoffice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BackofficeController {

    public record HabitacionEstado(String id, String tipo, String estado) {
    }

    private final List<HabitacionEstado> ocupacion = new ArrayList<>(List.of(
            new HabitacionEstado("S1", "Simple", "Libre"),
            new HabitacionEstado("S2", "Simple", "Limpieza"),
            new HabitacionEstado("D1", "Doble", "Libre"),
            new HabitacionEstado("D2", "Doble", "Ocupada"),
            new HabitacionEstado("M1", "Matrimonial", "Libre"),
            new HabitacionEstado("M2", "Matrimonial", "Mantenimiento")
    ));

    @GetMapping("/ocupacion")
    public List<HabitacionEstado> ocupacion() {
        return ocupacion;
    }

    @PatchMapping("/ocupacion/{id}/estado")
    public HabitacionEstado actualizarEstado(@PathVariable String id,
                                             @RequestBody Map<String, String> body) {
        for (int i = 0; i < ocupacion.size(); i++) {
            HabitacionEstado h = ocupacion.get(i);
            if (h.id().equals(id)) {
                String nuevo = body.getOrDefault("estado", h.estado());
                HabitacionEstado actualizado = new HabitacionEstado(h.id(), h.tipo(), nuevo);
                ocupacion.set(i, actualizado);
                return actualizado;
            }
        }
        throw new IllegalArgumentException("Habitación no encontrada: " + id);
    }

    @GetMapping("/reportes/ingresos")
    public Map<String, Object> ingresosDiarios() {
        return Map.of(
                "fecha", "2026-09-01",
                "ingresosTotales", 720.0,
                "reservasConfirmadas", 4,
                "reservasPendientes", 2,
                "metodoPrincipal", "efectivo"
        );
    }

    @GetMapping("/reservas")
    public List<Map<String, Object>> bitacoraReservas() {
        return List.of(
                Map.of("codigo", "TMP-AB12CD", "huesped", "Carlos Pérez", "habitacion", "Doble",
                        "fechas", "2026-09-10 → 2026-09-12", "total", 280.0, "estado", "Confirmada"),
                Map.of("codigo", "TMP-EF34GH", "huesped", "María López", "habitacion", "Matrimonial",
                        "fechas", "2026-09-15 → 2026-09-17", "total", 360.0, "estado", "Pendiente")
        );
    }

    // ===================== LIMPIEZA DEL DÍA =====================

    /** Estructura del hotel: piso -> [primera habitación, última habitación]. */
    private static final Map<Integer, int[]> PISOS = Map.of(
            1, new int[]{101, 107},
            2, new int[]{201, 212},
            3, new int[]{301, 305}
    );

    private static final double PROBABILIDAD_LIMPIEZA = 0.35;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    public record HabitacionLimpieza(int numero, int piso, boolean limpiada) {
    }

    /**
     * Habitaciones marcadas como "limpiada" por fecha (yyyy-MM-dd -> número de habitación).
     * Al cambiar el día, el mapa de ese día simplemente no existe todavía y arranca vacío:
     * no hace falta ningún job de limpieza de memoria.
     */
    private final Map<String, java.util.Set<Integer>> limpiadasPorFecha = new ConcurrentHashMap<>();

    /**
     * Devuelve las habitaciones que necesitan limpieza hoy.
     * <p>
     * La selección es aleatoria pero <b>determinística</b>: se usa la fecha del día como
     * semilla, así que la lista es idéntica durante todo el día (no cambia si se recarga
     * la página), pero distinta al día siguiente. Es una simulación mientras no exista un
     * sistema real de housekeeping conectado a check-ins/check-outs.
     */
    @GetMapping("/limpieza")
    public List<HabitacionLimpieza> limpiezaDeHoy() {
        return generarPendientesDelDia(LocalDate.now());
    }

    /** Marca (o desmarca) una habitación como limpiada para el día de hoy. */
    @PatchMapping("/limpieza/{numero}/estado")
    public ResponseEntity<?> actualizarLimpieza(@PathVariable int numero,
                                                 @RequestBody Map<String, Object> body) {
        List<HabitacionLimpieza> deHoy = generarPendientesDelDia(LocalDate.now());
        boolean existe = deHoy.stream().anyMatch(h -> h.numero() == numero);
        if (!existe) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La habitación " + numero + " no está en la lista de limpieza de hoy."
            ));
        }

        boolean limpiada = Boolean.TRUE.equals(body.get("limpiada"));
        String fecha = LocalDate.now().format(FORMATO_FECHA);
        java.util.Set<Integer> marcadas = limpiadasPorFecha.computeIfAbsent(fecha, f -> ConcurrentHashMap.newKeySet());
        if (limpiada) {
            marcadas.add(numero);
        } else {
            marcadas.remove(numero);
        }

        int piso = pisoDeHabitacion(numero);
        return ResponseEntity.ok(new HabitacionLimpieza(numero, piso, limpiada));
    }

    private List<HabitacionLimpieza> generarPendientesDelDia(LocalDate fecha) {
        String claveFecha = fecha.format(FORMATO_FECHA);
        java.util.Set<Integer> marcadas = limpiadasPorFecha.getOrDefault(claveFecha, java.util.Set.of());

        // Semilla estable por día: mismo resultado toda la jornada, distinto cada día.
        Random azar = new Random(fecha.toEpochDay());

        List<HabitacionLimpieza> pendientes = new ArrayList<>();
        for (Map.Entry<Integer, int[]> piso : PISOS.entrySet()) {
            int[] rango = piso.getValue();
            for (int numero = rango[0]; numero <= rango[1]; numero++) {
                if (azar.nextDouble() < PROBABILIDAD_LIMPIEZA) {
                    pendientes.add(new HabitacionLimpieza(numero, piso.getKey(), marcadas.contains(numero)));
                }
            }
        }
        pendientes.sort((a, b) -> Integer.compare(a.numero(), b.numero()));
        return pendientes;
    }

    private int pisoDeHabitacion(int numero) {
        return numero / 100;
    }
}
