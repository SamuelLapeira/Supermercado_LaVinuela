import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'componentes',
    title: 'Componentes · La Viñuela',
    loadComponent: () => import('./features/design/components.page').then((m) => m.ComponentsPage),
  },
  // Provisional: hasta que exista la portada, la tienda enseña la página de componentes.
  { path: '', pathMatch: 'full', redirectTo: 'componentes' },
  { path: '**', redirectTo: 'componentes' },
];
