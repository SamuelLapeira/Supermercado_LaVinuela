import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LvLogoComponent } from 'ui';

/** Marco común de las pantallas de cuenta: el logotipo arriba y el contenido en una tarjeta centrada. */
@Component({
  selector: 'lv-auth-layout',
  imports: [LvLogoComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="lv-container wrap">
      <a routerLink="/" class="brand" aria-label="La Viñuela, ir al inicio">
        <lv-logo tagline="Supermercado de la sierra" />
      </a>
      <section class="lv-card card">
        <div class="body lv-stack"><ng-content /></div>
      </section>
    </div>
  `,
  styles: `
    .wrap {
      display: grid;
      gap: var(--space-5);
      justify-items: center;
      padding-block: var(--space-6) var(--space-9);
    }
    .brand {
      text-decoration: none;
    }
    .card {
      width: 100%;
      max-width: 30rem;
    }
    .body {
      padding: var(--space-5);
    }
    @media (min-width: 40rem) {
      .body {
        padding: var(--space-6);
      }
    }
  `,
})
export class AuthLayout {}
