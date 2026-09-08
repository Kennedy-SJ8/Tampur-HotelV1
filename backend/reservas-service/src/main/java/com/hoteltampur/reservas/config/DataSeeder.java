package com.hoteltampur.reservas.config;

import com.hoteltampur.reservas.model.HabitacionEntity;
import com.hoteltampur.reservas.repository.HabitacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inserta las habitaciones iniciales del hotel si la tabla está vacía.
 * Se ejecuta una sola vez al arrancar el servicio.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final HabitacionRepository habitacionRepo;

    public DataSeeder(HabitacionRepository habitacionRepo) {
        this.habitacionRepo = habitacionRepo;
    }

    @Override
    public void run(String... args) {
        List<HabitacionEntity> existentes = habitacionRepo.findAll();
        // Actualizar precios de tipos existentes
        existentes.forEach(h -> {
            switch (h.getTipo()) {
                case "Simple" -> h.setPrecioNoche(60.0);
                case "Matrimonial" -> h.setPrecioNoche(80.0);
                case "Queen" -> h.setPrecioNoche(120.0);
                case "King" -> h.setPrecioNoche(140.0);
                default -> {}
            }
        });
        habitacionRepo.saveAll(existentes);

        // Agregar tipos que no existen aún
        java.util.Set<String> tiposExistentes = new java.util.HashSet<>();
        existentes.forEach(h -> tiposExistentes.add(h.getTipo()));
        List<HabitacionEntity> nuevos = new java.util.ArrayList<>();
        if (!tiposExistentes.contains("Queen")) {
            nuevos.add(new HabitacionEntity("Q1", "Queen", 120.0, "Libre"));
            nuevos.add(new HabitacionEntity("Q2", "Queen", 120.0, "Libre"));
        }
        if (!tiposExistentes.contains("King")) {
            nuevos.add(new HabitacionEntity("K1", "King", 140.0, "Libre"));
            nuevos.add(new HabitacionEntity("K2", "King", 140.0, "Libre"));
        }
        if (!nuevos.isEmpty()) {
            habitacionRepo.saveAll(nuevos);
            log.info("Nuevos tipos insertados: {}", nuevos.size());
        }
        log.info("DataSeeder completado. Total habitaciones: {}", habitacionRepo.count());
    }
}
