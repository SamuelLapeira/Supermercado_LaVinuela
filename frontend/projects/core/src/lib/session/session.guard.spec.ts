import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import type { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './session.guard';
import { SessionService } from './session.service';

describe('authGuard', () => {
  const status = signal<'unknown' | 'anonymous' | 'authenticated'>('anonymous');
  const fake = {
    status,
    isAuthenticated: () => status() === 'authenticated',
    restore: vi.fn(async () => undefined),
  };

  beforeEach(() => {
    fake.restore.mockClear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: SessionService, useValue: fake }] });
  });

  function run(url: string) {
    return TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));
  }

  it('lets a signed-in person through', async () => {
    status.set('authenticated');

    expect(await run('/cuenta')).toBe(true);
  });

  it('sends everyone else to the login and remembers where they were going', async () => {
    status.set('anonymous');

    const result = await run('/cuenta/pedidos');

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/entrar?volver=%2Fcuenta%2Fpedidos');
  });

  it('first tries to recover the session when it is not known yet', async () => {
    status.set('unknown');

    await run('/cuenta');

    expect(fake.restore).toHaveBeenCalledTimes(1);
  });
});
