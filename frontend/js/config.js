// Configuración de las URLs de los microservicios.
// Desarrollo local: deja los valores por defecto (localhost).
// Producción (Vercel): edita aquí las URLs de tu backend desplegado.
window.HOTEL_CONFIG = window.HOTEL_CONFIG || {
  reservas: 'https://reservas-service-5nkp.onrender.com',
  pagos: 'https://pagos-service-dxob.onrender.com',
  backoffice: 'https://backoffice-service-bhdd.onrender.com'
};
