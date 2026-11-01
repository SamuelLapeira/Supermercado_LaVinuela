import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideApiConfiguration } from 'api';
import { provideSession, sessionInterceptor } from 'core';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    // Las llamadas a la API van a la misma dirección que la página (ver docs/api-contract.md).
    provideApiConfiguration(''),
    // Todas las llamadas a /api/ llevan la cabecera de cliente y, si hay sesión, el token de acceso.
    provideHttpClient(withFetch(), withInterceptors([sessionInterceptor])),
    // Al arrancar, recupera la sesión si la persona la tenía abierta.
    provideSession(),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
  ],
};
