package com.hoteltampur.backoffice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowedHeaders = "*")
public class BackofficeController {

    // ─── MODELO UNIFICADO DE HABITACIONES ───────────────────────────

    /** Estados de limpieza: "" = no está en la lista de hoy, "pendiente" = necesita limpieza, "limpiada" = ya limpiada. */
    public record Habitacion(String id, String tipo, int piso, int numero, String estado, String limpieza) {}

    /** Item de limpieza con prioridad, timestamps y flujo. */
    public record LimpiezaItem(String id, String tipo, int piso, int numero, String estado, String limpieza,
                               int prioridad, String horaInicio, String horaFin, String estadoFlujo) {}

    private final List<Habitacion> habitaciones = new ArrayList<>(List.of(
        // Piso 1: Simple (101–107)
        new Habitacion("101", "Simple", 1, 101, "Libre", ""),
        new Habitacion("102", "Simple", 1, 102, "Libre", ""),
        new Habitacion("103", "Simple", 1, 103, "Libre", ""),
        new Habitacion("104", "Simple", 1, 104, "Libre", ""),
        new Habitacion("105", "Simple", 1, 105, "Libre", ""),
        new Habitacion("106", "Simple", 1, 106, "Libre", ""),
        new Habitacion("107", "Simple", 1, 107, "Libre", ""),
        // Piso 2: Doble (201–206)
        new Habitacion("201", "Doble", 2, 201, "Libre", ""),
        new Habitacion("202", "Doble", 2, 202, "Libre", ""),
        new Habitacion("203", "Doble", 2, 203, "Libre", ""),
        new Habitacion("204", "Doble", 2, 204, "Libre", ""),
        new Habitacion("205", "Doble", 2, 205, "Libre", ""),
        new Habitacion("206", "Doble", 2, 206, "Libre", ""),
        // Piso 2: Matrimonial (207–212)
        new Habitacion("207", "Matrimonial", 2, 207, "Libre", ""),
        new Habitacion("208", "Matrimonial", 2, 208, "Libre", ""),
        new Habitacion("209", "Matrimonial", 2, 209, "Libre", ""),
        new Habitacion("210", "Matrimonial", 2, 210, "Libre", ""),
        new Habitacion("211", "Matrimonial", 2, 211, "Libre", ""),
        new Habitacion("212", "Matrimonial", 2, 212, "Libre", ""),
        // Piso 3: Matrimonial (301–305)
        new Habitacion("301", "Matrimonial", 3, 301, "Libre", ""),
        new Habitacion("302", "Matrimonial", 3, 302, "Libre", ""),
        new Habitacion("303", "Matrimonial", 3, 303, "Libre", ""),
        new Habitacion("304", "Matrimonial", 3, 304, "Libre", ""),
        new Habitacion("305", "Matrimonial", 3, 305, "Libre", "")
    ));

    // ─── OCUPACION (PLANO DE HABITACIONES) ──────────────────────────

    @GetMapping("/ocupacion")
    public List<Habitacion> ocupacion() {
        return enrichConLimpieza(habitaciones);
    }

    @PatchMapping("/ocupacion/{id}/estado")
    public Habitacion actualizarEstado(@PathVariable String id,
                                       @RequestBody Map<String, String> body) {
        for (int i = 0; i < habitaciones.size(); i++) {
            Habitacion h = habitaciones.get(i);
            if (h.id().equals(id)) {
                String nuevo = body.getOrDefault("estado", h.estado());
                Habitacion actualizado = new Habitacion(h.id(), h.tipo(), h.piso(), h.numero(), nuevo, h.limpieza());
                habitaciones.set(i, actualizado);
                return actualizado;
            }
        }
        throw new IllegalArgumentException("Habitación no encontrada: " + id);
    }

    // ─── LIMPIEZA DEL DÍA ──────────────────────────────────────────

    private static final double PROBABILIDAD_LIMPIEZA = 0.35;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private final Map<String, java.util.Set<Integer>> limpiadasPorFecha = new ConcurrentHashMap<>();
    private final Map<String, String> horaInicioPorFecha = new ConcurrentHashMap<>();
    private final Map<String, String> horaFinPorFecha = new ConcurrentHashMap<>();
    private final Map<String, String> estadoFlujoPorFecha = new ConcurrentHashMap<>();

    @GetMapping("/limpieza")
    public List<LimpiezaItem> limpiezaDeHoy() {
        java.util.Set<Integer> seleccionadas = numerosLimpiasHoy();
        LocalDate hoy = LocalDate.now();
        String claveFecha = hoy.format(FORMATO_FECHA);
        java.util.Set<Integer> marcadas = limpiadasPorFecha.getOrDefault(claveFecha, java.util.Set.of());
        return habitaciones.stream()
            .filter(h -> seleccionadas.contains(h.numero()))
            .map(h -> {
                boolean limpiada = marcadas.contains(h.numero());
                int prioridad = calcularPrioridad(h.numero(), hoy);
                String clave = claveFecha + "_" + h.numero();
                String inicio = horaInicioPorFecha.getOrDefault(clave, "");
                String fin = horaFinPorFecha.getOrDefault(clave, "");
                String flujo = limpiada ? "completada" : estadoFlujoPorFecha.getOrDefault(clave, "pendiente");
                return new LimpiezaItem(h.id(), h.tipo(), h.piso(), h.numero(), h.estado(),
                                       limpiada ? "limpiada" : "pendiente", prioridad, inicio, fin, flujo);
            })
            .sorted(Comparator.comparingInt(LimpiezaItem::prioridad).thenComparingInt(LimpiezaItem::numero))
            .toList();
    }

    @PatchMapping("/limpieza/{numero}/estado")
    public ResponseEntity<?> actualizarLimpieza(@PathVariable int numero,
                                                 @RequestBody Map<String, Object> body) {
        boolean existe = habitaciones.stream().anyMatch(h -> h.numero() == numero);
        if (!existe) {
            return ResponseEntity.badRequest().body(Map.of("error", "Habitación no encontrada: " + numero));
        }
        boolean limpiada = Boolean.TRUE.equals(body.get("limpiada"));
        String nuevoFlujo = body.getOrDefault("estadoFlujo", "").toString();
        LocalDate hoy = LocalDate.now();
        String fecha = hoy.format(FORMATO_FECHA);
        String clave = fecha + "_" + numero;

        java.util.Set<Integer> marcadas = limpiadasPorFecha.computeIfAbsent(fecha, f -> ConcurrentHashMap.newKeySet());
        if (limpiada) { marcadas.add(numero); } else { marcadas.remove(numero); }

        if ("en_curso".equals(nuevoFlujo) && !horaInicioPorFecha.containsKey(clave)) {
            horaInicioPorFecha.put(clave, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if ("completada".equals(nuevoFlujo) || limpiada) {
            horaFinPorFecha.put(clave, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            nuevoFlujo = "completada";
        }
        if (!nuevoFlujo.isEmpty()) {
            estadoFlujoPorFecha.put(clave, nuevoFlujo);
        }

        Habitacion h = habitaciones.stream().filter(x -> x.numero() == numero).findFirst().orElseThrow();
        int prioridad = calcularPrioridad(numero, hoy);
        String flujo = estadoFlujoPorFecha.getOrDefault(clave, "pendiente");
        String inicio = horaInicioPorFecha.getOrDefault(clave, "");
        String fin = horaFinPorFecha.getOrDefault(clave, "");
        return ResponseEntity.ok(new LimpiezaItem(h.id(), h.tipo(), h.piso(), h.numero(), h.estado(),
                                                  limpiada ? "limpiada" : "pendiente", prioridad, inicio, fin, flujo));
    }

    // ─── HELPERS ────────────────────────────────────────────────────

    /** Números de habitación que entran en la lista de limpieza hoy (determinístico por día). */
    private java.util.Set<Integer> numerosLimpiasHoy() {
        Random azar = new Random(LocalDate.now().toEpochDay());
        java.util.Set<Integer> seleccionadas = new HashSet<>();
        for (Habitacion h : habitaciones) {
            if (azar.nextDouble() < PROBABILIDAD_LIMPIEZA) {
                seleccionadas.add(h.numero());
            }
        }
        return seleccionadas;
    }

    /** Calcula prioridad: 1=checkout hoy (urgente), 2=checkin hoy, 3=normal. */
    private int calcularPrioridad(int numero, LocalDate hoy) {
        // Simulación: prioridad basada en número de habitación (par=urgente, impar=normal)
        // En producción se conectaría a reservas-service para verificar checkin/checkout reales
        if (numero % 2 == 0) return 1; // checkout hoy (urgente)
        return 3; // normal
    }

    /** Agrega el estado de limpieza de hoy a cada habitación (para el plano). */
    private List<Habitacion> enrichConLimpieza(List<Habitacion> lista) {
        java.util.Set<Integer> seleccionadas = numerosLimpiasHoy();
        String claveFecha = LocalDate.now().format(FORMATO_FECHA);
        java.util.Set<Integer> marcadas = limpiadasPorFecha.getOrDefault(claveFecha, java.util.Set.of());
        return lista.stream()
            .map(h -> {
                String limpieza = "";
                if (seleccionadas.contains(h.numero())) {
                    limpieza = marcadas.contains(h.numero()) ? "limpiada" : "pendiente";
                }
                return new Habitacion(h.id(), h.tipo(), h.piso(), h.numero(), h.estado(), limpieza);
            })
            .toList();
    }

    // ─── REPORTES ───────────────────────────────────────────────────

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
}
