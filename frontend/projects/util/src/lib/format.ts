const EURO = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' });

/** Un importe en euros como se escribe en España: 1.234,50 € (con el espacio sin salto antes del símbolo). */
export function formatEuro(amount: number): string {
  return EURO.format(amount);
}

/** Redondea a céntimos (evita 0,1 + 0,2 = 0,30000000000000004). */
export function roundToCents(amount: number): number {
  return Math.round((amount + Number.EPSILON) * 100) / 100;
}
