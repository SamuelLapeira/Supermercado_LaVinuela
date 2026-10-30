import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type LvAlertVariant = 'ok' | 'warn' | 'danger' | 'info';

/**
 * Aviso dentro de la página. Los de tipo `danger` se anuncian de inmediato a los lectores de pantalla
 * (`role="alert"`); el resto, de forma educada (`role="status"`).
 *
 * El icono acompaña siempre al color: el estado nunca depende solo del color.
 */
@Component({
  selector: 'lv-alert',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': "'lv-alert lv-alert--' + variant()",
    '[attr.role]': 'role()',
  },
  template: `
    <span class="lv-alert__icon" aria-hidden="true">
      @switch (variant()) {
        @case ('ok') {
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9.5" /><path d="M7.5 12.5l3 3 6-6.5" /></svg>
        }
        @case ('warn') {
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.5l9.5 16.5h-19z" /><path d="M12 10v4.5M12 17.5v.01" /></svg>
        }
        @case ('danger') {
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9.5" /><path d="M8.5 8.5l7 7M15.5 8.5l-7 7" /></svg>
        }
        @default {
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9.5" /><path d="M12 11v5.5M12 7.5v.01" /></svg>
        }
      }
    </span>
    <div class="lv-alert__body"><ng-content /></div>
  `,
})
export class LvAlertComponent {
  readonly variant = input<LvAlertVariant>('info');
  readonly role = computed(() => (this.variant() === 'danger' ? 'alert' : 'status'));
}
