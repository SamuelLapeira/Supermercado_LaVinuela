import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LvButtonDirective } from './lv-button.directive';

@Component({
  imports: [LvButtonDirective],
  template: `
    <button lvButton id="plain">Añadir</button>
    <button lvButton="accent" lvSize="large" lvBlock id="accent">Ofertas</button>
    <a lvButton="ghost" href="#" id="link">Enlace</a>
  `,
})
class HostComponent {}

describe('LvButtonDirective', () => {
  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('uses the primary variant by default', async () => {
    const host = await render();
    expect(host.querySelector('#plain')?.className).toBe('lv-btn lv-btn--primary');
  });

  it('applies variant, size and block classes', async () => {
    const host = await render();
    const classes = host.querySelector('#accent')?.classList;
    expect(classes?.contains('lv-btn--accent')).toBe(true);
    expect(classes?.contains('lv-btn--large')).toBe(true);
    expect(classes?.contains('lv-btn--block')).toBe(true);
  });

  it('also works on links', async () => {
    const host = await render();
    expect(host.querySelector('#link')?.classList.contains('lv-btn--ghost')).toBe(true);
  });
});
