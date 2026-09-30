package com.hoteltampur.backoffice.controller;

import com.hoteltampur.reservas.model.Habitacion;
import com.hoteltampur.reservas.model.HabitacionEntity;
import com.hoteltampur.reservas.model.ReservaEntity;
import com.hoteltampur.reservas.repository.HabitacionRepository;
import com.hoteltampur.reservas.repository.ReservaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowedHeaders = "*")
public class BackofficeController {

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;

    public BackofficeController(HabitacionRepository habitacionRepository, ReservaRepository reservaRepository) {
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
    }

    public record LimpiezaItem(String id, String tipo, int piso, int numero, String estadoOcupacion,
                               String estadoLimpieza, int prioridad, String horaInicio, String horaFin, String estadoFlujo) {}

    // ─── MAPA INTERACTIVO (OCUPACIÓN) ────────────────────────────────

    @GetMapping("/ocupacion")
    public List<Habitacion> estadoHotel() {
        List<Habitacion> habitaciones = habitacionRepository.findAll().stream()
                .map(HabitacionEntity::toRecord)
                .collect(Collectors.toList());
        return enrichConLimpieza(habitaciones);
    }

    @PatchMapping("/ocupacion/{id}/estado")
    public Habitacion actualizarEstado(@PathVariable String id,
                                       @RequestBody Map<String, String> body) {
        HabitacionEntity entity = habitacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Habitación no encontrada: " + id));
        String nuevo = body.getOrDefault("estado", entity.getEstado());
        entity.setEstado(nuevo);
        habitacionRepository.save(entity);
        return entity.toRecord();
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
        List<HabitacionEntity> habs = habitacionRepository.findAll();
        java.util.Set<Integer> seleccionadas = numerosLimpiasHoy(habs);
        LocalDate hoy = LocalDate.now();
        String claveFecha = hoy.format(FORMATO_FECHA);
        java.util.Set<Integer> marcadas = limpiadasPorFecha.getOrDefault(claveFecha, java.util.Set.of());
        
        return habs.stream()
            .filter(h -> seleccionadas.contains(h.getNumero()))
            .map(h -> {
                boolean limpiada = marcadas.contains(h.getNumero());
                int prioridad = calcularPrioridad(h.getNumero(), hoy);
                String clave = claveFecha + "_" + h.getNumero();
                String inicio = horaInicioPorFecha.getOrDefault(clave, "");
                String fin = horaFinPorFecha.getOrDefault(clave, "");
                String flujo = limpiada ? "completada" : estadoFlujoPorFecha.getOrDefault(clave, "pendiente");
                return new LimpiezaItem(h.getId(), h.getTipo(), h.getPiso(), h.getNumero(), h.getEstado(),
                                       limpiada ? "limpiada" : "pendiente", prioridad, inicio, fin, flujo);
            })
            .sorted(Comparator.comparingInt(LimpiezaItem::prioridad).thenComparingInt(LimpiezaItem::numero))
            .toList();
    }

    @PatchMapping("/limpieza/{numero}/estado")
    public ResponseEntity<?> actualizarLimpieza(@PathVariable int numero,
                                                 @RequestBody Map<String, Object> body) {
        HabitacionEntity entity = habitacionRepository.findAll().stream()
                .filter(h -> h.getNumero() == numero).findFirst().orElse(null);
        if (entity == null) {
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

        int prioridad = calcularPrioridad(numero, hoy);
        String flujo = estadoFlujoPorFecha.getOrDefault(clave, "pendiente");
        String inicio = horaInicioPorFecha.getOrDefault(clave, "");
        String fin = horaFinPorFecha.getOrDefault(clave, "");
        return ResponseEntity.ok(new LimpiezaItem(entity.getId(), entity.getTipo(), entity.getPiso(), entity.getNumero(), entity.getEstado(),
                                                  limpiada ? "limpiada" : "pendiente", prioridad, inicio, fin, flujo));
    }

    private java.util.Set<Integer> numerosLimpiasHoy(List<HabitacionEntity> habitaciones) {
        Random azar = new Random(LocalDate.now().toEpochDay());
        java.util.Set<Integer> seleccionadas = new HashSet<>();
        for (HabitacionEntity h : habitaciones) {
            if (azar.nextDouble() < PROBABILIDAD_LIMPIEZA) {
                seleccionadas.add(h.getNumero());
            }
        }
        List<HabitacionEntity> ordenadas = new ArrayList<>(habitaciones);
        Collections.shuffle(ordenadas, azar);
        for (HabitacionEntity h : ordenadas) {
            if (seleccionadas.size() >= 8) break;
            seleccionadas.add(h.getNumero());
        }
        return seleccionadas;
    }

    private int calcularPrioridad(int numero, LocalDate hoy) {
        if (numero % 2 == 0) return 1;
        return 3;
    }

    private List<Habitacion> enrichConLimpieza(List<Habitacion> lista) {
        List<HabitacionEntity> habs = habitacionRepository.findAll();
        java.util.Set<Integer> seleccionadas = numerosLimpiasHoy(habs);
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

    // ─── REPORTES Y BASE DE DATOS REAL ──────────────────────────────

    @GetMapping("/reportes/ingresos")
    public Map<String, Object> ingresosDiarios() {
        List<ReservaEntity> todas = reservaRepository.findAll();
        double totalIngresos = todas.stream()
                .filter(r -> "Confirmada".equals(r.getEstado()) || "Pagada".equals(r.getEstado()))
                .mapToDouble(ReservaEntity::getTotal)
                .sum();
                
        long confirmadas = todas.stream().filter(r -> "Confirmada".equals(r.getEstado())).count();
        long pendientes = todas.stream().filter(r -> "Pendiente".equals(r.getEstado())).count();

        return Map.of(
            "fecha", LocalDate.now().toString(),
            "ingresosTotales", totalIngresos,
            "reservasConfirmadas", confirmadas,
            "reservasPendientes", pendientes,
            "metodoPrincipal", "transferencia"
        );
    }

    @GetMapping("/bitacora/reservas")
    public List<Map<String, Object>> bitacoraReservas() {
        return reservaRepository.findAll().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("codigo", r.getCodigo());
            m.put("huesped", r.getNombre());
            m.put("habitacion", r.getTipoHabitacion());
            m.put("fechas", r.getFechaEntrada() + " → " + r.getFechaSalida());
            m.put("total", r.getTotal());
            m.put("estado", r.getEstado());
            return m;
        }).collect(Collectors.toList());
    }
}
