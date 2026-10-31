import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'registro',
    title: 'Crea tu cuenta · La Viñuela',
    loadComponent: () => import('./features/account/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'verificar',
    title: 'Verifica tu correo · La Viñuela',
    loadComponent: () => import('./features/account/verify-email.page').then((m) => m.VerifyEmailPage),
  },
  {
    path: 'componentes',
    title: 'Componentes · La Viñuela',
    loadComponent: () => import('./features/design/components.page').then((m) => m.ComponentsPage),
  },
  // Provisional: hasta que exista la portada, la tienda empieza en el registro.
  { path: '', pathMatch: 'full', redirectTo: 'registro' },
  { path: '**', redirectTo: 'registro' },
];
