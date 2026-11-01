import type { ListingFilters, Product, SortKey } from './catalog.models';

/** Texto sin acentos, en minúsculas y sin espacios sobrantes: «Aceite de Oliva» y «aceite de olíva» coinciden. */
export function normalizeText(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

/**
 * Busca productos: cada palabra de la búsqueda tiene que aparecer en el nombre, la marca o el formato. Los que coinciden
 * por el nombre salen antes que los que solo coinciden por la marca.
 */
export function searchProducts(products: readonly Product[], query: string): Product[] {
  const words = normalizeText(query).split(/\s+/).filter((word) => word.length > 0);
  if (words.length === 0) {
    return [];
  }
  const scored = products
    .map((product, index) => {
      const name = normalizeText(product.name);
      const rest = normalizeText(`${product.brand} ${product.format}`);
      const matches = words.every((word) => name.includes(word) || rest.includes(word));
      const inName = words.filter((word) => name.includes(word)).length;
      return { product, index, matches, inName };
    })
    .filter((entry) => entry.matches);
  scored.sort((a, b) => b.inName - a.inName || a.index - b.index);
  return scored.map((entry) => entry.product);
}

/** Aplica los filtros del listado: subcategoría, marcas (cualquiera de las elegidas) y etiquetas (todas las elegidas). */
export function filterProducts(products: readonly Product[], filters: Pick<ListingFilters, 'subcategory' | 'brands' | 'tags'>): Product[] {
  return products.filter(
    (product) =>
      (filters.subcategory === null || product.subcategorySlug === filters.subcategory) &&
      (filters.brands.length === 0 || filters.brands.includes(product.brand)) &&
      filters.tags.every((tag) => product.tags.includes(tag)),
  );
}

/** Ordena sin modificar la lista original. Con «relevancia» se respeta el orden dado, pero lo agotado va al final. */
export function sortProducts(products: readonly Product[], sort: SortKey): Product[] {
  const copy = [...products];
  switch (sort) {
    case 'precio-asc':
      return copy.sort((a, b) => a.price - b.price);
    case 'precio-desc':
      return copy.sort((a, b) => b.price - a.price);
    case 'nombre':
      return copy.sort((a, b) => a.name.localeCompare(b.name, 'es'));
    case 'relevancia':
      return copy.sort((a, b) => Number(b.stock > 0) - Number(a.stock > 0));
  }
}

/** Cuántos productos hay de cada valor (por ejemplo, de cada marca), ordenado de más a menos y luego por nombre. */
export function countBy(products: readonly Product[], key: (product: Product) => string): { value: string; count: number }[] {
  const counts = new Map<string, number>();
  for (const product of products) {
    const value = key(product);
    counts.set(value, (counts.get(value) ?? 0) + 1);
  }
  return [...counts.entries()]
    .map(([value, count]) => ({ value, count }))
    .sort((a, b) => b.count - a.count || a.value.localeCompare(b.value, 'es'));
}

/** Porcentaje de descuento sobre el precio anterior, o `null` si no está de oferta. */
export function discountPercent(product: Pick<Product, 'price' | 'compareAtPrice'>): number | null {
  if (product.compareAtPrice === undefined || product.compareAtPrice <= product.price) {
    return null;
  }
  return Math.round((1 - product.price / product.compareAtPrice) * 100);
}
