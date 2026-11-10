package com.hoteltampur.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class ReservaEntity implements Persistable<String> {

    @Id
    private String codigo;
    private String tipoHabitacion;
    private String nombre;
    private String dni;
    private String correo;
    private String telefono;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int noches;
    private double total;
    private String estado;
    private String numeroHabitacion;
    private String metodoPago;
    private int huespedes;
    private LocalDateTime creadoEn;

    @Transient
    private boolean nuevo = true;

    public ReservaEntity() {}

    public ReservaEntity(String codigo, String tipoHabitacion, String nombre, String dni,
                         String correo, String telefono, LocalDate fechaEntrada,
                         LocalDate fechaSalida, int noches, double total, String estado) {
        this.codigo = codigo;
        this.tipoHabitacion = tipoHabitacion;
        this.nombre = nombre;
        this.dni = dni;
        this.correo = correo;
        this.telefono = telefono;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.noches = noches;
        this.total = total;
        this.estado = estado;
        this.creadoEn = LocalDateTime.now();
    }

    @Override
    public String getId() { return codigo; }

    @Override
    public boolean isNew() { return nuevo; }

    @PostLoad
    @PostPersist
    public void marcarComoPersistida() { this.nuevo = false; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTipoHabitacion() { return tipoHabitacion; }
    public void setTipoHabitacion(String tipoHabitacion) { this.tipoHabitacion = tipoHabitacion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public int getNoches() { return noches; }
    public void setNoches(int noches) { this.noches = noches; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNumeroHabitacion() { return numeroHabitacion; }
    public void setNumeroHabitacion(String numeroHabitacion) { this.numeroHabitacion = numeroHabitacion; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public int getHuespedes() { return huespedes; }
    public void setHuespedes(int huespedes) { this.huespedes = huespedes; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public Reserva toRecord() {
        LocalDate hoy = LocalDate.now();
        boolean esCheckinHoy = fechaEntrada != null && fechaEntrada.equals(hoy);
        boolean esCheckoutHoy = fechaSalida != null && fechaSalida.equals(hoy);
        return new Reserva(codigo, tipoHabitacion, nombre, dni, correo, telefono,
                fechaEntrada, fechaSalida, noches, total, estado, numeroHabitacion,
                metodoPago, huespedes, esCheckinHoy, esCheckoutHoy);
    }
}
