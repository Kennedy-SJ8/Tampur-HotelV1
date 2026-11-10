package com.hoteltampur.reservas.repository;

import com.hoteltampur.reservas.model.ReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<ReservaEntity, String> {

    @Query("SELECT COUNT(r) FROM ReservaEntity r WHERE r.tipoHabitacion = :tipo " +
           "AND r.estado <> 'Cancelada' " +
           "AND r.fechaSalida > :fechaEntrada AND r.fechaEntrada < :fechaSalida")
    long countOcupadasPorTipo(@Param("tipo") String tipo,
                              @Param("fechaEntrada") LocalDate fechaEntrada,
                              @Param("fechaSalida") LocalDate fechaSalida);

    List<ReservaEntity> findByEstadoNot(String estado);

    @Query("SELECT r FROM ReservaEntity r WHERE r.dni = :dni " +
           "AND r.tipoHabitacion = :tipo " +
           "AND r.fechaEntrada = :fechaEntrada " +
           "AND r.fechaSalida = :fechaSalida " +
           "AND r.estado <> 'Cancelada'")
    List<ReservaEntity> buscarDuplicada(@Param("dni") String dni,
                                         @Param("tipo") String tipo,
                                         @Param("fechaEntrada") LocalDate fechaEntrada,
                                         @Param("fechaSalida") LocalDate fechaSalida);
}
