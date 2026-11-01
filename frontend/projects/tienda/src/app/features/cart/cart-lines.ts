/** Una línea de la cesta: qué producto y cuántas unidades. Las funciones devuelven listas nuevas; no modifican la original. */
export interface CartLine {
  readonly slug: string;
  readonly quantity: number;
}

/** Máximo de unidades de un mismo producto en una línea, aparte del stock. */
export const MAX_UNITS_PER_LINE = 99;

function clamp(quantity: number, max: number): number {
  return Math.max(0, Math.min(Math.floor(quantity), Math.min(max, MAX_UNITS_PER_LINE)));
}

/** Suma unidades a una línea (la crea si no existe), sin pasar del máximo permitido. */
export function addToLines(lines: readonly CartLine[], slug: string, quantity: number, max: number): CartLine[] {
  const current = lines.find((line) => line.slug === slug)?.quantity ?? 0;
  return setLineQuantity(lines, slug, current + quantity, max);
}

/** Fija las unidades de una línea. Con 0 (o menos) la quita; con más del máximo, se queda en el máximo. */
export function setLineQuantity(lines: readonly CartLine[], slug: string, quantity: number, max: number): CartLine[] {
  const next = clamp(quantity, max);
  const others = lines.filter((line) => line.slug !== slug);
  if (next === 0) {
    return others;
  }
  const exists = lines.some((line) => line.slug === slug);
  return exists ? lines.map((line) => (line.slug === slug ? { slug, quantity: next } : line)) : [...others, { slug, quantity: next }];
}

export function removeLine(lines: readonly CartLine[], slug: string): CartLine[] {
  return lines.filter((line) => line.slug !== slug);
}

/** Lee lo guardado con cuidado: lo que no tenga la forma esperada se descarta (nunca debe romper la tienda). */
export function parseStoredLines(raw: string | null): CartLine[] {
  if (raw === null) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    const lines: CartLine[] = [];
    for (const entry of parsed as unknown[]) {
      if (typeof entry === 'object' && entry !== null) {
        const candidate = entry as Record<string, unknown>;
        const slug = candidate['slug'];
        const quantity = candidate['quantity'];
        if (typeof slug === 'string' && typeof quantity === 'number' && Number.isInteger(quantity) && quantity > 0 && quantity <= MAX_UNITS_PER_LINE) {
          lines.push({ slug, quantity });
        }
      }
    }
    return lines;
  } catch {
    return [];
  }
}
