import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessionService } from 'core';
import { LvButtonDirective, LvLogoComponent } from 'ui';
import { CartStore } from '../features/cart/cart.store';
import { CatalogService } from '../features/catalog/catalog.service';

/**
 * Cabecera de la tienda: aviso de demostración, logotipo, buscador, cuenta y cesta, y debajo las categorías. En el móvil el
 * buscador pasa a una segunda fila y las categorías se desplazan hacia los lados.
 */
@Component({
  selector: 'lv-site-header',
  imports: [ReactiveFormsModule, RouterLink, RouterLinkActive, LvButtonDirective, LvLogoComponent],
  templateUrl: './site-header.html',
  styleUrl: './site-header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SiteHeader {
  private readonly router = inject(Router);
  protected readonly session = inject(SessionService);
  protected readonly cart = inject(CartStore);
  protected readonly categories = inject(CatalogService).categories;
  protected readonly leaving = signal(false);

  protected readonly query = new FormControl('', { nonNullable: true });

  protected async search(): Promise<void> {
    const q = this.query.value.trim();
    if (q.length > 0) {
      await this.router.navigate(['/buscar'], { queryParams: { q } });
    }
  }

  protected async logout(): Promise<void> {
    this.leaving.set(true);
    try {
      await this.session.logout();
      await this.router.navigateByUrl('/entrar');
    } finally {
      this.leaving.set(false);
    }
  }
}
