import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type LvBadgeVariant = 'offer' | 'age' | 'neutral' | 'ok' | 'warn' | 'danger';

/** Etiqueta pequeña: «Oferta», «+18», «Agotado»... */
@Component({
  selector: 'lv-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': "'lv-badge lv-badge--' + variant()" },
  template: '<ng-content />',
})
export class LvBadgeComponent {
  readonly variant = input<LvBadgeVariant>('neutral');
}
