package com.hoteltampur.reservas.repository;

import com.hoteltampur.reservas.model.HabitacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HabitacionRepository extends JpaRepository<HabitacionEntity, String> {
    List<HabitacionEntity> findByEstado(String estado);
    long countByTipo(String tipo);
    long countByTipoAndEstado(String tipo, String estado);
}
