package com.hoteltampur.pagos.repository;

import com.hoteltampur.pagos.model.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoRepository extends JpaRepository<PagoEntity, String> {
}
