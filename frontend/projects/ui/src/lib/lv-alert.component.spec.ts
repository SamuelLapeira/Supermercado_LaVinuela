import { TestBed } from '@angular/core/testing';
import { LvAlertComponent } from './lv-alert.component';

describe('LvAlertComponent', () => {
  async function render(variant: 'ok' | 'warn' | 'danger' | 'info'): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(LvAlertComponent);
    fixture.componentRef.setInput('variant', variant);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('announces errors immediately (role alert)', async () => {
    const element = await render('danger');
    expect(element.getAttribute('role')).toBe('alert');
    expect(element.classList.contains('lv-alert--danger')).toBe(true);
  });

  it('announces the rest politely (role status)', async () => {
    for (const variant of ['ok', 'warn', 'info'] as const) {
      const element = await render(variant);
      expect(element.getAttribute('role')).toBe('status');
    }
  });

  it('always shows an icon next to the colour', async () => {
    const element = await render('ok');
    expect(element.querySelector('svg')).not.toBeNull();
  });
});
