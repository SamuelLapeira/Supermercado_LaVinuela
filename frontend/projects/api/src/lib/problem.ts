import { HttpErrorResponse } from '@angular/common/http';
import type { Problem } from './generated';

/**
 * Códigos de error del servidor que las pantallas saben interpretar. Las pantallas deciden qué mostrar por el
 * <b>código</b>, nunca por el texto del mensaje (que puede cambiar).
 */
export const PROBLEM_CODES = {
  validationFailed: 'VALIDATION_FAILED',
  usernameTaken: 'USERNAME_TAKEN',
  verificationCodeInvalid: 'VERIFICATION_CODE_INVALID',
  verificationCodeExpired: 'VERIFICATION_CODE_EXPIRED',
  verificationTooManyAttempts: 'VERIFICATION_TOO_MANY_ATTEMPTS',
  emailServiceUnavailable: 'EMAIL_SERVICE_UNAVAILABLE',
} as const;

/**
 * El error estándar del servidor (`Problem`) que viaja dentro de un error HTTP, o `null` si el error no es de ese tipo
 * (sin conexión, un fallo del navegador o una respuesta que no tiene la forma esperada).
 */
export function problemFrom(error: unknown): Problem | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const body: unknown = error.error;
  return isProblem(body) ? body : null;
}

/** El código del error del servidor (por ejemplo `USERNAME_TAKEN`), o `null` si no es un error estándar. */
export function problemCode(error: unknown): string | null {
  return problemFrom(error)?.code ?? null;
}

/**
 * Los errores por campo de una validación fallida, listos para mostrarse junto a cada campo: el nombre del campo y su
 * mensaje. Si un campo tiene varios errores se queda con el primero.
 */
export function fieldErrors(problem: Problem | null): Record<string, string> {
  const result: Record<string, string> = {};
  for (const violation of problem?.errors ?? []) {
    if (!(violation.field in result)) {
      result[violation.field] = violation.message;
    }
  }
  return result;
}

function isProblem(value: unknown): value is Problem {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate['code'] === 'string' &&
    typeof candidate['status'] === 'number' &&
    typeof candidate['detail'] === 'string'
  );
}
