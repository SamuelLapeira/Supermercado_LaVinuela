/** Etiquetas con las que se filtra el catálogo. */
export type ProductTag = 'local' | 'sin-gluten' | 'sin-lactosa' | 'ecologico' | 'oferta';

export const PRODUCT_TAG_LABELS: Readonly<Record<ProductTag, string>> = {
  local: 'De la sierra y la Axarquía',
  'sin-gluten': 'Sin gluten',
  'sin-lactosa': 'Sin lactosa',
  ecologico: 'Ecológico',
  oferta: 'En oferta',
};

export interface Subcategory {
  readonly slug: string;
  readonly name: string;
}

export interface Category {
  readonly slug: string;
  readonly name: string;
  /** Imagen provisional hasta que haya fotos propias. */
  readonly emoji: string;
  /** Color de fondo de la categoría (1 a 6, ver los tokens `--cat-N`). */
  readonly tone: 1 | 2 | 3 | 4 | 5 | 6;
  readonly subcategories: readonly Subcategory[];
}

export interface Nutrition {
  readonly energy: string;
  readonly fat: string;
  readonly saturates: string;
  readonly carbohydrates: string;
  readonly sugars: string;
  readonly protein: string;
  readonly salt: string;
}

export interface Product {
  readonly slug: string;
  readonly name: string;
  readonly brand: string;
  /** Formato de venta, de precio fijo: «1 L», «bandeja 500 g»... */
  readonly format: string;
  readonly price: number;
  /** Precio anterior, si está de oferta. */
  readonly compareAtPrice?: number;
  /** Precio por unidad de medida: «1,98 €/kg». */
  readonly unitPrice: string;
  readonly categorySlug: string;
  readonly subcategorySlug: string;
  readonly emoji: string;
  readonly tags: readonly ProductTag[];
  readonly stock: number;
  readonly description: string;
  readonly origin?: string;
  readonly ingredients?: string;
  readonly allergens: readonly string[];
  readonly nutrition?: Nutrition;
  /** Solo para mayores de 18 años (alcohol). */
  readonly adultsOnly: boolean;
}

export type SortKey = 'relevancia' | 'precio-asc' | 'precio-desc' | 'nombre';

export const SORT_LABELS: Readonly<Record<SortKey, string>> = {
  relevancia: 'Relevancia',
  'precio-asc': 'Precio: de menor a mayor',
  'precio-desc': 'Precio: de mayor a menor',
  nombre: 'Nombre (A-Z)',
};

export interface ListingFilters {
  readonly subcategory: string | null;
  readonly brands: readonly string[];
  readonly tags: readonly ProductTag[];
  readonly sort: SortKey;
}
