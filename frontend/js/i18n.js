// ─── SISTEMA i18n ────────────────────────────────────────────
let idiomaActual = localStorage.getItem('tampur_idioma') || 'ES';

const traducciones = {
  ES: {
    // Nav
    nav_inicio: 'Inicio', nav_habitaciones: 'Habitaciones', nav_amenidades: 'Amenidades',
    nav_ubicacion: 'Ubicación', nav_contacto: 'Contacto', nav_reservar: 'Reservar',
    // Hero
    hero_etiqueta: '✦ Reservas directas · sin comisiones',
    hero_titulo: 'Descanso en las alturas de <em>San Mateo</em>',
    hero_lead: 'A 3,200 msnm, rodeado de montañas. Reserve directamente y pague la mejor tarifa, sin intermediarios ni cargos ocultos.',
    lbl_llegada: 'Llegada', lbl_salida: 'Salida', lbl_huespedes: 'Huéspedes',
    huespedes_opciones: ['1 huésped','2 huéspedes'],
    lbl_moneda: 'Mostrar precios en',
    btn_buscar: 'Buscar',
    // Franja
    franja_comision: 'Comisión de terceros', franja_checkin: 'Check-in 1:00 p.m.',
    franja_checkout: 'Check-out 12:00 p.m.',     franja_tipos: 'Simple · Matrimonial · Queen · King',
    franja_altura: 'msnm de altura', franja_pago: 'Efectivo en recepción',
    // Habitaciones
    sec_alojamiento: 'Alojamiento', sec_habitaciones_titulo: 'Nuestras habitaciones',
    sec_habitaciones_lead: 'Espacios cómodos y cálidos pensados para el clima de altura. Elija el tipo ideal para su estadía.',
    precio_desde: 'Desde', precio_noche: '/noche',
    habitacion_simple: 'Habitación Simple', habitacion_matrimonial: 'Habitación Matrimonial',
    habitacion_queen: 'Habitación Queen', habitacion_king: 'Habitación King',
    simple_1: '1 cama individual', simple_2: 'Baño privado con agua caliente',
    simple_3: 'Wi-Fi de alta velocidad', simple_4: 'Vistas a la montaña',
    mat_1: '1 cama matrimonial', mat_2: 'Baño privado con agua caliente',
    mat_3: 'Wi-Fi + TV por cable', mat_4: 'Vistas a la montaña',
    queen_1: '1 cama queen size', queen_2: 'Baño privado con agua caliente',
    queen_3: 'Wi-Fi + TV por cable', queen_4: 'Balcón con vista',
    king_1: '1 cama king size', king_2: 'Baño privado con ducha de lluvia',
    king_3: 'Wi-Fi + Smart TV', king_4: 'Balcón panorámico',
    btn_reservar: 'Reservar',
    // Amenidades
    sec_amenidades: 'Comodidades', sec_amenidades_titulo: 'Amenidades del hotel',
    sec_amenidades_lead: 'Todo lo que necesita para una estadía cómoda y sin preocupaciones.',
    amen_wifi_titulo: 'Wi-Fi gratis', amen_wifi_desc: 'Conexión de alta velocidad en todas las áreas.',
    amen_desayuno_titulo: 'Desayuno (pago aparte)', amen_desayuno_desc: 'Opciones calientes para empezar el día.',
    amen_estacionamiento_titulo: 'Estacionamiento gratis', amen_estacionamiento_desc: 'Espacio seguro para su vehículo.',
    amen_piscina_titulo: 'Piscina', amen_piscina_desc: 'Zona de descanso y recreación.',
    amen_pet_titulo: 'Pet friendly', amen_pet_desc: 'Aceptamos mascotas con políticas claras.',
    amen_recepcion_titulo: 'Recepción 24h', amen_recepcion_desc: 'Personal disponible para ayudarle.',
    // Ubicación
    sec_ubicacion_kicker: 'Encuéntrenos', sec_ubicacion_titulo: 'Ubicación y contacto',
    sec_ubicacion_lead: 'Magnolias 197, San Mateo, Huarochirí, Lima — a 3,200 msnm.',
    dato_direccion: 'Dirección', dato_telefono: 'Teléfono / WhatsApp',
    dato_horarios: 'Horarios', dato_pago: 'Medios de pago',
    form_consulta: 'Envíe su consulta', form_nombre: 'Nombre', form_correo: 'Correo',
    form_mensaje: 'Mensaje', form_placeholder: 'Quisiera información sobre...', form_enviar: 'Enviar consulta',
    // Footer
    footer_explorar: 'Explorar', footer_contacto: 'Contacto',
    footer_derechos: 'Todos los derechos reservados', footer_admin: 'Acceso administrador',
    footer_desc: 'Plataforma de reservas directas. Reserve sin comisiones de terceros y disfrute de la mejor tarifa garantizada.',
    // Modal reserva
    reserva_subtitle: 'Complete sus datos para confirmar.',
    // Monitorear
    mon_sin_reservas: 'No hay reservas aún. Las reservas que se hagan desde la web aparecerán aquí automáticamente.',
    // Contacto
    sec_contacto: 'Contacto', sec_contacto_titulo: 'Reserve directo',
    sec_contacto_lead: 'Escríbanos por WhatsApp o llámanos. Atención personalizada sin comisiones.',
    btn_whatsapp: 'WhatsApp', btn_llamar: 'Llamar',
    // Footer
    footer_copy: '© 2026 Hotel Tampur · San Mateo, Huancavelica',
    footer_copy2: 'Reservas directas sin intermediarios',
    // Formulario reserva
    reserva_titulo: 'Reservar habitación',     reserva_nombre: 'Nombre completo',
    reserva_dni: 'DNI o Carnet', reserva_email: 'Correo electrónico', reserva_tel: 'Celular / WhatsApp',
    'reserva MetodoPago': 'Método de pago', 'reserva_efectivo': 'Efectivo en recepción',
    reserva_llegada: 'Fecha de llegada', reserva_salida: 'Fecha de salida',
    reserva_habitacion: 'Tipo de habitación', reserva_huespedes: 'Número de huéspedes',
    reserva_sencillo: 'Sencillo', reserva_doble_tip: 'Doble', reserva_matrimonial_tip: 'Matrimonial',
    reserva_noches: 'Noches', reserva_total: 'Total a pagar', reserva_pagar: 'Pagar con Yape/Plin',
    reserva_confirmar: 'Confirmar reserva',
    reserva_info1: 'La reserva se confirma al recibir el comprobante de pago.',
    reserva_info2: 'Si tiene alguna duda, escríbanos al WhatsApp.',
    // Pago
    pago_titulo: 'Pago con Yape o Plin', pago_monto: 'Monto a pagar',
    pago_escanear: 'Escanee el código QR desde su aplicación',
    pago_numero: 'Al número', pago_yapeplin: 'Yape / Plin',
    pago_subir: 'Adjuntar comprobante', pago_nombre_arch: 'No seleccionado',
    pago_como_pagar: 'Cómo pagar:', pago_paso1: 'Abre la app de',
    pago_paso2: 'Escanea este código QR', pago_paso3: 'Envía el monto exacto',
    pago_paso4: 'Adjunta la captura de pantalla',
    pago_enviar: 'Enviar comprobante', pago_procesando: 'Procesando...',
    pago_exito: '¡Reserva registrada!', pago_codigo: 'Su código de reserva',
    pago_info1: 'Guarde su código. Recibirá la confirmación por correo.',
    pago_info2: 'Si no recibe el correo, revise su bandeja de spam.',
    btn_cerrar: 'Cerrar',
    // Admin
    admin_titulo: 'Panel de administración', admin_subtitulo: 'Ingrese sus credenciales para monitorear las reservas.',
    admin_usuario: 'Usuario', admin_contrasena: 'Contraseña',
    admin_ingresar: 'Ingresar', admin_cargando: 'Entrando...', admin_err_credenciales: 'Credenciales incorrectas',
    admin_nav_reservas: 'Monitorear reservas', admin_nav_plano: 'Plano de habitaciones',
    admin_nav_limpieza: 'Limpieza del día',
    // Monitorear
    mon_total: 'Total', mon_confirmadas: 'Confirmadas', mon_pendientes: 'Pendientes',
    mon_canceladas: 'Canceladas', mon_ingresos: 'Ingresos confirmados',
    mon_checkin_hoy: 'Check-in hoy', mon_checkout_hoy: 'Check-out hoy',
    mon_buscar: 'Buscar por nombre o DNI...', mon_filtro_estado: 'Todos',
    mon_filtro_pend: 'Pendientes', mon_filtro_conf: 'Confirmadas', mon_filtro_canc: 'Canceladas',
    mon_th_codigo: 'Código', mon_th_huesped: 'Huésped', mon_th_tipo: 'Tipo',
    mon_th_hab: 'N° Hab.', mon_th_fechas: 'Fechas', mon_th_noches: 'Noches',
    mon_th_total: 'Total', mon_th_pago: 'Pago', mon_th_estado: 'Estado', mon_th_acciones: 'Acciones',
    mon_confirmar: 'Confirmar', mon_cancelar: 'Cancelar', mon_eliminar: 'Eliminar',
    mon_badge_yape: 'Yape', mon_badge_plin: 'Plin', mon_badge_efectivo: 'Efectivo',
    mon_sin_reservas: 'No hay reservas aún. Las reservas que se hagan desde la web aparecerán aquí automáticamente.',
    // Plano
    plano_libre: 'Libre', plano_ocupada: 'Ocupada', plano_limpieza: 'Limpieza',
    plano_mantenimiento: 'Mantenimiento', plano_limpiada: '✓ Limpiada', plano_pendiente: '🧹 Pendiente',
    plano_ocupacion: 'Ocupación', plano_libres: 'Libres', plano_en_limpieza: 'En limpieza',
    plano_mant: 'Mantenimiento', plano_limp_pend: 'Limpieza pend.',
    plano_piso: 'Piso',
    // Limpieza
    limp_titulo: 'Limpieza del día', limp_para_limpiar: 'Para limpiar hoy',
    limp_ya_limpiadas: 'Ya limpiadas', limp_pendientes: 'Pendientes', limp_progreso: 'Progreso total',
    limp_urgente: 'Urgente', limp_importante: 'Importante', limp_normal: 'Normal',
    limp_imprimir: 'Imprimir lista para la encargada',
    limp_ocupaciones: 'Ocupaciones próximas · reservas confirmadas',
    limp_habitacion: 'Habitación', limp_checkin: 'Check-in:', limp_checkout: 'Check-out:',
    limp_horaini: '1:00 p.m.', limp_horafin: '12:00 p.m.',
    limp_iniciar: 'Iniciar', limp_completar: 'Completar', limp_limpiada: 'Limpiada',
    limp_piso: 'Piso',
    limp_msj_vacio: 'No hay habitaciones pendientes de limpieza para hoy.',
    limp_msj_local: 'Backend no conectado: guardando en modo local.',
    limp_msj_online: 'Haga clic sobre un estado para cambiarlo.',
  },
  EN: {
    // Nav
    nav_inicio: 'Home', nav_habitaciones: 'Rooms', nav_amenidades: 'Amenities',
    nav_ubicacion: 'Location', nav_contacto: 'Contact', nav_reservar: 'Book Now',
    // Hero
    hero_etiqueta: '✦ Direct bookings · no commissions',
    hero_titulo: 'Rest in the heights of <em>San Mateo</em>',
    hero_lead: 'At 3,200 m.a.s.l., surrounded by mountains. Book directly and pay the best rate, no middlemen or hidden fees.',
    lbl_llegada: 'Check-in', lbl_salida: 'Check-out', lbl_huespedes: 'Guests',
    huespedes_opciones: ['1 guest','2 guests'],
    lbl_moneda: 'Show prices in',
    btn_buscar: 'Search',
    // Franja
    franja_comision: 'Third-party commission', franja_checkin: 'Check-in 1:00 p.m.',
    franja_checkout: 'Check-out 12:00 p.m.',     franja_tipos: 'Single · Matrimonial · Queen · King',
    franja_altura: 'm.a.s.l.', franja_pago: 'Cash at reception',
    // Habitaciones
    sec_alojamiento: 'Accommodation', sec_habitaciones_titulo: 'Our rooms',
    sec_habitaciones_lead: 'Comfortable, warm spaces designed for the highland climate. Choose the ideal type for your stay.',
    precio_desde: 'From', precio_noche: '/night',
    habitacion_simple: 'Single Room', habitacion_matrimonial: 'Matrimonial Room',
    habitacion_queen: 'Queen Room', habitacion_king: 'King Room',
    simple_1: '1 single bed', simple_2: 'Private bathroom with hot water',
    simple_3: 'High-speed Wi-Fi', simple_4: 'Mountain views',
    mat_1: '1 double bed', mat_2: 'Private bathroom with hot water',
    mat_3: 'Wi-Fi + Cable TV', mat_4: 'Mountain views',
    queen_1: '1 queen size bed', queen_2: 'Private bathroom with hot water',
    queen_3: 'Wi-Fi + Cable TV', queen_4: 'Balcony with view',
    king_1: '1 king size bed', king_2: 'Private bathroom with rain shower',
    king_3: 'Wi-Fi + Smart TV', king_4: 'Panoramic balcony',
    btn_reservar: 'Book',
    // Amenidades
    sec_amenidades: 'Comforts', sec_amenidades_titulo: 'Hotel amenities',
    sec_amenidades_lead: 'Everything you need for a comfortable, worry-free stay.',
    amen_wifi_titulo: 'Free Wi-Fi', amen_wifi_desc: 'High-speed connection in all areas.',
    amen_desayuno_titulo: 'Breakfast (extra charge)', amen_desayuno_desc: 'Hot options to start your day.',
    amen_estacionamiento_titulo: 'Free parking', amen_estacionamiento_desc: 'Safe space for your vehicle.',
    amen_piscina_titulo: 'Pool', amen_piscina_desc: 'Rest and recreation area.',
    amen_pet_titulo: 'Pet friendly', amen_pet_desc: 'We accept pets with clear policies.',
    amen_recepcion_titulo: '24h Reception', amen_recepcion_desc: 'Staff available to help you.',
    // Ubicación
    sec_ubicacion_kicker: 'Find us', sec_ubicacion_titulo: 'Location & contact',
    sec_ubicacion_lead: 'Magnolias 197, San Mateo, Huarochirí, Lima — 3,200 m.a.s.l.',
    dato_direccion: 'Address', dato_telefono: 'Phone / WhatsApp',
    dato_horarios: 'Hours', dato_pago: 'Payment methods',
    form_consulta: 'Send your inquiry', form_nombre: 'Name', form_correo: 'Email',
    form_mensaje: 'Message', form_placeholder: 'I would like information about...', form_enviar: 'Send inquiry',
    // Footer
    footer_explorar: 'Explore', footer_contacto: 'Contact',
    footer_derechos: 'All rights reserved', footer_admin: 'Admin access',
    footer_desc: 'Direct booking platform. Book without third-party commissions and enjoy the best guaranteed rate.',
    // Modal reserva
    reserva_subtitle: 'Complete your details to confirm.',
    // Monitorear
    mon_sin_reservas: 'No bookings yet. Reservations made from the website will appear here automatically.',
    // Contacto
    sec_contacto: 'Contact', sec_contacto_titulo: 'Book directly',
    sec_contacto_lead: 'Write to us on WhatsApp or call us. Personalized service with no commissions.',
    btn_whatsapp: 'WhatsApp', btn_llamar: 'Call',
    // Footer
    footer_copy: '© 2026 Hotel Tampur · San Mateo, Huancavelica',
    footer_copy2: 'Direct bookings without middlemen',
    // Formulario reserva
    reserva_titulo: 'Book a room',     reserva_nombre: 'Full name',
    reserva_dni: 'ID or DNI', reserva_email: 'Email', reserva_tel: 'Phone / WhatsApp',
    'reserva MetodoPago': 'Payment method', 'reserva_efectivo': 'Cash at reception',
    reserva_llegada: 'Check-in date', reserva_salida: 'Check-out date',
    reserva_habitacion: 'Room type', reserva_huespedes: 'Number of guests',
    reserva_sencillo: 'Single', reserva_doble_tip: 'Double', reserva_matrimonial_tip: 'Matrimonial',
    reserva_noches: 'Nights', reserva_total: 'Total to pay', reserva_pagar: 'Pay with Yape/Plin',
    reserva_confirmar: 'Confirm booking',
    reserva_info1: 'The booking is confirmed upon receiving the payment receipt.',
    reserva_info2: 'If you have any questions, write to us on WhatsApp.',
    // Pago
    pago_titulo: 'Pay with Yape or Plin', pago_monto: 'Amount to pay',
    pago_escanear: 'Scan the QR code from your app',
    pago_numero: 'To number', pago_yapeplin: 'Yape / Plin',
    pago_subir: 'Upload payment receipt', pago_nombre_arch: 'No file selected',
    pago_como_pagar: 'How to pay:', pago_paso1: 'Open the',
    pago_paso2: 'Scan this QR code', pago_paso3: 'Send the exact amount',
    pago_paso4: 'Attach the screenshot',
    pago_enviar: 'Submit receipt', pago_procesando: 'Processing...',
    pago_exito: 'Booking registered!', pago_codigo: 'Your booking code',
    pago_info1: 'Save your code. You will receive confirmation by email.',
    pago_info2: 'If you don\'t receive the email, check your spam folder.',
    btn_cerrar: 'Close',
    // Admin
    admin_titulo: 'Admin panel', admin_subtitulo: 'Enter your credentials to monitor bookings.',
    admin_usuario: 'Username', admin_contrasena: 'Password',
    admin_ingresar: 'Sign in', admin_cargando: 'Signing in...', admin_err_credenciales: 'Invalid credentials',
    admin_nav_reservas: 'Monitor bookings', admin_nav_plano: 'Room plan',
    admin_nav_limpieza: 'Daily cleaning',
    // Monitorear
    mon_total: 'Total', mon_confirmadas: 'Confirmed', mon_pendientes: 'Pending',
    mon_canceladas: 'Cancelled', mon_ingresos: 'Confirmed revenue',
    mon_checkin_hoy: 'Check-in today', mon_checkout_hoy: 'Check-out today',
    mon_buscar: 'Search by name or ID...', mon_filtro_estado: 'All',
    mon_filtro_pend: 'Pending', mon_filtro_conf: 'Confirmed', mon_filtro_canc: 'Cancelled',
    mon_th_codigo: 'Code', mon_th_huesped: 'Guest', mon_th_tipo: 'Type',
    mon_th_hab: 'Room #', mon_th_fechas: 'Dates', mon_th_noches: 'Nights',
    mon_th_total: 'Total', mon_th_pago: 'Payment', mon_th_estado: 'Status', mon_th_acciones: 'Actions',
    mon_confirmar: 'Confirm', mon_cancelar: 'Cancel', mon_eliminar: 'Delete',
    mon_badge_yape: 'Yape', mon_badge_plin: 'Plin', mon_badge_efectivo: 'Cash',
    mon_sin_reservas: 'No bookings yet. Reservations made from the website will appear here automatically.',
    // Plano
    plano_libre: 'Available', plano_ocupada: 'Occupied', plano_limpieza: 'Cleaning',
    plano_mantenimiento: 'Maintenance', plano_limpiada: '✓ Clean', plano_pendiente: '🧹 Pending',
    plano_ocupacion: 'Occupancy', plano_libres: 'Available', plano_en_limpieza: 'Cleaning',
    plano_mant: 'Maintenance', plano_limp_pend: 'Cleaning pending',
    plano_piso: 'Floor',
    // Limpieza
    limp_titulo: 'Daily cleaning', limp_para_limpiar: 'To clean today',
    limp_ya_limpiadas: 'Already cleaned', limp_pendientes: 'Pending', limp_progreso: 'Total progress',
    limp_urgente: 'Urgent', limp_importante: 'Important', limp_normal: 'Normal',
    limp_imprimir: 'Print list for housekeeper',
    limp_ocupaciones: 'Upcoming bookings · confirmed reservations',
    limp_habitacion: 'Room', limp_checkin: 'Check-in:', limp_checkout: 'Check-out:',
    limp_horaini: '1:00 p.m.', limp_horafin: '12:00 p.m.',
    limp_iniciar: 'Start', limp_completar: 'Complete', limp_limpiada: 'Cleaned',
    limp_piso: 'Floor',
    limp_msj_vacio: 'No rooms pending cleaning today.',
    limp_msj_local: 'Backend offline: saving in local mode.',
    limp_msj_online: 'Click on a status to change it.',
  }
};

function t(key) {
  return (traducciones[idiomaActual] && traducciones[idiomaActual][key]) || traducciones['ES'][key] || key;
}

function toggleIdioma() {
  idiomaActual = idiomaActual === 'ES' ? 'EN' : 'ES';
  localStorage.setItem('tampur_idioma', idiomaActual);
  document.getElementById('btnIdioma').textContent = idiomaActual === 'ES' ? 'EN' : 'ES';
  traducirPagina();
  // Re-render panel activo si admin está abierto
  if (typeof renderAdminPanel === 'function') renderAdminPanel();
}

function traducirPagina() {
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    const texto = t(key);
    if (el.tagName === 'INPUT' && el.type !== 'submit') {
      el.placeholder = texto;
    } else {
      el.innerHTML = texto;
    }
  });
  // Traducir opciones de selects
  document.querySelectorAll('[data-i18n-options]').forEach(el => {
    const key = el.getAttribute('data-i18n-options');
    const opciones = t(key);
    if (Array.isArray(opciones)) {
      const options = el.querySelectorAll('option');
      opciones.forEach((txt, i) => { if (options[i]) options[i].textContent = txt; });
    }
  });
}

// Inicializar idioma al cargar
document.addEventListener('DOMContentLoaded', function() {
  const btn = document.getElementById('btnIdioma');
  if (btn) btn.textContent = idiomaActual === 'ES' ? 'EN' : 'ES';
  traducirPagina();
});
