import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Emblema de La Viñuela con el nombre al lado. El nombre es texto real (no una imagen) para que se lea bien y
 * lo lean los lectores de pantalla; el emblema es decorativo.
 *
 * La imagen se sirve desde la carpeta `public` de cada aplicación (`logo-emblema.webp`).
 */
@Component({
  selector: 'lv-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'lv-logo' },
  template: `
    <img class="lv-logo__mark" [src]="emblemSrc()" alt="" [attr.width]="size()" [attr.height]="size()" />
    <span class="lv-logo__text">
      <strong>{{ name() }}</strong>
      @if (tagline()) {
        <small>{{ tagline() }}</small>
      }
    </span>
  `,
})
export class LvLogoComponent {
  readonly name = input('La Viñuela');
  readonly tagline = input('');
  readonly emblemSrc = input('logo-emblema.webp');
  /** Lado del emblema en píxeles (mínimo recomendado: 32). */
  readonly size = input(44);
}
