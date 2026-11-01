/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Api } from 'api';
import { sessionHint } from './session-hint';
import { SessionService } from './session.service';

const SESSION = {
  accessToken: 'token-1',
  expiresInSeconds: 900,
  user: { id: '01KB3Z6Q8M2R7V1N4P9XJ5C0TD', username: 'ana', email: 'ana@example.com', roles: ['CUSTOMER'] },
};

describe('SessionService', () => {
  const api = { invoke: vi.fn() };

  function service(): SessionService {
    TestBed.configureTestingModule({ providers: [{ provide: Api, useValue: api }] });
    return TestBed.inject(SessionService);
  }

  beforeEach(() => {
    api.invoke.mockReset();
    localStorage.clear();
  });

  it('starts without knowing whether there is a session', () => {
    const session = service();

    expect(session.status()).toBe('unknown');
    expect(session.user()).toBeNull();
    expect(session.accessToken()).toBeNull();
  });

  it('opens the session on login and keeps the token only in memory', async () => {
    api.invoke.mockResolvedValue(SESSION);
    const session = service();

    await session.login('ana@example.com', 'Una-contraseña-larga-2026');

    expect(session.status()).toBe('authenticated');
    expect(session.isAuthenticated()).toBe(true);
    expect(session.user()?.username).toBe('ana');
    expect(session.accessToken()).toBe('token-1');
    expect(api.invoke.mock.calls[0]?.[1]).toEqual({ body: { email: 'ana@example.com', password: 'Una-contraseña-larga-2026' } });
    expect(JSON.stringify({ ...localStorage })).not.toContain('token-1');
    expect(sessionHint.isSet()).toBe(true);
  });

  it('stays out when the login fails and lets the error reach the screen', async () => {
    const refused = new HttpErrorResponse({ status: 401 });
    api.invoke.mockRejectedValue(refused);
    const session = service();

    await expect(session.login('ana@example.com', 'mala')).rejects.toBe(refused);

    expect(session.isAuthenticated()).toBe(false);
    expect(session.accessToken()).toBeNull();
  });

  it('does not bother the server when nobody had a session', async () => {
    const session = service();

    await session.restore();

    expect(session.status()).toBe('anonymous');
    expect(api.invoke).not.toHaveBeenCalled();
  });

  it('recovers the session on start when the person had one', async () => {
    sessionHint.set();
    api.invoke.mockResolvedValue(SESSION);
    const session = service();

    await session.restore();

    expect(session.isAuthenticated()).toBe(true);
    expect(session.user()?.email).toBe('ana@example.com');
  });

  it('renews only once when several callers ask at the same time', async () => {
    api.invoke.mockResolvedValue(SESSION);
    const session = service();

    const results = await Promise.all([session.refresh(), session.refresh(), session.refresh()]);

    expect(results).toEqual([true, true, true]);
    expect(api.invoke).toHaveBeenCalledTimes(1);
  });

  it('can renew again after the previous renewal has finished', async () => {
    api.invoke.mockResolvedValue(SESSION);
    const session = service();

    await session.refresh();
    await session.refresh();

    expect(api.invoke).toHaveBeenCalledTimes(2);
  });

  it('forgets the session when the server refuses to renew it', async () => {
    sessionHint.set();
    api.invoke.mockRejectedValue(new HttpErrorResponse({ status: 401 }));
    const session = service();

    const renewed = await session.refresh();

    expect(renewed).toBe(false);
    expect(session.status()).toBe('anonymous');
    expect(sessionHint.isSet()).toBe(false);
  });

  it('keeps the hint when the server cannot be reached, so it can try again later', async () => {
    sessionHint.set();
    api.invoke.mockRejectedValue(new HttpErrorResponse({ status: 0 }));
    const session = service();

    await session.refresh();

    expect(session.status()).toBe('anonymous');
    expect(sessionHint.isSet()).toBe(true);
  });

  it('logs out: tells the server and forgets everything', async () => {
    api.invoke.mockResolvedValueOnce(SESSION).mockResolvedValueOnce(undefined);
    const session = service();
    await session.login('ana@example.com', 'x');

    await session.logout();

    expect(api.invoke).toHaveBeenCalledTimes(2);
    expect(session.status()).toBe('anonymous');
    expect(session.user()).toBeNull();
    expect(session.accessToken()).toBeNull();
    expect(sessionHint.isSet()).toBe(false);
  });

  it('logs out here even if the server does not answer', async () => {
    api.invoke.mockResolvedValueOnce(SESSION).mockRejectedValueOnce(new HttpErrorResponse({ status: 0 }));
    const session = service();
    await session.login('ana@example.com', 'x');

    await session.logout();

    expect(session.isAuthenticated()).toBe(false);
    expect(sessionHint.isSet()).toBe(false);
  });
});
