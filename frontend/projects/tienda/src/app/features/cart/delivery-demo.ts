/**
 * Condiciones de reparto de EJEMPLO para la maqueta. Los valores reales (tarifas, pedido mínimo, envío gratis y distancias)
 * los fijará la empresa y vivirán en los ajustes de la tienda (se editan desde el panel).
 */
export const DELIVERY_DEMO = {
  /** Importe mínimo del pedido, en euros. */
  minimumOrder: 20,
  /** A partir de este importe el envío es gratis. */
  freeShippingFrom: 60,
  /** Distancia máxima de reparto por carretera, en kilómetros. */
  maxKm: 40,
  /** Tarifas por tramo de distancia: hasta X km, Y euros. */
  bands: [
    { upToKm: 10, fee: 3 },
    { upToKm: 20, fee: 4.5 },
    { upToKm: 30, fee: 6 },
    { upToKm: 40, fee: 8 },
  ],
} as const;

export interface DeliveryZone {
  readonly slug: string;
  readonly name: string;
  /** Distancia de ejemplo por carretera desde la tienda. */
  readonly km: number;
}

export const DELIVERY_ZONES: readonly DeliveryZone[] = [
  { slug: 'la-vinuela', name: 'La Viñuela', km: 1 },
  { slug: 'sedella', name: 'Sedella', km: 5 },
  { slug: 'canillas-de-aceituno', name: 'Canillas de Aceituno', km: 9 },
  { slug: 'competa', name: 'Cómpeta', km: 16 },
  { slug: 'periana', name: 'Periana', km: 18 },
  { slug: 'velez-malaga', name: 'Vélez-Málaga', km: 20 },
  { slug: 'torrox', name: 'Torrox', km: 22 },
  { slug: 'torre-del-mar', name: 'Torre del Mar', km: 24 },
  { slug: 'nerja', name: 'Nerja', km: 32 },
  { slug: 'rincon-de-la-victoria', name: 'Rincón de la Victoria', km: 38 },
  { slug: 'malaga', name: 'Málaga capital', km: 52 },
];
