/**
 * Reglas del formulario de registro. Son las mismas que aplica el servidor (docs/identity.md): sirven para avisar a la
 * persona mientras escribe, pero la validación que cuenta es la del servidor, que además rechaza las contraseñas más
 * comunes y las que contienen el usuario o el correo.
 */

export const PASSWORD_MIN_LENGTH = 10;
export const PASSWORD_MAX_LENGTH = 128;

export const USERNAME_MESSAGE =
  'El nombre de usuario debe tener entre 3 y 30 caracteres: letras, números, punto, guion o guion bajo.';
export const EMAIL_MESSAGE = 'Escribe un correo electrónico válido.';
export const PASSWORD_MESSAGE = 'La contraseña no cumple todas las condiciones.';
export const TERMS_MESSAGE = 'Tienes que aceptar las condiciones y la política de privacidad.';
export const CODE_MESSAGE = 'El código tiene 6 cifras.';

const USERNAME = /^[a-z0-9][a-z0-9._-]{1,28}[a-z0-9]$/;
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;
const CODE = /^[0-9]{6}$/;

export function normalizeUsername(raw: string): string {
  return raw.trim().toLowerCase();
}

export function normalizeEmail(raw: string): string {
  return raw.trim().toLowerCase();
}

export function isUsernameValid(raw: string): boolean {
  return USERNAME.test(normalizeUsername(raw));
}

export function isEmailValid(raw: string): boolean {
  const email = normalizeEmail(raw);
  return email.length <= 254 && EMAIL.test(email);
}

export function isCodeValid(raw: string): boolean {
  return CODE.test(raw);
}

/** Una condición de la contraseña y si se cumple ahora mismo. */
export interface PasswordCheck {
  readonly id: 'length' | 'upper' | 'lower' | 'digit';
  readonly label: string;
  readonly met: boolean;
}

/** Las condiciones de la contraseña, con su estado para lo que la persona lleva escrito. Cuenta caracteres, no bytes. */
export function passwordChecks(password: string): readonly PasswordCheck[] {
  const length = [...password].length;
  return [
    { id: 'length', label: `Al menos ${PASSWORD_MIN_LENGTH} caracteres`, met: length >= PASSWORD_MIN_LENGTH && length <= PASSWORD_MAX_LENGTH },
    { id: 'upper', label: 'Una mayúscula', met: /\p{Lu}/u.test(password) },
    { id: 'lower', label: 'Una minúscula', met: /\p{Ll}/u.test(password) },
    { id: 'digit', label: 'Un número', met: /\p{Nd}/u.test(password) },
  ];
}

export function isPasswordValid(password: string): boolean {
  return passwordChecks(password).every((check) => check.met);
}

/**
 * La dirección a la que volver tras iniciar sesión, **solo si es de esta misma web**. Una dirección que empiece por «//»
 * o por «http» llevaría a otra web (el clásico «redirección abierta» con el que se engaña a la gente para que escriba su
 * contraseña en un sitio falso), así que se ignora.
 */
export function safeReturnUrl(candidate: string | null | undefined, fallback = '/cuenta'): string {
  if (!candidate || !candidate.startsWith('/') || candidate.startsWith('//') || candidate.includes('\\') || hasControlCharacters(candidate)) {
    return fallback;
  }
  return candidate;
}

/** El correo con la parte local oculta (a***@example.com), igual que lo hace el servidor. */
export function maskEmail(email: string): string {
  const at = email.indexOf('@');
  if (at <= 0) {
    return '***';
  }
  return `${email.charAt(0)}***${email.substring(at)}`;
}

function hasControlCharacters(text: string): boolean {
  return [...text].some((character) => character.charCodeAt(0) < 32);
}
