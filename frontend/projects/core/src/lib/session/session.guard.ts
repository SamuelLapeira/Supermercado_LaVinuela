import { inject } from '@angular/core';
import type { CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { SessionService } from './session.service';

/**
 * Protege una ruta: si no hay sesión, lleva a la pantalla de inicio de sesión y, al entrar, vuelve a la que se pidió.
 * La dirección de vuelta viaja como parámetro `volver`.
 */
export const authGuard: CanActivateFn = async (_route, state) => {
  const session = inject(SessionService);
  // Se pide ahora: después de un `await` ya no se puede usar inject().
  const router = inject(Router);
  if (session.status() === 'unknown') {
    await session.restore();
  }
  if (session.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/entrar'], { queryParams: { volver: state.url } });
};
