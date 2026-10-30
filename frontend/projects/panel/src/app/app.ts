import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Estructura del panel de gestión: enlace «Saltar al contenido» y la página activa. El menú lateral llegará con las primeras pantallas. */
@Component({
  selector: 'lv-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
