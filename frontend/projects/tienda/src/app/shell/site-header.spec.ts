/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessionService } from 'core';
import { CartStore } from '../features/cart/cart.store';
import { SiteHeader } from './site-header';

/** Quita los espacios sin salto que Intl pone antes del símbolo del euro, para comparar textos con tranquilidad. */
function plain(text: string | null | undefined): string {
  return (text ?? '').replace(/\s+/g, ' ').trim();
}

describe('SiteHeader', () => {
  const status = signal<'unknown' | 'anonymous' | 'authenticated'>('anonymous');
  const user = signal<{ id: string; username: string; email: string; roles: string[] } | null>(null);
  const session = { status, user, logout: vi.fn(async () => undefined) };

  beforeEach(async () => {
    localStorage.clear();
    session.logout.mockClear();
    await TestBed.configureTestingModule({
      imports: [SiteHeader],
      providers: [provideRouter([]), { provide: SessionService, useValue: session }],
    }).compileComponents();
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(SiteHeader);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('offers to log in to someone who has not entered', async () => {
    status.set('anonymous');
    user.set(null);

    const el = await render();

    expect(el.querySelector('a.login')?.getAttribute('aria-label')).toBe('Entrar en tu cuenta');
    expect(plain(el.textContent)).not.toContain('Salir');
  });

  it('shows who has entered and a way out', async () => {
    status.set('authenticated');
    user.set({ id: '01X', username: 'ana', email: 'ana@example.com', roles: ['CUSTOMER'] });

    const text = plain((await render()).textContent);

    expect(text).toContain('Hola, ana');
    expect(text).toContain('Salir');
  });

  it('shows only the cart while it is still finding out whether there is a session (no flicker)', async () => {
    status.set('unknown');
    user.set(null);

    const el = await render();

    expect(el.querySelectorAll('.actions a, .actions button').length).toBe(1);
    expect(el.querySelector('.actions .cart')).not.toBeNull();
  });

  it('logs out and goes to the login', async () => {
    status.set('authenticated');
    user.set({ id: '01X', username: 'ana', email: 'ana@example.com', roles: ['CUSTOMER'] });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const el = await render();

    Array.from(el.querySelectorAll('button')).find((button) => plain(button.textContent) === 'Salir')?.click();
    await new Promise<void>((resolve) => setTimeout(resolve));

    expect(session.logout).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  it('searches by going to the results page', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const el = await render();
    const input = el.querySelector<HTMLInputElement>('input[type="search"]');
    if (!input) {
      throw new Error('No existe el buscador');
    }

    input.value = '  aceite ';
    input.dispatchEvent(new Event('input'));
    el.querySelector('form')?.dispatchEvent(new Event('submit'));
    await new Promise<void>((resolve) => setTimeout(resolve));

    expect(navigate).toHaveBeenCalledWith(['/buscar'], { queryParams: { q: 'aceite' } });
  });

  it('does nothing when the search is empty', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const el = await render();

    el.querySelector('form')?.dispatchEvent(new Event('submit'));
    await new Promise<void>((resolve) => setTimeout(resolve));

    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows how many products are in the cart and opens the cart panel', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add('aove-1l', 3);
    const el = await render();
    const button = el.querySelector<HTMLButtonElement>('button.cart');

    expect(button?.getAttribute('aria-label')).toBe('Cesta: 3 productos');
    expect(plain(button?.textContent)).toContain('3');

    button?.click();
    expect(cart.drawerOpen()).toBe(true);
  });

  it('links to the offers and to every category', async () => {
    const el = await render();
    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('nav.cats a')).map((link) => link.getAttribute('href'));

    expect(links[0]).toBe('/ofertas');
    expect(links.length).toBe(9);
    expect(links).toContain('/categoria/bebidas');
  });
});
