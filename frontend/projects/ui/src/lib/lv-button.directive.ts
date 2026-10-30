import { booleanAttribute, computed, Directive, input } from '@angular/core';

export type LvButtonVariant = 'primary' | 'accent' | 'ghost' | 'danger';
export type LvButtonSize = 'medium' | 'large';

/**
 * Botón de La Viñuela. Se aplica a un `<button>` o a un `<a>`:
 *
 * ```html
 * <button lvButton>Añadir al carrito</button>
 * <button lvButton="accent" lvSize="large">Ver ofertas</button>
 * <a lvButton="ghost" href="/carrito">Ver carrito</a>
 * <button lvButton lvBlock>Pagar</button>
 * ```
 *
 * Los estilos están en la capa `components` del sistema de diseño (clases `lv-btn`).
 */
@Directive({
  selector: '[lvButton]',
  host: { '[class]': 'classes()' },
})
export class LvButtonDirective {
  /** Variante: `primary` (por defecto), `accent` (amarillo), `ghost` (contorno) o `danger`. */
  readonly lvButton = input<LvButtonVariant | ''>('');
  readonly lvSize = input<LvButtonSize>('medium');
  /** Ocupa todo el ancho disponible. */
  readonly lvBlock = input(false, { transform: booleanAttribute });

  readonly classes = computed(() => {
    const classes = ['lv-btn', `lv-btn--${this.lvButton() || 'primary'}`];
    if (this.lvSize() === 'large') {
      classes.push('lv-btn--large');
    }
    if (this.lvBlock()) {
      classes.push('lv-btn--block');
    }
    return classes.join(' ');
  });
}
