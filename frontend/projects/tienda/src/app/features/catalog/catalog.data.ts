import type { Category, Nutrition, Product, ProductTag } from './catalog.models';

/**
 * Datos de EJEMPLO para la maqueta navegable: categorías y productos inventados (marcas y precios ficticios). Cuando
 * exista el servidor de catálogo, este fichero desaparece y las mismas pantallas leerán de la API.
 */

export const CATEGORIES: readonly Category[] = [
  {
    slug: 'frutas-verduras', name: 'Frutas y verduras', emoji: '🍎', tone: 1,
    subcategories: [{ slug: 'frutas', name: 'Frutas' }, { slug: 'verduras', name: 'Verduras y hortalizas' }, { slug: 'ensaladas', name: 'Ensaladas y hierbas' }],
  },
  {
    slug: 'carne-charcuteria', name: 'Carne y charcutería', emoji: '🥩', tone: 6,
    subcategories: [{ slug: 'cerdo-iberico', name: 'Cerdo e ibérico' }, { slug: 'pollo-aves', name: 'Pollo y aves' }, { slug: 'embutidos-jamon', name: 'Embutidos y jamón' }],
  },
  {
    slug: 'pescados-mariscos', name: 'Pescados y mariscos', emoji: '🐟', tone: 5,
    subcategories: [{ slug: 'pescado', name: 'Pescado' }, { slug: 'marisco', name: 'Marisco' }],
  },
  {
    slug: 'lacteos-huevos', name: 'Lácteos y huevos', emoji: '🥛', tone: 3,
    subcategories: [{ slug: 'leche', name: 'Leche' }, { slug: 'quesos', name: 'Quesos' }, { slug: 'yogures', name: 'Yogures y mantequilla' }, { slug: 'huevos', name: 'Huevos' }],
  },
  {
    slug: 'panaderia-bolleria', name: 'Panadería y bollería', emoji: '🥖', tone: 4,
    subcategories: [{ slug: 'pan', name: 'Pan' }, { slug: 'bolleria', name: 'Bollería y dulces' }],
  },
  {
    slug: 'despensa', name: 'Despensa', emoji: '🫒', tone: 3,
    subcategories: [{ slug: 'aceites', name: 'Aceites' }, { slug: 'pasta-arroz', name: 'Pasta, arroz y legumbres' }, { slug: 'conservas', name: 'Conservas' }, { slug: 'cafe-miel', name: 'Café, miel y desayuno' }],
  },
  {
    slug: 'bebidas', name: 'Bebidas', emoji: '💧', tone: 5,
    subcategories: [{ slug: 'agua', name: 'Agua' }, { slug: 'refrescos-zumos', name: 'Refrescos y zumos' }, { slug: 'vino-cerveza', name: 'Vinos y cervezas' }],
  },
  {
    slug: 'limpieza-hogar', name: 'Limpieza y hogar', emoji: '🧴', tone: 2,
    subcategories: [{ slug: 'limpieza', name: 'Limpieza' }, { slug: 'papel-detergentes', name: 'Papel y detergentes' }],
  },
];

const SIERRA = 'La Axarquía (Málaga)';

const nutrition = (energy: string, fat: string, saturates: string, carbohydrates: string, sugars: string, protein: string, salt: string): Nutrition => ({
  energy, fat, saturates, carbohydrates, sugars, protein, salt,
});

interface Extra {
  readonly old?: number;
  readonly tags?: readonly ProductTag[];
  readonly stock?: number;
  readonly origin?: string;
  readonly allergens?: readonly string[];
  readonly ingredients?: string;
  readonly nutrition?: Nutrition;
  readonly adults?: boolean;
  readonly description?: string;
}

function product(
  slug: string, name: string, brand: string, format: string, price: number, unitPrice: string,
  categorySlug: string, subcategorySlug: string, emoji: string, extra: Extra = {},
): Product {
  const tags: ProductTag[] = [...(extra.tags ?? [])];
  if (extra.old !== undefined) {
    tags.push('oferta');
  }
  return {
    slug, name, brand, format, price, unitPrice, categorySlug, subcategorySlug, emoji, tags,
    ...(extra.old !== undefined ? { compareAtPrice: extra.old } : {}),
    stock: extra.stock ?? 40,
    description: extra.description ?? `${name}, en formato ${format}. Una selección de nuestro supermercado de La Viñuela.`,
    ...(extra.origin ? { origin: extra.origin } : {}),
    ...(extra.ingredients ? { ingredients: extra.ingredients } : {}),
    allergens: extra.allergens ?? [],
    ...(extra.nutrition ? { nutrition: extra.nutrition } : {}),
    adultsOnly: extra.adults ?? false,
  };
}

export const PRODUCTS: readonly Product[] = [
  // Frutas y verduras
  product('aguacates-hass-500g', 'Aguacates Hass', 'Huerta de la Axarquía', 'bolsa 500 g', 3.49, '6,98 €/kg', 'frutas-verduras', 'frutas', '🥑', { tags: ['local'], origin: SIERRA, description: 'Aguacates cultivados en la costa de la Axarquía, de piel rugosa y pulpa cremosa. Se compran duros y maduran en casa en unos días.' }),
  product('mango-bandeja-2', 'Mango de la Costa Tropical', 'Huerta de la Axarquía', 'bandeja 2 uds', 3.29, '4,11 €/kg', 'frutas-verduras', 'frutas', '🥭', { tags: ['local'], origin: 'Costa Tropical (Granada)' }),
  product('naranjas-mesa-2kg', 'Naranjas de mesa', 'Vega del Guadalhorce', 'malla 2 kg', 2.89, '1,45 €/kg', 'frutas-verduras', 'frutas', '🍊', { old: 3.49, origin: 'Valle del Guadalhorce (Málaga)' }),
  product('platanos-1kg', 'Plátanos de Canarias', 'Plátano de Canarias', 'bandeja 1 kg', 2.35, '2,35 €/kg', 'frutas-verduras', 'frutas', '🍌', { origin: 'Islas Canarias' }),
  product('tomates-pera-1kg', 'Tomates pera', 'Huerta de la Axarquía', 'bandeja 1 kg', 2.19, '2,19 €/kg', 'frutas-verduras', 'verduras', '🍅', { tags: ['local'], origin: SIERRA }),
  product('patatas-lavadas-3kg', 'Patatas lavadas', 'Campo Andaluz', 'saco 3 kg', 3.15, '1,05 €/kg', 'frutas-verduras', 'verduras', '🥔', {}),
  product('lechuga-romana', 'Lechuga romana', 'Campo Andaluz', '1 unidad', 1.05, '1,05 €/ud', 'frutas-verduras', 'ensaladas', '🥬', { tags: ['ecologico'], stock: 3 }),
  product('pimientos-freir-500g', 'Pimientos verdes de freír', 'Huerta de la Axarquía', 'bandeja 500 g', 1.79, '3,58 €/kg', 'frutas-verduras', 'verduras', '🫑', { tags: ['local'], origin: SIERRA }),
  // Carne y charcutería
  product('lomo-iberico-400g', 'Lomo de cerdo ibérico fileteado', 'Dehesa del Sur', 'bandeja 400 g', 6.95, '17,38 €/kg', 'carne-charcuteria', 'cerdo-iberico', '🥩', { origin: 'Andalucía' }),
  product('pechuga-pollo-600g', 'Pechuga de pollo fileteada', 'Corral Andaluz', 'bandeja 600 g', 5.49, '9,15 €/kg', 'carne-charcuteria', 'pollo-aves', '🍗', { old: 6.29 }),
  product('muslos-pollo-1kg', 'Muslos de pollo', 'Corral Andaluz', 'bandeja 1 kg', 3.95, '3,95 €/kg', 'carne-charcuteria', 'pollo-aves', '🍗', {}),
  product('chorizo-sierra-250g', 'Chorizo de la sierra', 'Embutidos del Cerro', 'pieza 250 g', 3.85, '15,40 €/kg', 'carne-charcuteria', 'embutidos-jamon', '🌶️', { tags: ['local', 'sin-gluten'], origin: SIERRA }),
  product('jamon-serrano-150g', 'Jamón serrano loncheado', 'Embutidos del Cerro', 'sobre 150 g', 3.75, '25,00 €/kg', 'carne-charcuteria', 'embutidos-jamon', '🥓', { tags: ['sin-gluten'] }),
  product('salchichon-iberico-200g', 'Salchichón ibérico', 'Dehesa del Sur', 'pieza 200 g', 4.25, '21,25 €/kg', 'carne-charcuteria', 'embutidos-jamon', '🍖', { tags: ['sin-gluten'] }),
  // Pescados y mariscos
  product('boquerones-500g', 'Boquerones limpios', 'Pescadería de Torre del Mar', 'bandeja 500 g', 5.95, '11,90 €/kg', 'pescados-mariscos', 'pescado', '🐟', { tags: ['local'], origin: 'Mar de Alborán', allergens: ['Pescado'] }),
  product('salmon-lomos-280g', 'Salmón en lomos', 'Mar Fresco', '2 lomos 280 g', 6.49, '23,18 €/kg', 'pescados-mariscos', 'pescado', '🍣', { allergens: ['Pescado'] }),
  product('gambas-peladas-400g', 'Gambas peladas congeladas', 'Mar Fresco', 'bolsa 400 g', 7.95, '19,88 €/kg', 'pescados-mariscos', 'marisco', '🦐', { old: 8.95, allergens: ['Crustáceos'] }),
  product('calamar-anillas-500g', 'Calamar en anillas', 'Mar Fresco', 'bolsa 500 g', 5.25, '10,50 €/kg', 'pescados-mariscos', 'marisco', '🦑', { allergens: ['Moluscos'] }),
  product('merluza-rodajas-600g', 'Merluza en rodajas', 'Mar Fresco', 'bandeja 600 g', 7.45, '12,42 €/kg', 'pescados-mariscos', 'pescado', '🐟', { allergens: ['Pescado'], stock: 0 }),
  // Lácteos y huevos
  product('leche-entera-1l', 'Leche entera', 'La Viñuela', 'brik 1 L', 0.95, '0,95 €/L', 'lacteos-huevos', 'leche', '🥛', { old: 1.1, allergens: ['Leche'], nutrition: nutrition('264 kJ / 63 kcal', '3,6 g', '2,3 g', '4,7 g', '4,7 g', '3,2 g', '0,1 g') }),
  product('leche-sin-lactosa-1l', 'Leche semidesnatada sin lactosa', 'La Viñuela', 'brik 1 L', 1.25, '1,25 €/L', 'lacteos-huevos', 'leche', '🥛', { tags: ['sin-lactosa'], allergens: ['Leche'] }),
  product('queso-cabra-curado-250g', 'Queso de cabra curado', 'Quesería de la Sierra', 'cuña 250 g', 4.95, '19,80 €/kg', 'lacteos-huevos', 'quesos', '🧀', { tags: ['local'], origin: SIERRA, allergens: ['Leche'] }),
  product('yogur-natural-4', 'Yogur natural', 'La Viñuela', 'pack 4 uds', 1.65, '3,44 €/kg', 'lacteos-huevos', 'yogures', '🥣', { allergens: ['Leche'] }),
  product('huevos-camperos-12', 'Huevos camperos', 'Granja La Solana', 'docena', 2.95, '0,25 €/ud', 'lacteos-huevos', 'huevos', '🥚', { tags: ['local', 'ecologico'], origin: SIERRA, allergens: ['Huevo'] }),
  product('mantequilla-250g', 'Mantequilla sin sal', 'La Viñuela', 'pastilla 250 g', 2.35, '9,40 €/kg', 'lacteos-huevos', 'yogures', '🧈', { allergens: ['Leche'] }),
  // Panadería y bollería
  product('pan-pueblo-500g', 'Pan de pueblo', 'Panadería de la Plaza', 'hogaza 500 g', 1.6, '3,20 €/kg', 'panaderia-bolleria', 'pan', '🍞', { tags: ['local'], origin: 'La Viñuela (Málaga)', allergens: ['Gluten'] }),
  product('picos-aceite-200g', 'Picos de aceite', 'Panadería de la Plaza', 'bolsa 200 g', 1.35, '6,75 €/kg', 'panaderia-bolleria', 'pan', '🥨', { tags: ['local'], allergens: ['Gluten'] }),
  product('magdalenas-6', 'Magdalenas caseras', 'Dulces del Cerro', 'bolsa 6 uds', 1.95, '0,33 €/ud', 'panaderia-bolleria', 'bolleria', '🧁', { allergens: ['Gluten', 'Huevo', 'Leche'] }),
  product('tortas-aceite-6', 'Tortas de aceite', 'Dulces del Cerro', 'pack 6 uds', 2.3, '0,38 €/ud', 'panaderia-bolleria', 'bolleria', '🥠', { tags: ['local'], allergens: ['Gluten'] }),
  product('pan-molde-integral-450g', 'Pan de molde integral', 'La Viñuela', 'bolsa 450 g', 1.85, '4,11 €/kg', 'panaderia-bolleria', 'pan', '🍞', { allergens: ['Gluten'] }),
  // Despensa
  product('aove-1l', 'Aceite de oliva virgen extra', 'Cooperativa Olivarera de la Axarquía', 'botella 1 L', 8.95, '8,95 €/L', 'despensa', 'aceites', '🫒', { old: 10.2, tags: ['local', 'sin-gluten'], origin: SIERRA, description: 'Aceite de oliva virgen extra de aceituna verdial, de primera extracción en frío. Afrutado, con un ligero picor final.', nutrition: nutrition('3404 kJ / 828 kcal', '92 g', '14 g', '0 g', '0 g', '0 g', '0 g') }),
  product('aove-5l', 'Aceite de oliva virgen extra', 'Cooperativa Olivarera de la Axarquía', 'garrafa 5 L', 39.9, '7,98 €/L', 'despensa', 'aceites', '🫒', { tags: ['local', 'sin-gluten'], origin: SIERRA }),
  product('arroz-redondo-1kg', 'Arroz redondo', 'Campo Andaluz', 'paquete 1 kg', 1.85, '1,85 €/kg', 'despensa', 'pasta-arroz', '🍚', { tags: ['sin-gluten'] }),
  product('espaguetis-500g', 'Espaguetis', 'La Viñuela', 'paquete 500 g', 1.05, '2,10 €/kg', 'despensa', 'pasta-arroz', '🍝', { allergens: ['Gluten'] }),
  product('garbanzos-cocidos-400g', 'Garbanzos cocidos', 'La Viñuela', 'tarro 400 g', 1.25, '3,13 €/kg', 'despensa', 'pasta-arroz', '🫘', { tags: ['sin-gluten'] }),
  product('atun-aceite-3', 'Atún claro en aceite de oliva', 'Mar Fresco', 'pack 3 uds', 3.25, '14,44 €/kg', 'despensa', 'conservas', '🥫', { allergens: ['Pescado'] }),
  product('cafe-molido-250g', 'Café molido natural', 'Tostadero del Sur', 'paquete 250 g', 2.95, '11,80 €/kg', 'despensa', 'cafe-miel', '☕', {}),
  product('miel-azahar-500g', 'Miel de azahar', 'Apicultores de la Axarquía', 'tarro 500 g', 5.95, '11,90 €/kg', 'despensa', 'cafe-miel', '🍯', { tags: ['local', 'sin-gluten'], origin: SIERRA }),
  // Bebidas
  product('agua-15l-6', 'Agua mineral', 'Fuente de la Sierra', 'pack 6 botellas 1,5 L', 3.6, '0,40 €/L', 'bebidas', 'agua', '💧', { tags: ['local'], origin: SIERRA }),
  product('refresco-cola-2l', 'Refresco de cola', 'Burbujas', 'botella 2 L', 1.59, '0,80 €/L', 'bebidas', 'refrescos-zumos', '🥤', {}),
  product('zumo-naranja-1l', 'Zumo de naranja exprimido', 'Vega del Guadalhorce', 'botella 1 L', 2.45, '2,45 €/L', 'bebidas', 'refrescos-zumos', '🧃', { tags: ['sin-gluten'] }),
  product('cerveza-rubia-6', 'Cerveza rubia', 'Cervecera del Sur', 'pack 6 latas 33 cl', 5.45, '2,75 €/L', 'bebidas', 'vino-cerveza', '🍺', { adults: true, allergens: ['Gluten'] }),
  product('vino-tinto-montes-75cl', 'Vino tinto joven de los Montes de Málaga', 'Bodega de la Sierra', 'botella 75 cl', 4.95, '6,60 €/L', 'bebidas', 'vino-cerveza', '🍷', { adults: true, tags: ['local'], origin: 'Montes de Málaga', allergens: ['Sulfitos'] }),
  product('vino-dulce-malaga-50cl', 'Vino dulce de Málaga', 'Bodega de la Sierra', 'botella 50 cl', 6.5, '13,00 €/L', 'bebidas', 'vino-cerveza', '🍷', { adults: true, tags: ['local'], origin: 'Málaga', allergens: ['Sulfitos'] }),
  // Limpieza y hogar
  product('detergente-40', 'Detergente líquido', 'Hogar Limpio', '40 lavados', 7.95, '0,20 €/lavado', 'limpieza-hogar', 'papel-detergentes', '🧴', { old: 9.5 }),
  product('lavavajillas-750ml', 'Lavavajillas a mano', 'Hogar Limpio', 'botella 750 ml', 1.95, '2,60 €/L', 'limpieza-hogar', 'limpieza', '🧽', {}),
  product('papel-higienico-12', 'Papel higiénico', 'Hogar Limpio', 'pack 12 rollos', 4.95, '0,41 €/rollo', 'limpieza-hogar', 'papel-detergentes', '🧻', {}),
  product('limpiador-multiusos-750ml', 'Limpiador multiusos', 'Hogar Limpio', 'pulverizador 750 ml', 1.75, '2,33 €/L', 'limpieza-hogar', 'limpieza', '🧼', {}),
];
