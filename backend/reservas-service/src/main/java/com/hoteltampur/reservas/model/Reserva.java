package com.hoteltampur.reservas.model;

import java.time.LocalDate;

public record Reserva(
        String codigo,
        String tipoHabitacion,
        String nombre,
        String dni,
        String correo,
        String telefono,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        double total,
        String estado,
        String numeroHabitacion,
        String metodoPago,
        boolean checkinHoy,
        boolean checkoutHoy
) {
}
