import { TestBed } from '@angular/core/testing';
import { LvBadgeComponent } from './lv-badge.component';

describe('LvBadgeComponent', () => {
  it('uses the neutral variant by default and accepts others', async () => {
    const fixture = TestBed.createComponent(LvBadgeComponent);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.className).toBe('lv-badge lv-badge--neutral');

    fixture.componentRef.setInput('variant', 'offer');
    await fixture.whenStable();
    expect(element.className).toBe('lv-badge lv-badge--offer');
  });
});
