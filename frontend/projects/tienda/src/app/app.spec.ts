/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessionService } from 'core';
import { App } from './app';

describe('App', () => {
  const session = { status: signal('anonymous'), user: signal(null), logout: vi.fn() };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: SessionService, useValue: session }],
    }).compileComponents();
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('should create the app', () => {
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });

  it('offers a skip link to the main content', async () => {
    expect((await render()).querySelector('a.lv-skip-link')?.getAttribute('href')).toBe('#contenido');
  });

  it('has the header, the footer and the cart panel around the page', async () => {
    const el = await render();

    expect(el.querySelector('lv-site-header')).not.toBeNull();
    expect(el.querySelector('lv-site-footer')).not.toBeNull();
    expect(el.querySelector('lv-cart-drawer')).not.toBeNull();
    expect(el.querySelector('main#contenido')).not.toBeNull();
  });
});
