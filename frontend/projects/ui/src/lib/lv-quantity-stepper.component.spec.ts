import { TestBed } from '@angular/core/testing';
import { LvQuantityStepperComponent } from './lv-quantity-stepper.component';

describe('LvQuantityStepperComponent', () => {
  async function render(inputs: Record<string, unknown> = {}) {
    const fixture = TestBed.createComponent(LvQuantityStepperComponent);
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    await fixture.whenStable();
    const host = fixture.nativeElement as HTMLElement;
    const [minus, plus] = Array.from(host.querySelectorAll('button')) as HTMLButtonElement[];
    return { fixture, host, minus, plus };
  }

  it('adds and removes units', async () => {
    const { fixture, plus, minus } = await render({ value: 2 });
    plus?.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe(3);

    minus?.click();
    minus?.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe(1);
  });

  it('never goes beyond the maximum', async () => {
    const { fixture, plus } = await render({ value: 3, max: 3 });
    expect(plus?.disabled).toBe(true);
    plus?.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe(3);
  });

  it('never goes below the minimum', async () => {
    const { minus } = await render({ value: 0, min: 0 });
    expect(minus?.disabled).toBe(true);
  });

  it('gives the buttons accessible names that include the product', async () => {
    const { host, minus, plus } = await render({ itemName: 'Leche entera' });
    expect(plus?.getAttribute('aria-label')).toBe('Añadir una unidad de Leche entera');
    expect(minus?.getAttribute('aria-label')).toBe('Quitar una unidad de Leche entera');
    expect(host.getAttribute('aria-label')).toBe('Cantidad de Leche entera');
  });

  it('shows the current value in a status region', async () => {
    const { host } = await render({ value: 5 });
    expect(host.querySelector('output')?.textContent?.trim()).toBe('5');
  });
});
