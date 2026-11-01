import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessionService } from 'core';
import { AccountPage } from './account.page';

describe('AccountPage', () => {
  const user = signal({ id: '01X', username: 'ana', email: 'ana@example.com', roles: ['CUSTOMER'] });
  const session = { user, logout: vi.fn(async () => undefined) };

  beforeEach(async () => {
    session.logout.mockClear();
    await TestBed.configureTestingModule({
      imports: [AccountPage],
      providers: [provideRouter([]), { provide: SessionService, useValue: session }],
    }).compileComponents();
  });

  it('greets the person and shows which account is open', async () => {
    const fixture = TestBed.createComponent(AccountPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent;

    expect(text).toContain('Hola, ana');
    expect(text).toContain('ana@example.com');
  });

  it('logs out and goes to the login', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(AccountPage);
    await fixture.whenStable();

    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => b.textContent?.includes('Cerrar sesión'))?.click();
    await new Promise<void>((resolve) => setTimeout(resolve));

    expect(session.logout).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });
});
