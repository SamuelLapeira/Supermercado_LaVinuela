import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import {
  LvAlertComponent,
  LvBadgeComponent,
  LvButtonDirective,
  LvControlDirective,
  LvFieldComponent,
  LvLogoComponent,
  LvQuantityStepperComponent,
} from 'ui';

/**
 * Página interna con todos los componentes del sistema de diseño y sus estados. Sirve para revisarlos a ojo y con
 * teclado, y como referencia al construir pantallas. No forma parte de la tienda pública definitiva.
 */
@Component({
  selector: 'lv-components-page',
  imports: [
    LvAlertComponent,
    LvBadgeComponent,
    LvButtonDirective,
    LvControlDirective,
    LvFieldComponent,
    LvLogoComponent,
    LvQuantityStepperComponent,
  ],
  templateUrl: './components.page.html',
  styleUrl: './components.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComponentsPage {
  protected readonly quantity = signal(1);
  protected readonly categories = [
    { id: 1, name: 'Frutas y verduras' },
    { id: 2, name: 'Carne y pescado' },
    { id: 3, name: 'Lácteos y huevos' },
    { id: 4, name: 'Despensa' },
    { id: 5, name: 'Bebidas' },
    { id: 6, name: 'Hogar y limpieza' },
  ];
}
