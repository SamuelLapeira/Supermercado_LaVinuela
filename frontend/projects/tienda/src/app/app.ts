import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CartDrawer } from './shell/cart-drawer';
import { SiteFooter } from './shell/site-footer';
import { SiteHeader } from './shell/site-header';

/** Estructura de la tienda: enlace «Saltar al contenido», cabecera, página activa, pie y el panel de la cesta. */
@Component({
  selector: 'lv-root',
  imports: [RouterOutlet, SiteHeader, SiteFooter, CartDrawer],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
