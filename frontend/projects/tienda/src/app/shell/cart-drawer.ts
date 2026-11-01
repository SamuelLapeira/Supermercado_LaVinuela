import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatEuro } from 'util';
import { LvButtonDirective, LvProgressComponent, LvQuantityStepperComponent } from 'ui';
import { CartStore } from '../features/cart/cart.store';
import { DELIVERY_DEMO } from '../features/cart/delivery-demo';
import { remainingForFreeShipping } from '../features/cart/delivery-pricing';

/**
 * Panel lateral con la cesta, que se abre desde la cabecera. Usa el elemento `<dialog>` del navegador: el foco queda
 * dentro mientras está abierto, se cierra con Escape y devuelve el foco al botón que lo abrió.
 */
@Component({
  selector: 'lv-cart-drawer',
  imports: [RouterLink, LvButtonDirective, LvProgressComponent, LvQuantityStepperComponent],
  templateUrl: './cart-drawer.html',
  styleUrl: './cart-drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartDrawer {
  protected readonly cart = inject(CartStore);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly freeFrom = DELIVERY_DEMO.freeShippingFrom;
  protected readonly missing = computed(() => remainingForFreeShipping(this.cart.subtotal()));
  protected readonly subtotalText = computed(() => formatEuro(this.cart.subtotal()));
  protected readonly missingText = computed(() => formatEuro(this.missing()));

  constructor() {
    effect(() => {
      const dialog = this.dialog().nativeElement;
      const isOpen = dialog.hasAttribute('open');
      if (this.cart.drawerOpen() && !isOpen) {
        if (typeof dialog.showModal === 'function') {
          dialog.showModal();
        } else {
          dialog.setAttribute('open', '');
        }
      } else if (!this.cart.drawerOpen() && isOpen) {
        if (typeof dialog.close === 'function') {
          dialog.close();
        } else {
          dialog.removeAttribute('open');
        }
      }
    });
  }

  protected money(amount: number): string {
    return formatEuro(amount);
  }

  /** Un clic en el fondo oscuro (fuera del panel) cierra el panel. */
  protected onDialogClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.cart.closeDrawer();
    }
  }
}
