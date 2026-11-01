import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LvButtonDirective } from 'ui';

/** Marcador de posición del pago: la siguiente entrega del diseño añade la dirección, el pago y la confirmación. */
@Component({
  selector: 'lv-checkout-soon-page',
  imports: [RouterLink, LvButtonDirective],
  template: `
    <div class="lv-container lv-stack lv-section soon">
      <p class="big" aria-hidden="true">🚧</p>
      <h1>El pago llega en la próxima entrega</h1>
      <p>Estamos diseñando los pasos de dirección de entrega, pago seguro y confirmación del pedido. Tu cesta se guarda.</p>
      <a lvButton routerLink="/cesta">Volver a la cesta</a>
    </div>
  `,
  styles: `
    .soon {
      justify-items: center;
      padding-block: var(--space-9);
      text-align: center;
    }
    .big {
      font-size: 4rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckoutSoonPage {}
