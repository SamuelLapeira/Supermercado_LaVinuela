import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LvControlDirective } from './lv-control.directive';
import { LvFieldComponent } from './lv-field.component';

@Component({
  imports: [LvFieldComponent, LvControlDirective],
  template: `
    <lv-field label="Código postal" hint="Cinco cifras" [error]="error()" required>
      <input lvControl />
    </lv-field>
  `,
})
class HostComponent {
  readonly error = signal('');
}

describe('LvFieldComponent', () => {
  async function render() {
    const fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
    const host = fixture.nativeElement as HTMLElement;
    return { fixture, host, input: host.querySelector('input') as HTMLInputElement };
  }

  it('links the visible label to the control', async () => {
    const { host, input } = await render();
    const label = host.querySelector('label');
    expect(input.id).not.toBe('');
    expect(label?.getAttribute('for')).toBe(input.id);
    expect(label?.textContent).toContain('Código postal');
  });

  it('describes the control with the hint', async () => {
    const { host, input } = await render();
    const hint = host.querySelector('.lv-field__hint');
    expect(hint?.id).toBeTruthy();
    expect(input.getAttribute('aria-describedby')).toBe(hint?.id);
  });

  it('marks required controls', async () => {
    const { input } = await render();
    expect(input.getAttribute('aria-required')).toBe('true');
  });

  it('is not invalid when there is no error', async () => {
    const { input } = await render();
    expect(input.getAttribute('aria-invalid')).toBeNull();
  });

  it('flags the control as invalid and announces the error', async () => {
    const { fixture, host, input } = await render();
    fixture.componentInstance.error.set('Introduce un código postal de 5 cifras.');
    await fixture.whenStable();

    const error = host.querySelector('.lv-field__error');
    expect(error?.getAttribute('role')).toBe('alert');
    expect(error?.textContent).toContain('5 cifras');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toContain(error?.id ?? 'sin-error');
  });
});
