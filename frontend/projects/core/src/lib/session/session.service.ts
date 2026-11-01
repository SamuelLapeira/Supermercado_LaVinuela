import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Api, login as loginOperation, logout as logoutOperation, refreshSession as refreshOperation } from 'api';
import type { SessionResponse, UserResponse } from 'api';
import { sessionHint } from './session-hint';

/** `unknown` solo dura hasta que la aplicación intenta recuperar la sesión al arrancar. */
export type SessionStatus = 'unknown' | 'anonymous' | 'authenticated';

/**
 * La sesión de la persona en el navegador.
 *
 * - El **token de acceso** (15 minutos) vive solo en memoria: no se guarda en `localStorage` ni en ningún otro sitio que
 *   un fallo de seguridad de la página pueda leer.
 * - El **token de renovación** está en una cookie que JavaScript no puede leer: lo gestiona el navegador y el servidor.
 * - Al recargar la página la memoria se vacía, y la sesión se recupera pidiendo al servidor que la renueve con esa cookie.
 *
 * Renovar es una operación «de una en una»: si varias peticiones fallan a la vez por token caducado, todas esperan la misma
 * renovación (el servidor cierra la sesión entera si recibe dos renovaciones con el mismo token).
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly api = inject(Api);

  private readonly currentStatus = signal<SessionStatus>('unknown');
  private readonly currentUser = signal<UserResponse | null>(null);
  private readonly currentToken = signal<string | null>(null);
  private pendingRefresh: Promise<boolean> | null = null;

  readonly status = this.currentStatus.asReadonly();
  readonly user = this.currentUser.asReadonly();
  readonly accessToken = this.currentToken.asReadonly();
  readonly isAuthenticated = computed(() => this.currentStatus() === 'authenticated');

  /** Inicia sesión con el correo y la contraseña. Si falla, el error HTTP llega tal cual para que la pantalla lo explique. */
  async login(email: string, password: string): Promise<void> {
    const session = await this.api.invoke(loginOperation, { body: { email, password } });
    this.open(session);
  }

  /** Al arrancar la aplicación: recupera la sesión si la había. */
  async restore(): Promise<void> {
    if (!sessionHint.isSet()) {
      this.currentStatus.set('anonymous');
      return;
    }
    await this.refresh();
  }

  /**
   * Renueva la sesión con la cookie. Devuelve `true` si lo ha conseguido. Si hay una renovación en marcha, espera a esa en
   * vez de empezar otra.
   */
  refresh(): Promise<boolean> {
    this.pendingRefresh ??= this.doRefresh().finally(() => {
      this.pendingRefresh = null;
    });
    return this.pendingRefresh;
  }

  /** Cierra la sesión: avisa al servidor (para anular el token de renovación) y la olvida aquí pase lo que pase. */
  async logout(): Promise<void> {
    try {
      await this.api.invoke(logoutOperation);
    } catch (error) {
      console.error('Cerrar sesión: el servidor no ha respondido', error);
    } finally {
      this.close();
    }
  }

  private async doRefresh(): Promise<boolean> {
    try {
      this.open(await this.api.invoke(refreshOperation));
      return true;
    } catch (error) {
      // Sin conexión no se sabe nada: no se borra el indicador para poder reintentar. Con una respuesta del servidor, la
      // sesión ya no vale.
      const serverRefused = error instanceof HttpErrorResponse && error.status > 0;
      this.close(serverRefused);
      return false;
    }
  }

  private open(session: SessionResponse): void {
    this.currentToken.set(session.accessToken);
    this.currentUser.set(session.user);
    this.currentStatus.set('authenticated');
    sessionHint.set();
  }

  private close(forgetHint = true): void {
    this.currentToken.set(null);
    this.currentUser.set(null);
    this.currentStatus.set('anonymous');
    if (forgetHint) {
      sessionHint.clear();
    }
  }
}
