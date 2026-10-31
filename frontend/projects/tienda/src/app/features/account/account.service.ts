import { Injectable, inject } from '@angular/core';
import {
  Api,
  registerAccount,
  resendVerification as resendVerificationOperation,
  verifyEmail as verifyEmailOperation,
} from 'api';
import type {
  RegisterRequest,
  RegistrationResponse,
  ResendVerificationRequest,
  ResendVerificationResponse,
  VerifyEmailRequest,
} from 'api';

/**
 * Las llamadas del servidor relacionadas con las cuentas. Es el <b>único</b> sitio de la tienda que conoce la forma
 * exacta del cliente generado: las pantallas solo ven estos tres métodos. Si el cliente generado cambia de forma, se
 * ajusta aquí y nada más.
 *
 * Si una llamada falla, la promesa se rechaza con un error HTTP: con `problemFrom(error)` (de la librería `api`) se
 * obtiene el error estándar del servidor.
 */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly api = inject(Api);

  register(body: RegisterRequest): Promise<RegistrationResponse> {
    return this.api.invoke(registerAccount, { body });
  }

  verifyEmail(body: VerifyEmailRequest): Promise<void> {
    return this.api.invoke(verifyEmailOperation, { body });
  }

  resendVerification(body: ResendVerificationRequest): Promise<ResendVerificationResponse> {
    return this.api.invoke(resendVerificationOperation, { body });
  }
}
