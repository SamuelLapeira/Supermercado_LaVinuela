import { DELIVERY_DEMO } from './delivery-demo';

/** Resultado de calcular el envío de un pedido. */
export type DeliveryQuote =
  | { readonly kind: 'out-of-zone' }
  | { readonly kind: 'fee'; readonly fee: number; readonly free: boolean };

/**
 * Coste del envío según el importe del pedido y la distancia por carretera. Fuera de la distancia máxima no se reparte;
 * por encima del importe de envío gratis, no se cobra.
 */
export function quoteDelivery(subtotal: number, km: number): DeliveryQuote {
  if (km > DELIVERY_DEMO.maxKm) {
    return { kind: 'out-of-zone' };
  }
  if (subtotal >= DELIVERY_DEMO.freeShippingFrom) {
    return { kind: 'fee', fee: 0, free: true };
  }
  const band = DELIVERY_DEMO.bands.find((candidate) => km <= candidate.upToKm);
  return band ? { kind: 'fee', fee: band.fee, free: false } : { kind: 'out-of-zone' };
}

/** Lo que falta para llegar al pedido mínimo (0 si ya se ha llegado). */
export function remainingForMinimum(subtotal: number): number {
  return Math.max(0, roundCents(DELIVERY_DEMO.minimumOrder - subtotal));
}

/** Lo que falta para el envío gratis (0 si ya se ha llegado). */
export function remainingForFreeShipping(subtotal: number): number {
  return Math.max(0, roundCents(DELIVERY_DEMO.freeShippingFrom - subtotal));
}

function roundCents(amount: number): number {
  return Math.round((amount + Number.EPSILON) * 100) / 100;
}
