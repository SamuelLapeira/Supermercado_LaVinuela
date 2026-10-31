/**
 * Lo que la pantalla de registro le pasa a la de verificación del correo. Viaja en el estado del historial del navegador
 * (no en la dirección web): así el correo no queda escrito en la URL ni en el historial visible, y sobrevive a recargar
 * la página.
 */
export interface VerificationState {
  /** Correo con el que se registró, tal como lo escribió (normalizado). */
  readonly email: string;
  /** El mismo correo con la parte local oculta, para enseñarlo en pantalla. */
  readonly maskedEmail: string;
  /** Segundos que hay que esperar antes de poder pedir otro código. */
  readonly resendAfterSeconds: number;
}

/** Lee el estado de forma segura: devuelve `null` si no está o no tiene la forma esperada. */
export function parseVerificationState(raw: unknown): VerificationState | null {
  if (typeof raw !== 'object' || raw === null) {
    return null;
  }
  const candidate = raw as Record<string, unknown>;
  const { email, maskedEmail, resendAfterSeconds } = candidate;
  if (typeof email === 'string' && typeof maskedEmail === 'string' && typeof resendAfterSeconds === 'number') {
    return { email, maskedEmail, resendAfterSeconds };
  }
  return null;
}
