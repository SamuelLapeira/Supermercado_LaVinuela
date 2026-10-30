import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

/**
 * Selector de cantidad con botones «−» y «+» (el control «Añadir» de las tarjetas de producto).
 *
 * ```html
 * <lv-quantity-stepper itemName="Leche entera" [max]="stock" [(value)]="cantidad" />
 * ```
 *
 * Los botones tienen nombre accesible propio («Añadir una unidad de Leche entera») y el valor se anuncia
 * a los lectores de pantalla al cambiar (el elemento `<output>` es una región de estado).
 */
@Component({
  selector: 'lv-quantity-stepper',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'lv-stepper',
    role: 'group',
    '[attr.aria-label]': 'groupLabel()',
  },
  template: `
    <button
      type="button"
      class="lv-stepper__btn"
      [disabled]="value() <= min()"
      [attr.aria-label]="'Quitar una unidad de ' + itemName()"
      (click)="step(-1)"
    >
      <span aria-hidden="true">−</span>
    </button>
    <output class="lv-stepper__value">{{ value() }}</output>
    <button
      type="button"
      class="lv-stepper__btn"
      [disabled]="value() >= max()"
      [attr.aria-label]="'Añadir una unidad de ' + itemName()"
      (click)="step(1)"
    >
      <span aria-hidden="true">+</span>
    </button>
  `,
})
export class LvQuantityStepperComponent {
  /** Cantidad actual (enlace bidireccional con `[(value)]`). */
  readonly value = model(1);
  readonly min = input(0);
  readonly max = input(99);
  /** Nombre del producto, para los nombres accesibles de los botones. */
  readonly itemName = input('producto');

  readonly groupLabel = computed(() => `Cantidad de ${this.itemName()}`);

  step(delta: number): void {
    const next = Math.min(this.max(), Math.max(this.min(), this.value() + delta));
    if (next !== this.value()) {
      this.value.set(next);
    }
  }
}
