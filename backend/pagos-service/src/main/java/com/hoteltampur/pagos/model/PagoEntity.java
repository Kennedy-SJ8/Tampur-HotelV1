package com.hoteltampur.pagos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class PagoEntity {

    @Id
    private String id;
    private String codigoReserva;
    private String metodo;
    private String estado;
    private String voucher;
    private LocalDateTime creadoEn;

    public PagoEntity() {}

    public PagoEntity(String id, String codigoReserva, String metodo, String estado, String voucher) {
        this.id = id;
        this.codigoReserva = codigoReserva;
        this.metodo = metodo;
        this.estado = estado;
        this.voucher = voucher;
        this.creadoEn = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getVoucher() { return voucher; }
    public void setVoucher(String voucher) { this.voucher = voucher; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
