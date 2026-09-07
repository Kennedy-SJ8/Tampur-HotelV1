package com.hoteltampur.reservas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "habitaciones")
public class HabitacionEntity {

    @Id
    private String id;
    private String tipo;
    private double precioNoche;
    private String estado;

    public HabitacionEntity() {}

    public HabitacionEntity(String id, String tipo, double precioNoche, String estado) {
        this.id = id;
        this.tipo = tipo;
        this.precioNoche = precioNoche;
        this.estado = estado;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(double precioNoche) { this.precioNoche = precioNoche; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Habitacion toRecord() {
        return new Habitacion(id, tipo, precioNoche, estado);
    }
}
