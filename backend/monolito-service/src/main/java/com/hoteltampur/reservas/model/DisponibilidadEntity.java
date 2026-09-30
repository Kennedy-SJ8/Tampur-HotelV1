package com.hoteltampur.reservas.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.LocalDate;

@Entity
@Table(name = "disponibilidad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"numero_habitacion", "fecha"})
})
public class DisponibilidadEntity implements Persistable<String> {

    @Id
    private String id;
    private String numeroHabitacion;
    private LocalDate fecha;
    private String estado; // "reservada", "mantenimiento", "bloqueada"
    private String codigoReserva; ///null si es bloqueo manual

    @Transient
    private boolean nuevo = true;

    public DisponibilidadEntity() {}

    public DisponibilidadEntity(String id, String numeroHabitacion, LocalDate fecha, String estado, String codigoReserva) {
        this.id = id;
        this.numeroHabitacion = numeroHabitacion;
        this.fecha = fecha;
        this.estado = estado;
        this.codigoReserva = codigoReserva;
        this.nuevo = true;
    }

    @Override
    public String getId() { return id; }

    @Override
    public boolean isNew() { return nuevo; }

    @PostLoad
    @PostPersist
    public void marcarComoPersistida() { this.nuevo = false; }

    public String getNumeroHabitacion() { return numeroHabitacion; }
    public void setNumeroHabitacion(String numeroHabitacion) { this.numeroHabitacion = numeroHabitacion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }
}
