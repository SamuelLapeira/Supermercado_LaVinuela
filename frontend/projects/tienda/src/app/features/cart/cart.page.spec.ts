/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartPage } from './cart.page';
import { CartStore } from './cart.store';

/** Quita los espacios sin salto que Intl pone antes del símbolo del euro, para comparar textos con tranquilidad. */
function plain(text: string | null | undefined): string {
  return (text ?? '').replace(/\s+/g, ' ').trim();
}

describe('CartPage', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  async function render(): Promise<{ el: HTMLElement; fixture: ReturnType<typeof TestBed.createComponent<CartPage>> }> {
    const fixture = TestBed.createComponent(CartPage);
    await fixture.whenStable();
    return { el: fixture.nativeElement as HTMLElement, fixture };
  }

  const checkout = (el: HTMLElement) => Array.from(el.querySelectorAll<HTMLElement>('a, button')).find((entry) => plain(entry.textContent) === 'Tramitar pedido');

  it('invites to shop when the cart is empty', async () => {
    const { el } = await render();

    expect(plain(el.textContent)).toContain('Todavía no has añadido nada');
    expect(checkout(el)).toBeUndefined();
  });

  it('lists the lines with their amounts and the subtotal', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-1l', 2);
    cart.add('leche-entera-1l', 6);
    const { el } = await render();
    const text = plain(el.textContent);

    expect(el.querySelectorAll('.line').length).toBe(2);
    expect(text).toContain('17,90 €');
    expect(text).toContain('5,70 €');
    expect(text).toContain('Subtotal (8 productos)');
    expect(text).toContain('23,60 €');
  });

  it('asks where to deliver and then adds the shipping to the total', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-5l', 1);
    const { el, fixture } = await render();
    expect(plain(el.textContent)).toContain('Elige dónde');

    const zone = el.querySelector<HTMLSelectElement>('select#zona');
    if (!zone) {
      throw new Error('No existe el selector de localidad');
    }
    zone.value = 'competa';
    zone.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    const text = plain(el.textContent);
    expect(text).toContain('4,50 €');
    expect(text).toContain('44,40 €');
  });

  it('says that shipping is free when the amount is reached', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-5l', 2);
    cart.selectZone('competa');
    const { el } = await render();

    expect(plain(el.textContent)).toContain('Gratis');
    expect(plain(el.textContent)).toContain('¡Tienes el envío gratis!');
  });

  it('warns about the minimum order and does not let the person continue', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('leche-entera-1l', 1);
    cart.selectZone('competa');
    const { el } = await render();

    expect(plain(el.textContent)).toContain('El pedido mínimo es de 20,00 €');
    expect(checkout(el)?.hasAttribute('disabled')).toBe(true);
  });

  it('does not let the person continue outside the delivery area, and says why', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-5l', 1);
    cart.selectZone('malaga');
    const { el } = await render();

    expect(plain(el.textContent)).toContain('Todavía no repartimos en Málaga capital');
    expect(checkout(el)?.hasAttribute('disabled')).toBe(true);
  });

  it('lets the person continue when everything is in order', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-5l', 1);
    cart.selectZone('competa');
    const { el } = await render();

    expect(checkout(el)?.getAttribute('href')).toBe('/pedido');
  });

  it('warns that the cart has products for adults only', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('cerveza-rubia-6', 4);
    const { el } = await render();

    expect(plain(el.textContent)).toContain('confirmar tu edad');
  });

  it('removes a line', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-1l');
    cart.add('leche-entera-1l');
    const { el, fixture } = await render();

    el.querySelector<HTMLButtonElement>('.line .remove')?.click();
    await fixture.whenStable();

    expect(cart.items().length).toBe(1);
    expect(el.querySelectorAll('.line').length).toBe(1);
  });
});
