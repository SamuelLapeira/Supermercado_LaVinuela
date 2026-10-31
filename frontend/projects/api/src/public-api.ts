/*
 * API pública de la librería api: el cliente de la API del servidor.
 * Todo lo de ./lib/generated se genera con "npm run api:generate" a partir de contracts/openapi.json: no se edita a mano.
 * Las ayudas para leer los errores estándar del servidor están en ./lib/problem.
 */
export * from './lib/generated';
export * from './lib/problem';
