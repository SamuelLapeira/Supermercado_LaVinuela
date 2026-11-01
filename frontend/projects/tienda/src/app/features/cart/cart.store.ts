import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { roundToCents } from 'util';
import { CatalogService } from '../catalog/catalog.service';
import type { Product } from '../catalog/catalog.models';
import { addToLines, removeLine, setLineQuantity } from './cart-lines';
import { readStoredCart, writeStoredCart } from './cart-storage';
import type { StoredCart } from './cart-storage';
import { DELIVERY_DEMO, DELIVERY_ZONES } from './delivery-demo';
import { quoteDelivery } from './delivery-pricing';
import type { DeliveryQuote } from './delivery-pricing';

export interface CartItem {
  readonly product: Product;
  readonly quantity: number;
  readonly total: number;
}

/**
 * La cesta de la compra. En la maqueta vive en el navegador; con el servidor, será la cesta persistente de la persona.
 * Todos los importes se redondean a céntimos en cada operación.
 */
@Injectable({ providedIn: 'root' })
export class CartStore {
  private readonly catalog = inject(CatalogService);
  private readonly state = signal<StoredCart>(readStoredCart());

  /** El panel lateral de la cesta está abierto. */
  readonly drawerOpen = signal(false);

  readonly zones = DELIVERY_ZONES;

  readonly items = computed<CartItem[]>(() =>
    this.state().lines.flatMap((line) => {
      const product = this.catalog.product(line.slug);
      return product ? [{ product, quantity: line.quantity, total: roundToCents(product.price * line.quantity) }] : [];
    }),
  );
  readonly count = computed(() => this.items().reduce((sum, item) => sum + item.quantity, 0));
  readonly subtotal = computed(() => roundToCents(this.items().reduce((sum, item) => sum + item.total, 0)));

  readonly zone = computed(() => DELIVERY_ZONES.find((zone) => zone.slug === this.state().zone) ?? null);
  readonly delivery = computed<DeliveryQuote | null>(() => {
    const zone = this.zone();
    return zone ? quoteDelivery(this.subtotal(), zone.km) : null;
  });
  /** Coste del envío, o `null` si todavía no se sabe (sin zona) o no se reparte. */
  readonly deliveryFee = computed(() => {
    const delivery = this.delivery();
    return delivery?.kind === 'fee' ? delivery.fee : null;
  });
  readonly total = computed(() => roundToCents(this.subtotal() + (this.deliveryFee() ?? 0)));

  readonly belowMinimum = computed(() => this.subtotal() < DELIVERY_DEMO.minimumOrder);
  readonly hasAdultsOnly = computed(() => this.items().some((item) => item.product.adultsOnly));
  readonly canCheckout = computed(() => this.count() > 0 && !this.belowMinimum() && this.delivery()?.kind === 'fee');

  constructor() {
    effect(() => writeStoredCart(this.state()));
  }

  quantityOf(slug: string): number {
    return this.state().lines.find((line) => line.slug === slug)?.quantity ?? 0;
  }

  add(slug: string, quantity = 1): void {
    const product = this.catalog.product(slug);
    if (product) {
      this.update((lines) => addToLines(lines, slug, quantity, product.stock));
    }
  }

  setQuantity(slug: string, quantity: number): void {
    const product = this.catalog.product(slug);
    if (product) {
      this.update((lines) => setLineQuantity(lines, slug, quantity, product.stock));
    }
  }

  remove(slug: string): void {
    this.update((lines) => removeLine(lines, slug));
  }

  clear(): void {
    this.update(() => []);
  }

  selectZone(slug: string | null): void {
    this.state.update((current) => ({ ...current, zone: slug }));
  }

  openDrawer(): void {
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  private update(change: (lines: StoredCart['lines']) => StoredCart['lines']): void {
    this.state.update((current) => ({ ...current, lines: change(current.lines) }));
  }
}
