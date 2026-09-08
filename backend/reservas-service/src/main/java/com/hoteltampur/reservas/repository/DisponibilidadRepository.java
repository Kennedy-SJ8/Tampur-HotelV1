package com.hoteltampur.reservas.repository;

import com.hoteltampur.reservas.model.DisponibilidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DisponibilidadRepository extends JpaRepository<DisponibilidadEntity, String> {
    List<DisponibilidadEntity> findByNumeroHabitacionAndFechaBetween(String numeroHabitacion, LocalDate inicio, LocalDate fin);
    List<DisponibilidadEntity> findByFechaBetween(LocalDate inicio, LocalDate fin);
    void deleteByCodigoReserva(String codigoReserva);
}
