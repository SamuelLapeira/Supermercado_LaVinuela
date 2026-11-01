/**
 * Un indicador de «esta persona tenía una sesión abierta», para no pedir al servidor que renueve la sesión de quien nunca
 * ha entrado (cada visita anónima dejaría un error 401 inútil en la consola).
 *
 * Solo guarda un «sí» o un «no»: **ningún token ni dato personal**. El token de renovación vive en una cookie que
 * JavaScript no puede leer, y el de acceso, solo en la memoria de la página.
 */
const KEY = 'lv_session_hint';

/* Excepción justificada a la regla de no usar el almacenamiento del navegador: aquí solo se guarda un indicador inocuo. */
function storage(): Storage | null {
  try {
    // eslint-disable-next-line no-restricted-globals
    return localStorage;
  } catch {
    return null;
  }
}

export const sessionHint = {
  isSet(): boolean {
    return storage()?.getItem(KEY) === '1';
  },
  set(): void {
    try {
      storage()?.setItem(KEY, '1');
    } catch {
      // Sin almacenamiento disponible: se pierde solo la comodidad de no hacer una petición inútil.
    }
  },
  clear(): void {
    try {
      storage()?.removeItem(KEY);
    } catch {
      // Igual que arriba.
    }
  },
};
