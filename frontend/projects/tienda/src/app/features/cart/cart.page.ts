import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatEuro } from 'util';
import { LvAlertComponent, LvButtonDirective, LvProgressComponent, LvQuantityStepperComponent } from 'ui';
import { CartStore } from './cart.store';
import { DELIVERY_DEMO } from './delivery-demo';
import { remainingForFreeShipping, remainingForMinimum } from './delivery-pricing';

/**
 * Página de la cesta: las líneas con sus cantidades, el cálculo del envío según el lugar de entrega y el resumen con el
 * total. Avisa de lo que falta para el pedido mínimo y para el envío gratis.
 */
@Component({
  selector: 'lv-cart-page',
  imports: [RouterLink, LvAlertComponent, LvButtonDirective, LvProgressComponent, LvQuantityStepperComponent],
  templateUrl: './cart.page.html',
  styleUrl: './cart.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartPage {
  protected readonly cart = inject(CartStore);
  protected readonly demo = DELIVERY_DEMO;

  protected readonly missingForMinimum = computed(() => remainingForMinimum(this.cart.subtotal()));
  protected readonly missingForFree = computed(() => remainingForFreeShipping(this.cart.subtotal()));

  protected money(amount: number): string {
    return formatEuro(amount);
  }

  protected onZone(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.cart.selectZone(value === '' ? null : value);
  }
}
