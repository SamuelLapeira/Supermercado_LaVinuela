import { Routes } from '@angular/router';
import { authGuard } from 'core';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'La Viñuela · Supermercado de la sierra',
    loadComponent: () => import('./features/catalog/home.page').then((m) => m.HomePage),
  },
  {
    path: 'categoria/:slug',
    data: { mode: 'category' },
    loadComponent: () => import('./features/catalog/listing.page').then((m) => m.ListingPage),
  },
  {
    path: 'buscar',
    data: { mode: 'search' },
    loadComponent: () => import('./features/catalog/listing.page').then((m) => m.ListingPage),
  },
  {
    path: 'ofertas',
    data: { mode: 'offers' },
    loadComponent: () => import('./features/catalog/listing.page').then((m) => m.ListingPage),
  },
  {
    path: 'producto/:slug',
    loadComponent: () => import('./features/catalog/product.page').then((m) => m.ProductPage),
  },
  {
    path: 'cesta',
    title: 'Tu cesta · La Viñuela',
    loadComponent: () => import('./features/cart/cart.page').then((m) => m.CartPage),
  },
  {
    path: 'pedido',
    title: 'Tramitar pedido · La Viñuela',
    loadComponent: () => import('./features/cart/checkout-soon.page').then((m) => m.CheckoutSoonPage),
  },
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
    path: 'entrar',
    title: 'Entra en tu cuenta · La Viñuela',
    loadComponent: () => import('./features/account/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'cuenta',
    title: 'Mi cuenta · La Viñuela',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/account.page').then((m) => m.AccountPage),
  },
  {
    path: 'componentes',
    title: 'Componentes · La Viñuela',
    loadComponent: () => import('./features/design/components.page').then((m) => m.ComponentsPage),
  },
  { path: '**', redirectTo: '' },
];
