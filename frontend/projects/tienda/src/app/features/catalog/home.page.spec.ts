/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CatalogService } from './catalog.service';
import { HomePage } from './home.page';

describe('HomePage', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(HomePage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('says what the shop is and offers the first steps', async () => {
    const el = await render();

    expect(el.querySelector('h1')?.textContent).toContain('Tu supermercado de la sierra');
    expect(el.textContent).toContain('Ver las ofertas');
  });

  it('shows every category with a link to its listing', async () => {
    const el = await render();
    const catalog = TestBed.inject(CatalogService);
    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('a.cat')).map((link) => link.getAttribute('href'));

    expect(links).toEqual(catalog.categories.map((category) => `/categoria/${category.slug}`));
  });

  it('shows the offers and the local products as product cards', async () => {
    const el = await render();
    const catalog = TestBed.inject(CatalogService);
    const sections = el.querySelectorAll('section[aria-labelledby="home-ofertas"] lv-product-card, section[aria-labelledby="home-local"] lv-product-card');

    expect(sections.length).toBe(catalog.offers().slice(0, 8).length + catalog.local().slice(0, 8).length);
  });

  it('shows where the physical shop is and when it opens', async () => {
    const text = (await render()).textContent ?? '';

    expect(text).toContain('C. Pizarras, 34');
    expect(text).toContain('9:00 a 21:00');
  });
});
