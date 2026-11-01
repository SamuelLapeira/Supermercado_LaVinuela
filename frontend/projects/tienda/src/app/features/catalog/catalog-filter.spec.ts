import { countBy, discountPercent, filterProducts, normalizeText, searchProducts, sortProducts } from './catalog-filter';
import { PRODUCTS } from './catalog.data';
import type { Product } from './catalog.models';

const names = (products: readonly Product[]) => products.map((product) => product.slug);

describe('normalizeText', () => {
  it('ignores accents, case and surrounding spaces', () => {
    expect(normalizeText('  Aceite de OLÍVA ')).toBe('aceite de oliva');
    expect(normalizeText('Jamón')).toBe('jamon');
  });
});

describe('searchProducts', () => {
  it('finds products ignoring accents and case', () => {
    expect(names(searchProducts(PRODUCTS, 'JAMON')).includes('jamon-serrano-150g')).toBe(true);
  });

  it('needs every word of the search', () => {
    const results = searchProducts(PRODUCTS, 'aceite oliva 5');
    expect(names(results)).toEqual(['aove-5l']);
  });

  it('finds by brand too, but lists name matches first', () => {
    const results = searchProducts(PRODUCTS, 'mar fresco');
    expect(results.length).toBeGreaterThan(3);
    expect(results.every((product) => product.brand === 'Mar Fresco')).toBe(true);
  });

  it('returns nothing for an empty or unknown search', () => {
    expect(searchProducts(PRODUCTS, '   ')).toEqual([]);
    expect(searchProducts(PRODUCTS, 'xyzzy')).toEqual([]);
  });
});

describe('filterProducts', () => {
  const none = { subcategory: null, brands: [], tags: [] } as const;

  it('keeps everything without filters', () => {
    expect(filterProducts(PRODUCTS, none)).toHaveLength(PRODUCTS.length);
  });

  it('filters by subcategory', () => {
    const results = filterProducts(PRODUCTS, { ...none, subcategory: 'leche' });
    expect(names(results)).toEqual(['leche-entera-1l', 'leche-sin-lactosa-1l']);
  });

  it('accepts any of the chosen brands', () => {
    const results = filterProducts(PRODUCTS, { ...none, brands: ['La Viñuela', 'Burbujas'] });
    expect(results.every((product) => ['La Viñuela', 'Burbujas'].includes(product.brand))).toBe(true);
    expect(results.some((product) => product.brand === 'Burbujas')).toBe(true);
  });

  it('needs every chosen tag', () => {
    const results = filterProducts(PRODUCTS, { ...none, tags: ['local', 'sin-gluten'] });
    expect(results.length).toBeGreaterThan(0);
    expect(results.every((product) => product.tags.includes('local') && product.tags.includes('sin-gluten'))).toBe(true);
  });
});

describe('sortProducts', () => {
  it('sorts by price both ways without touching the original', () => {
    const original = names(PRODUCTS);
    const cheap = sortProducts(PRODUCTS, 'precio-asc');
    const expensive = sortProducts(PRODUCTS, 'precio-desc');

    expect(cheap[0]?.price).toBe(Math.min(...PRODUCTS.map((product) => product.price)));
    expect(expensive[0]?.price).toBe(Math.max(...PRODUCTS.map((product) => product.price)));
    expect(names(PRODUCTS)).toEqual(original);
  });

  it('sorts by name using Spanish rules', () => {
    const sorted = sortProducts(PRODUCTS, 'nombre').map((product) => product.name);
    expect(sorted).toEqual([...sorted].sort((a, b) => a.localeCompare(b, 'es')));
  });

  it('puts what is sold out at the end when sorting by relevance', () => {
    const sorted = sortProducts(PRODUCTS, 'relevancia');
    const firstSoldOut = sorted.findIndex((product) => product.stock === 0);
    expect(firstSoldOut).toBeGreaterThan(-1);
    expect(sorted.slice(firstSoldOut).every((product) => product.stock === 0)).toBe(true);
  });
});

describe('countBy', () => {
  it('counts and sorts from most to least', () => {
    const counts = countBy(PRODUCTS, (product) => product.brand);
    expect(counts[0]?.count).toBeGreaterThanOrEqual(counts[1]?.count ?? 0);
    expect(counts.reduce((sum, entry) => sum + entry.count, 0)).toBe(PRODUCTS.length);
  });
});

describe('discountPercent', () => {
  it('computes the discount of products on offer', () => {
    expect(discountPercent({ price: 8.95, compareAtPrice: 10.2 })).toBe(12);
    expect(discountPercent({ price: 0.95, compareAtPrice: 1.1 })).toBe(14);
  });

  it('is null when there is no offer', () => {
    expect(discountPercent({ price: 1 })).toBeNull();
    expect(discountPercent({ price: 1, compareAtPrice: 1 })).toBeNull();
  });
});

describe('the sample data', () => {
  it('has unique slugs and every product belongs to a real category and subcategory', async () => {
    const { CATEGORIES } = await import('./catalog.data');
    expect(new Set(PRODUCTS.map((product) => product.slug)).size).toBe(PRODUCTS.length);
    for (const product of PRODUCTS) {
      const category = CATEGORIES.find((entry) => entry.slug === product.categorySlug);
      expect(category, product.slug).toBeDefined();
      expect(category?.subcategories.some((sub) => sub.slug === product.subcategorySlug), product.slug).toBe(true);
    }
  });

  it('marks as on offer exactly the products with a previous price', () => {
    for (const product of PRODUCTS) {
      expect(product.tags.includes('oferta'), product.slug).toBe(product.compareAtPrice !== undefined);
    }
  });
});
