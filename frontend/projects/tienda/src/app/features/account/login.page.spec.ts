import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { SessionService } from 'core';
import { AccountService } from './account.service';
import { LoginPage } from './login.page';

function problem(status: number, code: string, detail: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { type: 'urn:test', title: 'Error', status, detail, code } });
}

describe('LoginPage', () => {
  const session = { login: vi.fn() };
  const accounts = { resendVerification: vi.fn() };

  beforeEach(() => {
    session.login.mockReset();
    accounts.resendVerification.mockReset();
  });

  async function render(returnTo?: string): Promise<{ fixture: ComponentFixture<LoginPage>; el: HTMLElement }> {
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: session },
        { provide: AccountService, useValue: accounts },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(returnTo ? { volver: returnTo } : {}) } } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  async function settle(fixture: ComponentFixture<LoginPage>): Promise<void> {
    await new Promise<void>((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  function control(el: HTMLElement, selector: string): HTMLInputElement {
    const found = el.querySelector<HTMLInputElement>(selector);
    if (!found) {
      throw new Error(`No existe ${selector}`);
    }
    return found;
  }

  function type(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillAndSubmit(el: HTMLElement, email = ' Ana@Example.com ', password = 'Una-contraseña-larga-2026'): void {
    type(control(el, 'input[formcontrolname="email"]'), email);
    type(control(el, 'input[formcontrolname="password"]'), password);
    el.querySelector('form')?.dispatchEvent(new Event('submit'));
  }

  it('does not call the server when something is missing and points at the problem', async () => {
    const { fixture, el } = await render();

    el.querySelector('form')?.dispatchEvent(new Event('submit'));
    await settle(fixture);

    expect(session.login).not.toHaveBeenCalled();
    expect(el.querySelectorAll('[aria-invalid="true"]').length).toBeGreaterThan(0);
  });

  it('does not ask for a strong password when logging in (only that it is not empty)', async () => {
    session.login.mockResolvedValue(undefined);
    const { fixture, el } = await render();
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fillAndSubmit(el, 'ana@example.com', 'corta');
    await settle(fixture);

    expect(session.login).toHaveBeenCalledWith('ana@example.com', 'corta');
  });

  it('logs in with the normalised email and goes to the account', async () => {
    session.login.mockResolvedValue(undefined);
    const { fixture, el } = await render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fillAndSubmit(el);
    await settle(fixture);

    expect(session.login).toHaveBeenCalledWith('ana@example.com', 'Una-contraseña-larga-2026');
    expect(navigate).toHaveBeenCalledWith('/cuenta');
  });

  it('goes back to the page that was asked for, if it belongs to this site', async () => {
    session.login.mockResolvedValue(undefined);
    const { fixture, el } = await render('/cuenta/pedidos');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fillAndSubmit(el);
    await settle(fixture);

    expect(navigate).toHaveBeenCalledWith('/cuenta/pedidos');
  });

  it('never goes to another site, however the return address is written', async () => {
    session.login.mockResolvedValue(undefined);
    const { fixture, el } = await render('//evil.example.com');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fillAndSubmit(el);
    await settle(fixture);

    expect(navigate).toHaveBeenCalledWith('/cuenta');
  });

  it('shows the message of the server when the credentials are wrong and does not move', async () => {
    session.login.mockRejectedValue(problem(401, 'AUTH_INVALID_CREDENTIALS', 'El correo o la contraseña no son correctos.'));
    const { fixture, el } = await render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fillAndSubmit(el);
    await settle(fixture);

    expect(el.querySelector('lv-alert')?.textContent).toContain('El correo o la contraseña no son correctos.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('explains the temporary block', async () => {
    session.login.mockRejectedValue(problem(423, 'AUTH_ACCOUNT_LOCKED', 'Tu cuenta está bloqueada temporalmente por demasiados intentos. Inténtalo más tarde.'));
    const { fixture, el } = await render();

    fillAndSubmit(el);
    await settle(fixture);

    expect(el.querySelector('lv-alert')?.textContent).toContain('bloqueada temporalmente');
  });

  it('sends a new code and takes the person to verify when the email is not confirmed yet', async () => {
    session.login.mockRejectedValue(problem(403, 'AUTH_EMAIL_NOT_VERIFIED', 'Tienes que confirmar tu correo antes de entrar.'));
    accounts.resendVerification.mockResolvedValue({ resendAfterSeconds: 60 });
    const { fixture, el } = await render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fillAndSubmit(el);
    await settle(fixture);

    expect(accounts.resendVerification).toHaveBeenCalledWith({ email: 'ana@example.com' });
    expect(navigate).toHaveBeenCalledWith(['/verificar'], {
      state: { email: 'ana@example.com', maskedEmail: 'a***@example.com', resendAfterSeconds: 60 },
    });
  });

  it('still takes the person to verify when the new code could not be requested', async () => {
    session.login.mockRejectedValue(problem(403, 'AUTH_EMAIL_NOT_VERIFIED', 'Tienes que confirmar tu correo antes de entrar.'));
    accounts.resendVerification.mockRejectedValue(new HttpErrorResponse({ status: 0 }));
    const { fixture, el } = await render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fillAndSubmit(el);
    await settle(fixture);

    expect(navigate).toHaveBeenCalledWith(['/verificar'], {
      state: { email: 'ana@example.com', maskedEmail: 'a***@example.com', resendAfterSeconds: 0 },
    });
  });

  it('tells the person when the server cannot be reached', async () => {
    session.login.mockRejectedValue(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }));
    const { fixture, el } = await render();

    fillAndSubmit(el);
    await settle(fixture);

    expect(el.querySelector('lv-alert')?.textContent).toContain('No hemos podido conectar con el servidor');
  });

  it('disables the button while logging in', async () => {
    session.login.mockReturnValue(new Promise(() => undefined));
    const { fixture, el } = await render();

    fillAndSubmit(el);
    await settle(fixture);

    const button = control(el, 'button[type="submit"]');
    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Entrando');
  });

  it('can show and hide the password', async () => {
    const { fixture, el } = await render();
    const password = control(el, 'input[formcontrolname="password"]');
    expect(password.type).toBe('password');

    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Mostrar contraseña'))?.click();
    await fixture.whenStable();

    expect(password.type).toBe('text');
  });
});
