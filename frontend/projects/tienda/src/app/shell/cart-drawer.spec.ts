/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartStore } from '../features/cart/cart.store';
import { CartDrawer } from './cart-drawer';

/** Quita los espacios sin salto que Intl pone antes del símbolo del euro, para comparar textos con tranquilidad. */
function plain(text: string | null | undefined): string {
  return (text ?? '').replace(/\s+/g, ' ').trim();
}

describe('CartDrawer', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  async function render(): Promise<{ el: HTMLElement; dialog: HTMLDialogElement; fixture: ReturnType<typeof TestBed.createComponent<CartDrawer>> }> {
    const fixture = TestBed.createComponent(CartDrawer);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { el, dialog: el.querySelector('dialog') as HTMLDialogElement, fixture };
  }

  it('stays closed until the cart is opened, and closes again', async () => {
    const cart = TestBed.inject(CartStore);
    const { dialog, fixture } = await render();
    expect(dialog.hasAttribute('open')).toBe(false);

    cart.openDrawer();
    await fixture.whenStable();
    expect(dialog.hasAttribute('open')).toBe(true);

    cart.closeDrawer();
    await fixture.whenStable();
    expect(dialog.hasAttribute('open')).toBe(false);
  });

  it('shows an empty cart with a way to start shopping', async () => {
    const { el } = await render();

    expect(plain(el.textContent)).toContain('Tu cesta está vacía');
  });

  it('lists the products, the subtotal and how much is missing for free shipping', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-1l', 2);
    const { el } = await render();
    const text = plain(el.textContent);

    expect(text).toContain('Aceite de oliva virgen extra');
    expect(text).toContain('17,90 €');
    expect(text).toContain('Te faltan 42,10 € para el envío gratis');
    expect(el.querySelector('lv-progress')?.getAttribute('aria-valuenow')).toBe('17.9');
  });

  it('changes the units from the panel', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('leche-entera-1l', 1);
    const { el, fixture } = await render();

    el.querySelector<HTMLButtonElement>('button[aria-label^="Añadir una unidad"]')?.click();
    await fixture.whenStable();

    expect(cart.quantityOf('leche-entera-1l')).toBe(2);
  });

  it('closes with its own button', async () => {
    const cart = TestBed.inject(CartStore);
    cart.openDrawer();
    const { el, fixture } = await render();

    el.querySelector<HTMLButtonElement>('button[aria-label="Cerrar la cesta"]')?.click();
    await fixture.whenStable();

    expect(cart.drawerOpen()).toBe(false);
  });
});
