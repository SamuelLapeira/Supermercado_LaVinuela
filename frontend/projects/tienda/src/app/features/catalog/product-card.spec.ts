/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartStore } from '../cart/cart.store';
import { PRODUCTS } from './catalog.data';
import { ProductCard } from './product-card';

/** Quita los espacios sin salto que Intl pone antes del símbolo del euro, para comparar textos con tranquilidad. */
function plain(text: string | null | undefined): string {
  return (text ?? '').replace(/\s+/g, ' ').trim();
}

describe('ProductCard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  async function render(slug: string): Promise<{ el: HTMLElement; fixture: ReturnType<typeof TestBed.createComponent<ProductCard>> }> {
    const product = PRODUCTS.find((entry) => entry.slug === slug);
    if (!product) {
      throw new Error(`No existe el producto ${slug}`);
    }
    const fixture = TestBed.createComponent(ProductCard);
    fixture.componentRef.setInput('product', product);
    await fixture.whenStable();
    return { el: fixture.nativeElement as HTMLElement, fixture };
  }

  it('shows the brand, name, format, price and price per unit', async () => {
    const { el } = await render('aove-1l');
    const text = plain(el.textContent);

    expect(text).toContain('Cooperativa Olivarera de la Axarquía');
    expect(text).toContain('Aceite de oliva virgen extra');
    expect(text).toContain('botella 1 L');
    expect(text).toContain('8,95 €');
    expect(text).toContain('8,95 €/L');
  });

  it('shows the discount and the previous price of offers', async () => {
    const { el } = await render('aove-1l');
    const text = plain(el.textContent);

    expect(text).toContain('−12 %');
    expect(text).toContain('10,20 €');
  });

  it('marks local products and products for adults only', async () => {
    expect(plain((await render('aove-1l')).el.textContent)).toContain('De la sierra');
    expect(plain((await render('cerveza-rubia-6')).el.textContent)).toContain('+18');
  });

  it('adds to the cart and then shows the quantity selector instead of the add button', async () => {
    const { el, fixture } = await render('leche-entera-1l');
    const cart = TestBed.inject(CartStore);

    el.querySelector<HTMLButtonElement>('button')?.click();
    await fixture.whenStable();

    expect(cart.quantityOf('leche-entera-1l')).toBe(1);
    expect(el.querySelector('lv-quantity-stepper')).not.toBeNull();
    expect(plain(el.textContent)).not.toContain('Añadir a la cesta');
  });

  it('goes back to the add button when the last unit is removed', async () => {
    const { el, fixture } = await render('leche-entera-1l');
    TestBed.inject(CartStore).add('leche-entera-1l');
    await fixture.whenStable();

    el.querySelector<HTMLButtonElement>('button[aria-label^="Quitar una unidad"]')?.click();
    await fixture.whenStable();

    expect(TestBed.inject(CartStore).quantityOf('leche-entera-1l')).toBe(0);
    expect(el.querySelector('lv-quantity-stepper')).toBeNull();
  });

  it('shows sold out products without a way to buy them', async () => {
    const { el } = await render('merluza-rodajas-600g');

    expect(plain(el.textContent)).toContain('Agotado');
    expect(el.querySelector('button')).toBeNull();
  });

  it('warns when only a few units are left', async () => {
    expect(plain((await render('lechuga-romana')).el.textContent)).toContain('Quedan 3');
  });

  it('links to the product page', async () => {
    const { el } = await render('aove-1l');

    expect(el.querySelector('a.name, h3 a')?.getAttribute('href')).toBe('/producto/aove-1l');
  });
});
