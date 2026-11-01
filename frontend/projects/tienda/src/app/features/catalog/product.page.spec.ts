/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CartStore } from '../cart/cart.store';
import { ProductPage } from './product.page';

/** Quita los espacios sin salto que Intl pone antes del símbolo del euro, para comparar textos con tranquilidad. */
function plain(text: string | null | undefined): string {
  return (text ?? '').replace(/\s+/g, ' ').trim();
}

describe('ProductPage', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'producto/:slug', component: ProductPage }])] });
  });

  async function open(slug: string): Promise<{ el: HTMLElement; harness: RouterTestingHarness }> {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(`/producto/${slug}`, ProductPage);
    harness.detectChanges();
    return { el: harness.routeNativeElement as HTMLElement, harness };
  }

  /** El botón principal de la ficha (no los de las tarjetas de productos parecidos, que también dicen «Añadir»). */
  const addButton = (el: HTMLElement) => el.querySelector<HTMLButtonElement>('.info .buy button.lv-btn');

  it('shows the product with its price, offer and unit price', async () => {
    const { el } = await open('aove-1l');
    const text = plain(el.textContent);

    expect(plain(el.querySelector('h1')?.textContent)).toBe('Aceite de oliva virgen extra');
    expect(text).toContain('8,95 €');
    expect(text).toContain('10,20 €');
    expect(text).toContain('−12 %');
    expect(text).toContain('Imagen de ejemplo');
  });

  it('shows the nutrition table when there is information', async () => {
    expect((await open('aove-1l')).el.querySelector('table.nutrition')).not.toBeNull();
  });

  it('does not show the nutrition table when there is no information, but shows the allergens', async () => {
    const { el } = await open('gambas-peladas-400g');

    expect(el.querySelector('table.nutrition')).toBeNull();
    expect(plain(el.textContent)).toContain('Crustáceos');
  });

  it('lets the person choose the quantity and says how many will be added', async () => {
    const { el, harness } = await open('aove-1l');
    expect(plain(addButton(el)?.textContent)).toBe('Añadir 1 unidad a la cesta');

    el.querySelector<HTMLButtonElement>('button[aria-label^="Añadir una unidad"]')?.click();
    await harness.fixture.whenStable();

    expect(plain(addButton(el)?.textContent)).toBe('Añadir 2 unidades a la cesta');
  });

  it('adds the chosen units to the cart and opens the cart panel', async () => {
    const { el, harness } = await open('aove-1l');
    const cart = TestBed.inject(CartStore);
    el.querySelector<HTMLButtonElement>('button[aria-label^="Añadir una unidad"]')?.click();
    await harness.fixture.whenStable();

    addButton(el)?.click();
    await harness.fixture.whenStable();

    expect(cart.quantityOf('aove-1l')).toBe(2);
    expect(cart.drawerOpen()).toBe(true);
    expect(plain(el.textContent)).toContain('Ya tienes 2 en la cesta');
  });

  it('explains that sold out products cannot be bought', async () => {
    const { el } = await open('merluza-rodajas-600g');

    expect(plain(el.textContent)).toContain('agotado');
    expect(addButton(el)).toBeNull();
  });

  it('warns that alcohol is only for adults', async () => {
    expect(plain((await open('cerveza-rubia-6')).el.textContent)).toContain('mayores de 18 años');
  });

  it('suggests other products of the same category', async () => {
    const { el } = await open('aove-1l');

    expect(el.querySelectorAll('.related lv-product-card').length).toBe(4);
  });

  it('says so when the product does not exist', async () => {
    const { el } = await open('no-existe');

    expect(plain(el.textContent)).toContain('No encontramos ese producto');
  });
});
