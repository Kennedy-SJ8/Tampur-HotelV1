// Configuración de las URLs de los microservicios.
// Desarrollo local: deja los valores por defecto (localhost).
// Producción (Vercel): edita aquí las URLs de tu backend desplegado.
window.HOTEL_CONFIG = window.HOTEL_CONFIG || {
  reservas: 'http://localhost:8081',
  pagos: 'http://localhost:8082',
  backoffice: 'http://localhost:8083'
};
