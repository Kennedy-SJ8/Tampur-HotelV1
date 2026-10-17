package com.hoteltampur.backoffice.controller;

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

    public record HabitacionDTO(String id, String tipo, int piso, int numero, String estado, String limpieza) {}

    public record LimpiezaItem(String id, String tipo, int piso, int numero, String estadoOcupacion,
                               String estadoLimpieza, int prioridad, String horaInicio, String horaFin, String estadoFlujo) {}

    private static final Map<String, Integer> MAPA_NUMEROS = Map.of(
        "S1", 101, "S2", 102,
        "M1", 201, "M2", 202,
        "Q1", 203, "Q2", 204,
        "K1", 301, "K2", 302
    );

    private int getNumero(HabitacionEntity h) {
        if (h == null || h.getId() == null) return 0;
        String id = h.getId().trim().toUpperCase();
        if (MAPA_NUMEROS.containsKey(id)) {
            return MAPA_NUMEROS.get(id);
        }
        try {
            return Integer.parseInt(id);
        } catch (Exception e) {
            return Math.abs(id.hashCode() % 900) + 100;
        }
    }

    private int getPiso(HabitacionEntity h) {
        int num = getNumero(h);
        if (num >= 100) return num / 100;
        if (h != null && h.getTipo() != null) {
            return switch (h.getTipo()) {
                case "Simple" -> 1;
                case "Matrimonial", "Queen" -> 2;
                case "King" -> 3;
                default -> 1;
            };
        }
        return 1;
    }

    private boolean tieneReservaActivaHoy(String idHabitacion, LocalDate hoy) {
        return reservaRepository.findAll().stream()
                .filter(r -> idHabitacion.equalsIgnoreCase(r.getNumeroHabitacion()))
                .filter(r -> "Confirmada".equalsIgnoreCase(r.getEstado()))
                .filter(r -> r.getFechaEntrada() != null && r.getFechaSalida() != null)
                .anyMatch(r -> !hoy.isBefore(r.getFechaEntrada()) && hoy.isBefore(r.getFechaSalida()));
    }

    // ─── MAPA INTERACTIVO (OCUPACIÓN) ────────────────────────────────

    @GetMapping("/ocupacion")
    public List<HabitacionDTO> estadoHotel() {
        List<HabitacionEntity> habitaciones = habitacionRepository.findAll();
        return enrichConLimpieza(habitaciones);
    }

    @PatchMapping("/ocupacion/{id}/estado")
    public HabitacionDTO actualizarEstado(@PathVariable String id,
                                       @RequestBody Map<String, String> body) {
        HabitacionEntity entity = habitacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Habitación no encontrada: " + id));
        String nuevo = body.getOrDefault("estado", entity.getEstado());
        entity.setEstado(nuevo);
        habitacionRepository.save(entity);

        int num = getNumero(entity);
        String claveFecha = LocalDate.now().format(FORMATO_FECHA);
        boolean limpiada = limpiadasPorFecha.getOrDefault(claveFecha, java.util.Set.of()).contains(num);
        return new HabitacionDTO(entity.getId(), entity.getTipo(), getPiso(entity), num, entity.getEstado(), limpiada ? "limpiada" : "");
    }

    // ─── LIMPIEZA DEL DÍA ──────────────────────────────────────────
    private static final double PROBABILIDAD_LIMPIEZA = 0.50;
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
            .filter(h -> seleccionadas.contains(getNumero(h)))
            .map(h -> {
                int num = getNumero(h);
                boolean limpiada = marcadas.contains(num);
                int prioridad = calcularPrioridad(h, num, hoy);
                String clave = claveFecha + "_" + num;
                String inicio = horaInicioPorFecha.getOrDefault(clave, "");
                String fin = horaFinPorFecha.getOrDefault(clave, "");
                String flujo = limpiada ? "completada" : estadoFlujoPorFecha.getOrDefault(clave, "pendiente");
                return new LimpiezaItem(h.getId(), h.getTipo(), getPiso(h), num, h.getEstado(),
                                       limpiada ? "limpiada" : "pendiente", prioridad, inicio, fin, flujo);
            })
            .sorted(Comparator.comparingInt(LimpiezaItem::prioridad).thenComparingInt(LimpiezaItem::numero))
            .toList();
    }

    @PatchMapping("/limpieza/{numero}/estado")
    public ResponseEntity<?> actualizarLimpieza(@PathVariable int numero,
                                                 @RequestBody Map<String, Object> body) {
        HabitacionEntity entity = habitacionRepository.findAll().stream()
                .filter(h -> getNumero(h) == numero).findFirst().orElse(null);
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

        if ("en_curso".equals(nuevoFlujo)) {
            if (!horaInicioPorFecha.containsKey(clave)) {
                horaInicioPorFecha.put(clave, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            entity.setEstado("Limpieza");
            habitacionRepository.save(entity);
        }
        if ("completada".equals(nuevoFlujo) || limpiada) {
            horaFinPorFecha.put(clave, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            nuevoFlujo = "completada";
            if ("Limpieza".equals(entity.getEstado())) {
                entity.setEstado(tieneReservaActivaHoy(entity.getId(), hoy) ? "Ocupada" : "Libre");
                habitacionRepository.save(entity);
            }
        }
        if (!nuevoFlujo.isEmpty()) {
            estadoFlujoPorFecha.put(clave, nuevoFlujo);
        }

        int prioridad = calcularPrioridad(entity, numero, hoy);
        String flujo = estadoFlujoPorFecha.getOrDefault(clave, "pendiente");
        String inicio = horaInicioPorFecha.getOrDefault(clave, "");
        String fin = horaFinPorFecha.getOrDefault(clave, "");
        return ResponseEntity.ok(new LimpiezaItem(entity.getId(), entity.getTipo(), getPiso(entity), getNumero(entity), entity.getEstado(),
                                                  limpiada ? "limpiada" : "pendiente", prioridad, inicio, fin, flujo));
    }

    private java.util.Set<Integer> numerosLimpiasHoy(List<HabitacionEntity> habitaciones) {
        LocalDate hoy = LocalDate.now();
        Random azar = new Random(hoy.toEpochDay());
        java.util.Set<Integer> seleccionadas = new HashSet<>();
        for (HabitacionEntity h : habitaciones) {
            int num = getNumero(h);
            if ("Limpieza".equalsIgnoreCase(h.getEstado()) || tieneMovimientoHoy(h.getId(), hoy) || azar.nextDouble() < PROBABILIDAD_LIMPIEZA) {
                seleccionadas.add(num);
            }
        }
        List<HabitacionEntity> ordenadas = new ArrayList<>(habitaciones);
        Collections.shuffle(ordenadas, azar);
        for (HabitacionEntity h : ordenadas) {
            if (seleccionadas.size() >= Math.min(6, habitaciones.size())) break;
            seleccionadas.add(getNumero(h));
        }
        return seleccionadas;
    }

    private boolean tieneMovimientoHoy(String idHabitacion, LocalDate hoy) {
        return reservaRepository.findAll().stream()
                .filter(r -> idHabitacion.equalsIgnoreCase(r.getNumeroHabitacion()))
                .filter(r -> !"Cancelada".equalsIgnoreCase(r.getEstado()))
                .anyMatch(r -> hoy.equals(r.getFechaSalida()) || hoy.equals(r.getFechaEntrada()));
    }

    private int calcularPrioridad(HabitacionEntity h, int numero, LocalDate hoy) {
        if (h != null && tieneMovimientoHoy(h.getId(), hoy)) {
            return 1; // Urgente: tiene check-out o check-in hoy
        }
        if (numero % 2 == 0) return 1;
        return 3;
    }

    private List<HabitacionDTO> enrichConLimpieza(List<HabitacionEntity> lista) {
        java.util.Set<Integer> seleccionadas = numerosLimpiasHoy(lista);
        LocalDate hoy = LocalDate.now();
        String claveFecha = hoy.format(FORMATO_FECHA);
        java.util.Set<Integer> marcadas = limpiadasPorFecha.getOrDefault(claveFecha, java.util.Set.of());
        return lista.stream()
            .map(h -> {
                int num = getNumero(h);
                String limpieza = "";
                if (seleccionadas.contains(num)) {
                    limpieza = marcadas.contains(num) ? "limpiada" : "pendiente";
                }
                String estadoEfectivo = h.getEstado();
                if ("Libre".equalsIgnoreCase(estadoEfectivo) && tieneReservaActivaHoy(h.getId(), hoy)) {
                    estadoEfectivo = "Ocupada";
                }
                return new HabitacionDTO(h.getId(), h.getTipo(), getPiso(h), num, estadoEfectivo, limpieza);
            })
            .sorted(Comparator.comparingInt(HabitacionDTO::piso).thenComparingInt(HabitacionDTO::numero))
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
