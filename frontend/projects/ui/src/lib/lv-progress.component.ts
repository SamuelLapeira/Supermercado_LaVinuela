import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Barra de progreso (por ejemplo, «te faltan 12,50 € para el envío gratis»). El texto es obligatorio: la barra por sí sola
 * no dice nada a un lector de pantalla.
 */
@Component({
  selector: 'lv-progress',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'lv-progress',
    role: 'progressbar',
    'aria-valuemin': '0',
    '[attr.aria-valuemax]': 'max()',
    '[attr.aria-valuenow]': 'clamped()',
    '[attr.aria-label]': 'label()',
  },
  template: '<span class="lv-progress__fill" [style.width.%]="percent()"></span>',
})
export class LvProgressComponent {
  readonly value = input.required<number>();
  readonly max = input(100);
  readonly label = input.required<string>();

  readonly clamped = computed(() => Math.max(0, Math.min(this.value(), this.max())));
  readonly percent = computed(() => (this.max() > 0 ? (this.clamped() / this.max()) * 100 : 0));
}
