import { parseStoredLines } from './cart-lines';
import type { CartLine } from './cart-lines';

/**
 * Lo que se recuerda de la cesta entre visitas, **solo en la maqueta**: las líneas y la zona de entrega elegida. No es
 * información sensible. Cuando exista el servidor, la cesta vivirá allí (ya prevista en el diseño) y esto desaparecerá.
 */
export interface StoredCart {
  readonly lines: CartLine[];
  readonly zone: string | null;
}

const KEY = 'lv_demo_cart';

/* Excepción justificada a la regla de no usar el almacenamiento del navegador: solo se guardan productos de ejemplo. */
function storage(): Storage | null {
  try {
    // eslint-disable-next-line no-restricted-globals
    return localStorage;
  } catch {
    return null;
  }
}

export function readStoredCart(): StoredCart {
  const raw = storage()?.getItem(KEY) ?? null;
  if (raw === null) {
    return { lines: [], zone: null };
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed === 'object' && parsed !== null) {
      const candidate = parsed as Record<string, unknown>;
      const zone = typeof candidate['zone'] === 'string' ? candidate['zone'] : null;
      return { lines: parseStoredLines(JSON.stringify(candidate['lines'] ?? [])), zone };
    }
  } catch {
    // Lo guardado está dañado: se empieza con una cesta vacía.
  }
  return { lines: [], zone: null };
}

export function writeStoredCart(cart: StoredCart): void {
  try {
    storage()?.setItem(KEY, JSON.stringify(cart));
  } catch {
    // Sin almacenamiento disponible: la cesta sigue funcionando, solo que no se recuerda entre visitas.
  }
}
