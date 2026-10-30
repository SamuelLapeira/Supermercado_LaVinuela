package com.lavinuela.shop.identity;

/**
 * Eventos que publica el módulo de identidad. Son parte de su API pública: otros módulos (notificaciones) los escuchan.
 *
 * <p>Se publican dentro de la transacción y se entregan <b>después de confirmarla</b>, solo en memoria: no se guardan
 * en ninguna tabla. Así el código de verificación nunca queda escrito en claro en la base de datos.
 */
public final class IdentityEvents {

    private IdentityEvents() {
    }

    /**
     * Se ha emitido un código de verificación de correo y hay que enviarlo a la persona.
     *
     * @param email correo de destino
     * @param username nombre de usuario (para saludar)
     * @param code código de 6 cifras, en claro: solo existe en memoria el tiempo de enviar el correo
     * @param validForMinutes minutos de validez
     */
    public record VerificationCodeIssued(String email, String username, String code, int validForMinutes) {

        /** El código no se muestra nunca en los registros. */
        @Override
        public String toString() {
            return "VerificationCodeIssued[code=***]";
        }
    }

    /** Alguien ha intentado registrarse con un correo que ya tiene una cuenta activa: hay que avisar al titular. */
    public record AccountAlreadyExists(String email, String username) {
    }
}
