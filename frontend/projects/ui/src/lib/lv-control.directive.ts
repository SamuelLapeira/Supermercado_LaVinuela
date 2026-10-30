import { Directive, inject } from '@angular/core';
import { LvFieldComponent } from './lv-field.component';

/**
 * Control de formulario (`input`, `textarea` o `select`) de La Viñuela.
 *
 * Dentro de un `<lv-field>` recibe automáticamente su `id` (para que la etiqueta apunte a él),
 * `aria-describedby`, `aria-invalid` y `aria-required`. Fuera de un campo, solo aplica el estilo.
 */
@Directive({
  selector: 'input[lvControl], textarea[lvControl], select[lvControl]',
  host: {
    class: 'lv-control',
    '[attr.id]': 'field?.controlId ?? null',
    '[attr.aria-describedby]': 'field?.describedBy() ?? null',
    '[attr.aria-invalid]': "field?.invalid() ? 'true' : null",
    '[attr.aria-required]': "field?.required() ? 'true' : null",
  },
})
export class LvControlDirective {
  readonly field = inject(LvFieldComponent, { optional: true });
}
