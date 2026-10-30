import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LvAlertComponent, LvLogoComponent } from 'ui';

/** Pantalla provisional del panel de gestión, hasta que lleguen los pedidos, los productos y los ajustes. */
@Component({
  selector: 'lv-home-page',
  imports: [LvAlertComponent, LvLogoComponent],
  templateUrl: './home.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {}
