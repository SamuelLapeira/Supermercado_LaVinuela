/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { searchProducts } from './catalog-filter';
import { CatalogService } from './catalog.service';
import { ListingPage } from './listing.page';

/** Quita los espacios sin salto que Intl pone antes del símbolo del euro, para comparar textos con tranquilidad. */
function plain(text: string | null | undefined): string {
  return (text ?? '').replace(/\s+/g, ' ').trim();
}

describe('ListingPage', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'categoria/:slug', data: { mode: 'category' }, component: ListingPage },
          { path: 'buscar', data: { mode: 'search' }, component: ListingPage },
          { path: 'ofertas', data: { mode: 'offers' }, component: ListingPage },
        ]),
      ],
    });
  });

  async function open(url: string): Promise<{ el: HTMLElement; harness: RouterTestingHarness }> {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url, ListingPage);
    harness.detectChanges();
    return { el: harness.routeNativeElement as HTMLElement, harness };
  }

  const cards = (el: HTMLElement) => el.querySelectorAll('lv-product-card');
  const names = (el: HTMLElement) => Array.from(el.querySelectorAll('lv-product-card h3')).map((heading) => plain(heading.textContent));

  it('lists the products of a category with their count and subcategories', async () => {
    const { el } = await open('/categoria/lacteos-huevos');

    expect(plain(el.querySelector('h1')?.textContent)).toBe('Lácteos y huevos');
    expect(cards(el).length).toBe(6);
    expect(plain(el.querySelector('.count')?.textContent)).toBe('6 productos');
    expect(plain(el.textContent)).toContain('Yogures y mantequilla (2)');
  });

  it('says so when the category does not exist', async () => {
    const { el } = await open('/categoria/no-existe');

    expect(plain(el.textContent)).toContain('No encontramos esa categoría');
    expect(cards(el).length).toBe(0);
  });

  it('shows the search results with the words that were searched', async () => {
    const { el } = await open('/buscar?q=aceite');
    const expected = searchProducts(TestBed.inject(CatalogService).products, 'aceite').length;

    expect(plain(el.querySelector('h1')?.textContent)).toBe('Resultados para «aceite»');
    expect(cards(el).length).toBe(expected);
    expect(expected).toBeGreaterThan(2);
  });

  it('suggests what to do when the search finds nothing', async () => {
    const { el } = await open('/buscar?q=xyzzy');

    expect(plain(el.textContent)).toContain('No hay productos con esos criterios');
    expect(cards(el).length).toBe(0);
  });

  it('lists only the offers on the offers page', async () => {
    const { el } = await open('/ofertas');

    expect(plain(el.querySelector('h1')?.textContent)).toBe('Ofertas de la semana');
    expect(cards(el).length).toBe(TestBed.inject(CatalogService).offers().length);
  });

  it('filters by subcategory from the address', async () => {
    expect(cards((await open('/categoria/lacteos-huevos?sub=leche')).el).length).toBe(2);
  });

  it('filters by brand and tag from the address', async () => {
    expect(cards((await open('/categoria/lacteos-huevos?marcas=La%20Viñuela&etiquetas=sin-lactosa')).el).length).toBe(1);
  });

  it('ignores filters that do not exist instead of breaking', async () => {
    const { el } = await open('/categoria/lacteos-huevos?etiquetas=inventada&orden=raro');

    expect(cards(el).length).toBe(6);
  });

  it('sorts by price from the address', async () => {
    const { el } = await open('/categoria/lacteos-huevos?orden=precio-desc');

    expect(names(el)[0]).toBe('Queso de cabra curado');
  });

  it('puts what is sold out at the end by default', async () => {
    const { el } = await open('/categoria/pescados-mariscos');
    const all = names(el);

    expect(all[all.length - 1]).toBe('Merluza en rodajas');
  });

  it('changes the address when a filter is chosen, so it can be shared and the back button works', async () => {
    const { el, harness } = await open('/categoria/lacteos-huevos');

    el.querySelector<HTMLInputElement>('fieldset input[type="checkbox"]')?.click();
    await harness.fixture.whenStable();

    expect(TestBed.inject(Router).url).toMatch(/etiquetas=/);
  });

  it('offers to clear the filters when there are active ones', async () => {
    const { el } = await open('/categoria/lacteos-huevos?sub=leche');

    expect(plain(el.textContent)).toContain('Quitar los filtros');
    expect(plain(el.querySelector('.filters-toggle')?.textContent)).toContain('(1)');
  });
});
