import { booleanAttribute, ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

let nextFieldId = 0;

/**
 * Campo de formulario: etiqueta visible, ayuda y error, todo enlazado al control para los lectores de pantalla.
 *
 * ```html
 * <lv-field label="Código postal" hint="Cinco cifras" [error]="errorDelCampo">
 *   <input lvControl inputmode="numeric" autocomplete="postal-code" />
 * </lv-field>
 * ```
 *
 * Reglas de accesibilidad que cumple:
 * - La etiqueta está siempre visible (no solo como texto de ejemplo dentro del campo).
 * - La ayuda y el error se enlazan al control con `aria-describedby`.
 * - El error se anuncia al aparecer (`role="alert"`) y marca el control con `aria-invalid`.
 */
@Component({
  selector: 'lv-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'lv-field' },
  template: `
    <label class="lv-field__label" [for]="controlId">
      {{ label() }}
      @if (required()) {
        <span class="lv-field__required" aria-hidden="true">*</span>
      }
    </label>
    <ng-content />
    @if (hint()) {
      <p class="lv-field__hint" [id]="hintId">{{ hint() }}</p>
    }
    @if (error()) {
      <p class="lv-field__error" [id]="errorId" role="alert">{{ error() }}</p>
    }
  `,
})
export class LvFieldComponent {
  readonly label = input.required<string>();
  readonly hint = input('');
  readonly error = input('');
  readonly required = input(false, { transform: booleanAttribute });

  private readonly uid = `lv-field-${++nextFieldId}`;
  readonly controlId = `${this.uid}-control`;
  readonly hintId = `${this.uid}-hint`;
  readonly errorId = `${this.uid}-error`;

  /** Identificadores de la ayuda y del error que existen en este momento, o `null` si no hay ninguno. */
  readonly describedBy = computed(() => {
    const ids: string[] = [];
    if (this.hint()) {
      ids.push(this.hintId);
    }
    if (this.error()) {
      ids.push(this.errorId);
    }
    return ids.length > 0 ? ids.join(' ') : null;
  });

  readonly invalid = computed(() => this.error() !== '');
}
