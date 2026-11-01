import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LvLogoComponent } from 'ui';

/** Pie de la tienda: dónde estamos, cuándo abrimos, ayuda y avisos. Los textos legales definitivos llegarán con la historia E14-03. */
@Component({
  selector: 'lv-site-footer',
  imports: [RouterLink, LvLogoComponent],
  templateUrl: './site-footer.html',
  styleUrl: './site-footer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SiteFooter {}
