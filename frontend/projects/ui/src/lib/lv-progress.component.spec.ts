import { TestBed } from '@angular/core/testing';
import { LvProgressComponent } from './lv-progress.component';

describe('LvProgressComponent', () => {
  async function render(value: number, max: number) {
    const fixture = TestBed.createComponent(LvProgressComponent);
    fixture.componentRef.setInput('value', value);
    fixture.componentRef.setInput('max', max);
    fixture.componentRef.setInput('label', 'Envío gratis');
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('describes itself to screen readers', async () => {
    const el = await render(30, 60);

    expect(el.getAttribute('role')).toBe('progressbar');
    expect(el.getAttribute('aria-valuenow')).toBe('30');
    expect(el.getAttribute('aria-valuemax')).toBe('60');
    expect(el.getAttribute('aria-label')).toBe('Envío gratis');
  });

  it('fills in proportion', async () => {
    const fill = (await render(15, 60)).querySelector<HTMLElement>('.lv-progress__fill');

    expect(fill?.style.width).toBe('25%');
  });

  it('never goes past full or below empty', async () => {
    expect((await render(90, 60)).querySelector<HTMLElement>('.lv-progress__fill')?.style.width).toBe('100%');
    expect((await render(-5, 60)).querySelector<HTMLElement>('.lv-progress__fill')?.style.width).toBe('0%');
  });
});
