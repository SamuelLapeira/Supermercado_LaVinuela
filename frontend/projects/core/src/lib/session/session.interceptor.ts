import { HttpErrorResponse } from '@angular/common/http';
import type { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { SessionService } from './session.service';

const API_PREFIX = '/api/';
const AUTH_PREFIX = '/api/v1/auth/';

/**
 * Prepara todas las llamadas a la API:
 *
 * - Les añade la cabecera `X-LV-Client: web`, que exigen las operaciones de sesión (una web ajena no puede añadirla).
 * - Les añade el token de acceso (`Authorization: Bearer ...`) si hay sesión, salvo a las operaciones de sesión.
 * - Si una llamada con token falla con 401 (el token ha caducado), renueva la sesión **una sola vez** y repite la llamada
 *   con el token nuevo. Si no se puede renovar, el error llega a quien llamó.
 */
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(API_PREFIX)) {
    return next(request);
  }
  const session = inject(SessionService);
  const base = request.clone({ setHeaders: { 'X-LV-Client': 'web' } });
  if (request.url.startsWith(AUTH_PREFIX)) {
    return next(base);
  }

  const token = session.accessToken();
  if (token === null) {
    return next(base);
  }
  return next(withToken(base, token)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return from(session.refresh()).pipe(switchMap((renewed) => retry(renewed, base, next, session, error)));
    }),
  );
};

function retry(renewed: boolean, base: HttpRequest<unknown>, next: HttpHandlerFn, session: SessionService, original: HttpErrorResponse) {
  const token = session.accessToken();
  if (!renewed || token === null) {
    return throwError(() => original);
  }
  return next(withToken(base, token));
}

function withToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
