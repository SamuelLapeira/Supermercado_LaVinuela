import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LvButtonDirective } from 'ui';
import { CatalogService } from './catalog.service';
import { ProductCard } from './product-card';

/** Portada de la tienda: propuesta de valor, categorías, ofertas, producto local y cómo funciona el reparto. */
@Component({
  selector: 'lv-home-page',
  imports: [RouterLink, LvButtonDirective, ProductCard],
  templateUrl: './home.page.html',
  styleUrl: './home.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {
  private readonly catalog = inject(CatalogService);

  protected readonly categories = this.catalog.categories.map((category) => ({
    ...category,
    count: this.catalog.byCategory(category.slug).length,
  }));
  protected readonly offers = this.catalog.offers().slice(0, 8);
  protected readonly local = this.catalog.local().slice(0, 8);
}
