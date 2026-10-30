import { TestBed } from '@angular/core/testing';
import { LvLogoComponent } from './lv-logo.component';

describe('LvLogoComponent', () => {
  it('shows the name as text and hides the decorative emblem from screen readers', async () => {
    const fixture = TestBed.createComponent(LvLogoComponent);
    fixture.componentRef.setInput('tagline', 'Supermercado de la sierra');
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('strong')?.textContent).toBe('La Viñuela');
    expect(element.querySelector('small')?.textContent).toBe('Supermercado de la sierra');
    expect(element.querySelector('img')?.getAttribute('alt')).toBe('');
  });

  it('omits the tagline when there is none', async () => {
    const fixture = TestBed.createComponent(LvLogoComponent);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('small')).toBeNull();
  });
});
