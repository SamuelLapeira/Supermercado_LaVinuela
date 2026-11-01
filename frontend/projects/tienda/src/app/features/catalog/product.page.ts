import { ChangeDetectionStrategy, Component, computed, effect, inject, linkedSignal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { formatEuro } from 'util';
import { LvAlertComponent, LvBadgeComponent, LvButtonDirective, LvQuantityStepperComponent } from 'ui';
import { CartStore } from '../cart/cart.store';
import { discountPercent } from './catalog-filter';
import { PRODUCT_TAG_LABELS } from './catalog.models';
import { CatalogService } from './catalog.service';
import { ProductCard } from './product-card';

/** Ficha de producto: imagen, precio, cantidad, información útil (ingredientes, alérgenos, nutrición) y productos parecidos. */
@Component({
  selector: 'lv-product-page',
  imports: [RouterLink, LvAlertComponent, LvBadgeComponent, LvButtonDirective, LvQuantityStepperComponent, ProductCard],
  templateUrl: './product.page.html',
  styleUrl: './product.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductPage {
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  private readonly pageTitle = inject(Title);
  protected readonly cart = inject(CartStore);
  protected readonly tagLabels = PRODUCT_TAG_LABELS;

  private readonly slug = toSignal(this.route.paramMap.pipe(map((params) => params.get('slug') ?? '')), {
    initialValue: this.route.snapshot.paramMap.get('slug') ?? '',
  });

  protected readonly product = computed(() => this.catalog.product(this.slug()));
  protected readonly category = computed(() => this.catalog.category(this.product()?.categorySlug ?? ''));
  protected readonly subcategory = computed(() => this.category()?.subcategories.find((sub) => sub.slug === this.product()?.subcategorySlug));
  protected readonly related = computed(() => {
    const product = this.product();
    return product ? this.catalog.related(product) : [];
  });
  protected readonly tone = computed(() => this.category()?.tone ?? 1);
  protected readonly discount = computed(() => {
    const product = this.product();
    return product ? discountPercent(product) : null;
  });
  protected readonly price = computed(() => formatEuro(this.product()?.price ?? 0));
  protected readonly oldPrice = computed(() => {
    const previous = this.product()?.compareAtPrice;
    return previous === undefined ? null : formatEuro(previous);
  });
  protected readonly inCart = computed(() => this.cart.quantityOf(this.slug()));
  protected readonly lowStock = computed(() => {
    const stock = this.product()?.stock ?? 0;
    return stock > 0 && stock <= 5;
  });

  /** Unidades que se van a añadir. Vuelve a 1 al cambiar de producto. */
  protected readonly quantity = linkedSignal(() => {
    this.slug();
    return 1;
  });

  protected readonly addLabel = computed(() => `Añadir ${this.quantity()} ${this.quantity() === 1 ? 'unidad' : 'unidades'} a la cesta`);

  constructor() {
    effect(() => {
      const product = this.product();
      this.pageTitle.setTitle(product ? `${product.name} · La Viñuela` : 'Producto no encontrado · La Viñuela');
    });
  }

  protected add(): void {
    const product = this.product();
    if (product) {
      this.cart.add(product.slug, this.quantity());
      this.cart.openDrawer();
    }
  }
}
