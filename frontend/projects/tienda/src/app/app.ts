import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Estructura de la tienda: enlace «Saltar al contenido» y la página activa. La cabecera y el pie llegarán con el catálogo. */
@Component({
  selector: 'lv-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
