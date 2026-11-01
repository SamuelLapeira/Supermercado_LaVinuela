/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Api } from 'api';
import { sessionInterceptor } from './session.interceptor';
import { SessionService } from './session.service';

function session(token: string) {
  return { accessToken: token, expiresInSeconds: 900, user: { id: '01X', username: 'ana', email: 'ana@example.com', roles: ['CUSTOMER'] } };
}

describe('sessionInterceptor', () => {
  const api = { invoke: vi.fn() };
  let http: HttpClient;
  let controller: HttpTestingController;
  let sessions: SessionService;

  beforeEach(() => {
    api.invoke.mockReset();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(), { provide: Api, useValue: api }],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    sessions = TestBed.inject(SessionService);
  });

  afterEach(() => controller.verify());

  async function loggedIn(token = 'token-1'): Promise<void> {
    api.invoke.mockResolvedValueOnce(session(token));
    await sessions.login('ana@example.com', 'x');
    api.invoke.mockReset();
  }

  /** Deja que terminen las promesas pendientes (la renovación) para que se haga la llamada repetida. */
  const tick = () => new Promise<void>((resolve) => setTimeout(resolve));

  it('adds the client header to the API calls and leaves other addresses alone', () => {
    http.get('/api/v1/algo').subscribe();
    http.get('/assets/datos.json').subscribe();

    expect(controller.expectOne('/api/v1/algo').request.headers.get('X-LV-Client')).toBe('web');
    expect(controller.expectOne('/assets/datos.json').request.headers.has('X-LV-Client')).toBe(false);
  });

  it('sends no token when there is no session', () => {
    http.get('/api/v1/me').subscribe();

    expect(controller.expectOne('/api/v1/me').request.headers.has('Authorization')).toBe(false);
  });

  it('adds the access token when there is a session', async () => {
    await loggedIn('token-1');

    http.get('/api/v1/me').subscribe();

    expect(controller.expectOne('/api/v1/me').request.headers.get('Authorization')).toBe('Bearer token-1');
  });

  it('does not send the token to the session operations themselves', async () => {
    await loggedIn('token-1');

    http.post('/api/v1/auth/refresh', null).subscribe();

    const request = controller.expectOne('/api/v1/auth/refresh').request;
    expect(request.headers.has('Authorization')).toBe(false);
    expect(request.headers.get('X-LV-Client')).toBe('web');
  });

  it('renews the session once and repeats the call with the new token when the token has expired', async () => {
    await loggedIn('token-1');
    api.invoke.mockResolvedValue(session('token-2'));
    let result: unknown;
    http.get('/api/v1/me').subscribe((value) => (result = value));

    controller.expectOne('/api/v1/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await tick();
    controller.expectOne((r) => r.url === '/api/v1/me' && r.headers.get('Authorization') === 'Bearer token-2').flush({ ok: true });

    expect(result).toEqual({ ok: true });
    expect(api.invoke).toHaveBeenCalledTimes(1);
  });

  it('lets the error reach the caller when the session cannot be renewed', async () => {
    await loggedIn('token-1');
    api.invoke.mockRejectedValue(new Error('sesión caducada'));
    let status = 0;
    http.get('/api/v1/me').subscribe({ error: (error: { status: number }) => (status = error.status) });

    controller.expectOne('/api/v1/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await tick();

    expect(status).toBe(401);
    expect(sessions.isAuthenticated()).toBe(false);
  });

  it('does not retry other kinds of errors', async () => {
    await loggedIn('token-1');
    let status = 0;
    http.get('/api/v1/me').subscribe({ error: (error: { status: number }) => (status = error.status) });

    controller.expectOne('/api/v1/me').flush({}, { status: 500, statusText: 'Server Error' });
    await tick();

    expect(status).toBe(500);
    expect(api.invoke).not.toHaveBeenCalled();
  });

  it('renews only once when several calls fail at the same time', async () => {
    await loggedIn('token-1');
    api.invoke.mockResolvedValue(session('token-2'));
    http.get('/api/v1/a').subscribe();
    http.get('/api/v1/b').subscribe();

    controller.expectOne('/api/v1/a').flush({}, { status: 401, statusText: 'Unauthorized' });
    controller.expectOne('/api/v1/b').flush({}, { status: 401, statusText: 'Unauthorized' });
    await tick();
    controller.expectOne((r) => r.url === '/api/v1/a' && r.headers.get('Authorization') === 'Bearer token-2').flush({});
    controller.expectOne((r) => r.url === '/api/v1/b' && r.headers.get('Authorization') === 'Bearer token-2').flush({});

    expect(api.invoke).toHaveBeenCalledTimes(1);
  });
});
