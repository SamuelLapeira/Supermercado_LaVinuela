import { TestBed } from '@angular/core/testing';
import { HomePage } from './home.page';

describe('HomePage', () => {
  it('shows the panel title', async () => {
    const fixture = TestBed.createComponent(HomePage);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Panel de gestión');
  });
});
