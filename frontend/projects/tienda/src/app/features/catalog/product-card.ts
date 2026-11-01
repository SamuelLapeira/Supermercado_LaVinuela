import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatEuro } from 'util';
import { LvBadgeComponent, LvButtonDirective, LvQuantityStepperComponent } from 'ui';
import { CartStore } from '../cart/cart.store';
import { discountPercent } from './catalog-filter';
import { CatalogService } from './catalog.service';
import type { Product } from './catalog.models';

/**
 * Tarjeta de producto de los listados: imagen provisional, marca, nombre, formato, precio y el botón de añadir, que se
 * convierte en un selector de cantidad cuando el producto ya está en la cesta.
 */
@Component({
  selector: 'lv-product-card',
  imports: [RouterLink, LvBadgeComponent, LvButtonDirective, LvQuantityStepperComponent],
  templateUrl: './product-card.html',
  styleUrl: './product-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductCard {
  private readonly cart = inject(CartStore);
  private readonly catalog = inject(CatalogService);

  readonly product = input.required<Product>();

  protected readonly quantity = computed(() => this.cart.quantityOf(this.product().slug));
  protected readonly tone = computed(() => this.catalog.category(this.product().categorySlug)?.tone ?? 1);
  protected readonly discount = computed(() => discountPercent(this.product()));
  protected readonly price = computed(() => formatEuro(this.product().price));
  protected readonly oldPrice = computed(() => {
    const previous = this.product().compareAtPrice;
    return previous === undefined ? null : formatEuro(previous);
  });
  protected readonly isLocal = computed(() => this.product().tags.includes('local'));
  protected readonly lowStock = computed(() => this.product().stock > 0 && this.product().stock <= 5);

  protected add(): void {
    this.cart.add(this.product().slug);
  }

  protected setQuantity(quantity: number): void {
    this.cart.setQuantity(this.product().slug, quantity);
  }
}
