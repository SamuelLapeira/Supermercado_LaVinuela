import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LvButtonDirective } from 'ui';
import { countBy, filterProducts, searchProducts, sortProducts } from './catalog-filter';
import { PRODUCT_TAG_LABELS, SORT_LABELS } from './catalog.models';
import type { ListingFilters, Product, ProductTag, SortKey } from './catalog.models';
import { CatalogService } from './catalog.service';
import { ProductCard } from './product-card';

type Mode = 'category' | 'search' | 'offers';

const PAGE_SIZE = 12;
const FILTER_TAGS: readonly ProductTag[] = ['local', 'sin-gluten', 'sin-lactosa', 'ecologico'];
const SORT_KEYS = Object.keys(SORT_LABELS) as SortKey[];

/**
 * Listado de productos: por categoría, resultados de búsqueda u ofertas. Los filtros (subcategoría, marcas, etiquetas) y el
 * orden viven en la dirección web, así que se pueden compartir y funcionan con el botón de «atrás».
 */
@Component({
  selector: 'lv-listing-page',
  imports: [RouterLink, LvButtonDirective, ProductCard],
  templateUrl: './listing.page.html',
  styleUrl: './listing.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListingPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalog = inject(CatalogService);
  private readonly pageTitle = inject(Title);

  protected readonly mode: Mode = ListingPage.modeOf(this.route.snapshot.data['mode']);
  protected readonly sortLabels = SORT_LABELS;
  protected readonly sortKeys = SORT_KEYS;
  protected readonly tagLabels = PRODUCT_TAG_LABELS;
  protected readonly categories = this.catalog.categories;

  private readonly params = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  protected readonly category = computed(() => (this.mode === 'category' ? this.catalog.category(this.params().get('slug') ?? '') : undefined));
  protected readonly searchText = computed(() => this.query().get('q') ?? '');

  private readonly base = computed<readonly Product[]>(() => {
    switch (this.mode) {
      case 'category':
        return this.category() ? this.catalog.byCategory(this.category()?.slug ?? '') : [];
      case 'offers':
        return this.catalog.offers();
      case 'search':
        return searchProducts(this.catalog.products, this.searchText());
    }
  });

  protected readonly filters = computed<ListingFilters>(() => {
    const query = this.query();
    const sort = query.get('orden');
    return {
      subcategory: query.get('sub'),
      brands: ListingPage.list(query.get('marcas')),
      tags: ListingPage.list(query.get('etiquetas')).filter((tag): tag is ProductTag => FILTER_TAGS.includes(tag as ProductTag)),
      sort: SORT_KEYS.includes(sort as SortKey) ? (sort as SortKey) : 'relevancia',
    };
  });

  protected readonly visible = computed(() => sortProducts(filterProducts(this.base(), this.filters()), this.filters().sort));
  private readonly limit = signal(PAGE_SIZE);
  protected readonly shown = computed(() => this.visible().slice(0, this.limit()));
  protected readonly hasMore = computed(() => this.visible().length > this.limit());

  protected readonly heading = computed(() => {
    switch (this.mode) {
      case 'category':
        return this.category()?.name ?? 'Categoría no encontrada';
      case 'offers':
        return 'Ofertas de la semana';
      case 'search':
        return this.searchText().trim() === '' ? 'Buscar productos' : `Resultados para «${this.searchText().trim()}»`;
    }
  });

  protected readonly subcategoryOptions = computed(() =>
    (this.category()?.subcategories ?? []).map((sub) => ({
      ...sub,
      count: filterProducts(this.base(), { ...this.filters(), subcategory: sub.slug }).length,
    })),
  );
  protected readonly brandOptions = computed(() =>
    countBy(filterProducts(this.base(), { ...this.filters(), brands: [] }), (product) => product.brand),
  );
  protected readonly tagOptions = computed(() =>
    FILTER_TAGS.map((tag) => ({ tag, count: filterProducts(this.base(), { ...this.filters(), tags: [tag] }).length })).filter(
      (option) => option.count > 0 || this.filters().tags.includes(option.tag),
    ),
  );
  protected readonly activeFilterCount = computed(() => {
    const filters = this.filters();
    return (filters.subcategory ? 1 : 0) + filters.brands.length + filters.tags.length;
  });
  protected readonly showFilters = signal(false);

  constructor() {
    effect(() => this.pageTitle.setTitle(`${this.heading()} · La Viñuela`));
  }

  protected isBrandSelected(brand: string): boolean {
    return this.filters().brands.includes(brand);
  }

  protected isTagSelected(tag: ProductTag): boolean {
    return this.filters().tags.includes(tag);
  }

  protected setSubcategory(slug: string | null): void {
    this.go({ sub: slug });
  }

  protected toggleBrand(brand: string): void {
    const brands = this.filters().brands;
    this.go({ marcas: ListingPage.join(brands.includes(brand) ? brands.filter((b) => b !== brand) : [...brands, brand]) });
  }

  protected toggleTag(tag: ProductTag): void {
    const tags = this.filters().tags;
    this.go({ etiquetas: ListingPage.join(tags.includes(tag) ? tags.filter((t) => t !== tag) : [...tags, tag]) });
  }

  protected onSort(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.go({ orden: value === 'relevancia' ? null : value });
  }

  protected clearFilters(): void {
    this.go({ sub: null, marcas: null, etiquetas: null });
  }

  protected showMore(): void {
    this.limit.update((current) => current + PAGE_SIZE);
  }

  protected toggleFilters(): void {
    this.showFilters.update((open) => !open);
  }

  private go(patch: Record<string, string | null>): void {
    this.limit.set(PAGE_SIZE);
    void this.router.navigate([], { relativeTo: this.route, queryParams: patch, queryParamsHandling: 'merge' });
  }

  private static modeOf(value: unknown): Mode {
    return value === 'search' || value === 'offers' ? value : 'category';
  }

  private static list(value: string | null): string[] {
    return value ? value.split(',').filter((entry) => entry.length > 0) : [];
  }

  private static join(values: readonly string[]): string | null {
    return values.length > 0 ? values.join(',') : null;
  }
}
