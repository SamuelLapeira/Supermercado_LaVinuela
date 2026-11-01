import { Injectable } from '@angular/core';
import { CATEGORIES, PRODUCTS } from './catalog.data';
import type { Category, Product } from './catalog.models';

/**
 * Acceso al catálogo. Hoy lee los datos de ejemplo; cuando exista el servidor de catálogo, este servicio hará las
 * llamadas a la API y las pantallas no cambiarán.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  readonly categories: readonly Category[] = CATEGORIES;
  readonly products: readonly Product[] = PRODUCTS;

  category(slug: string): Category | undefined {
    return this.categories.find((category) => category.slug === slug);
  }

  product(slug: string): Product | undefined {
    return this.products.find((product) => product.slug === slug);
  }

  byCategory(slug: string): Product[] {
    return this.products.filter((product) => product.categorySlug === slug);
  }

  offers(): Product[] {
    return this.products.filter((product) => product.tags.includes('oferta'));
  }

  local(): Product[] {
    return this.products.filter((product) => product.tags.includes('local'));
  }

  /** Otros productos de la misma categoría, empezando por los de la misma subcategoría. */
  related(product: Product, limit = 4): Product[] {
    const others = this.products.filter((other) => other.slug !== product.slug && other.categorySlug === product.categorySlug && other.stock > 0);
    const sameSub = others.filter((other) => other.subcategorySlug === product.subcategorySlug);
    const rest = others.filter((other) => other.subcategorySlug !== product.subcategorySlug);
    return [...sameSub, ...rest].slice(0, limit);
  }
}
