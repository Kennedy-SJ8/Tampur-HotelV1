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
        if (habitacionRepo.count() > 0) {
            log.info("Habitaciones ya existentes, se omite el seed.");
            return;
        }
        List<HabitacionEntity> iniciales = List.of(
                new HabitacionEntity("S1", "Simple", 60.0, "Libre"),
                new HabitacionEntity("S2", "Simple", 60.0, "Libre"),
                new HabitacionEntity("M1", "Matrimonial", 80.0, "Libre"),
                new HabitacionEntity("M2", "Matrimonial", 80.0, "Libre"),
                new HabitacionEntity("Q1", "Queen", 120.0, "Libre"),
                new HabitacionEntity("Q2", "Queen", 120.0, "Libre"),
                new HabitacionEntity("K1", "King", 140.0, "Libre"),
                new HabitacionEntity("K2", "King", 140.0, "Libre")
        );
        habitacionRepo.saveAll(iniciales);
        log.info("Habitaciones iniciales insertadas: {}", iniciales.size());
    }
}
