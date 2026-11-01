import { makeEnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import type { EnvironmentProviders } from '@angular/core';
import { SessionService } from './session.service';

/** Al arrancar la aplicación, recupera la sesión si la persona la tenía abierta (antes de pintar la primera pantalla). */
export function provideSession(): EnvironmentProviders {
  return makeEnvironmentProviders([provideAppInitializer(() => inject(SessionService).restore())]);
}
