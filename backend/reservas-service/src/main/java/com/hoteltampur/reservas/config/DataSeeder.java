package com.hoteltampur.reservas.config;

import com.hoteltampur.reservas.model.HabitacionEntity;
import com.hoteltampur.reservas.repository.HabitacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final HabitacionRepository habitacionRepo;
    private final JdbcTemplate jdbc;

    public DataSeeder(HabitacionRepository habitacionRepo, JdbcTemplate jdbc) {
        this.habitacionRepo = habitacionRepo;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Asegurar que las columnas nuevas existan en las tablas
        asegurarColumnas();

        List<HabitacionEntity> existentes = habitacionRepo.findAll();
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

    private void asegurarColumnas() {
        String[][] columnasReservas = {
            {"numero_habitacion", "VARCHAR(10)"},
            {"metodo_pago", "VARCHAR(20)"},
            {"huespedes", "INTEGER DEFAULT 1"},
            {"creado_en", "TIMESTAMP"}
        };
        for (String[] col : columnasReservas) {
            try {
                jdbc.execute("ALTER TABLE reservas ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]);
            } catch (Exception e) {
                log.warn("No se pudo agregar columna reservas.{}: {}", col[0], e.getMessage());
            }
        }

        String[][] columnasHabitaciones = {
            {"precio_noche", "DOUBLE PRECISION"},
            {"estado", "VARCHAR(20)"}
        };
        for (String[] col : columnasHabitaciones) {
            try {
                jdbc.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]);
            } catch (Exception e) {
                log.warn("No se pudo agregar columna habitaciones.{}: {}", col[0], e.getMessage());
            }
        }

        // Crear tabla disponibilidad si no existe
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS disponibilidad (
                    id VARCHAR(50) PRIMARY KEY,
                    numero_habitacion VARCHAR(10),
                    fecha DATE,
                    estado VARCHAR(20),
                    codigo_reserva VARCHAR(30)
                )
            """);
        } catch (Exception e) {
            log.warn("No se pudo crear tabla disponibilidad: {}", e.getMessage());
        }

        log.info("Esquema de BD verificado.");
    }
}
