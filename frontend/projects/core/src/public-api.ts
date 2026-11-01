/*
 * API pública de la librería core: lo que necesitan todas las aplicaciones y no es visual ni del negocio.
 * Hoy: la sesión de la persona (inicio de sesión, renovación, protección de rutas).
 */
export * from './lib/session/session-hint';
export * from './lib/session/session.guard';
export * from './lib/session/session.interceptor';
export * from './lib/session/session.providers';
export * from './lib/session/session.service';
