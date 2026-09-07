package com.hoteltampur.reservas.model;

import java.time.LocalDate;

/**
 * Datos que envía el frontend para crear una reserva.
 * <p>
 * {@code idempotencyKey} es opcional: el cliente genera un identificador único
 * por cada intento de reserva (por ejemplo, al abrir el modal) y lo reenvía
 * si el usuario presiona "Confirmar" varias veces. El backend lo usa para
 * devolver la reserva ya creada en vez de duplicarla.
 */
public record ReservaRequest(
        String tipoHabitacion,
        String nombre,
        String dni,
        String correo,
        String telefono,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        String idempotencyKey
) {
}
