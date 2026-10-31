import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccountService } from './account.service';
import { VerifyEmailPage } from './verify-email.page';

const STATE = { email: 'ana@example.com', maskedEmail: 'a***@example.com', resendAfterSeconds: 60 };

function problem(status: number, code: string, detail: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { type: 'urn:test', title: 'Error', status, detail, code } });
}

describe('VerifyEmailPage', () => {
  const accounts = { register: vi.fn(), verifyEmail: vi.fn(), resendVerification: vi.fn() };

  beforeEach(async () => {
    accounts.verifyEmail.mockReset();
    accounts.resendVerification.mockReset();
    await TestBed.configureTestingModule({
      imports: [VerifyEmailPage],
      providers: [provideRouter([]), { provide: AccountService, useValue: accounts }],
    }).compileComponents();
  });

  afterEach(() => {
    window.history.replaceState(null, '');
    vi.useRealTimers();
  });

  async function render(state: unknown = STATE): Promise<{ fixture: ComponentFixture<VerifyEmailPage>; el: HTMLElement }> {
    window.history.replaceState(state, '');
    const fixture = TestBed.createComponent(VerifyEmailPage);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  async function settle(fixture: ComponentFixture<VerifyEmailPage>): Promise<void> {
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

  function submit(el: HTMLElement): void {
    el.querySelector('form')?.dispatchEvent(new Event('submit'));
  }

  function resendButton(el: HTMLElement): HTMLButtonElement {
    const found = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Reenviar código'));
    if (!found) {
      throw new Error('No existe el botón de reenviar');
    }
    return found;
  }

  it('shows the masked email and only asks for the code when it comes from the registration', async () => {
    const { el } = await render();

    expect(el.textContent).toContain('a***@example.com');
    expect(el.querySelector('input[type="email"]')).toBeNull();
    expect(control(el, 'input[formcontrolname="code"]').getAttribute('autocomplete')).toBe('one-time-code');
  });

  it('asks for the email too when the page is opened directly', async () => {
    const { el } = await render(null);

    expect(el.querySelector('input[type="email"]')).not.toBeNull();
    expect(resendButton(el).disabled).toBe(false);
  });

  it('starts with the resend button disabled because a code has just been sent', async () => {
    const { el } = await render();

    expect(resendButton(el).disabled).toBe(true);
    expect(resendButton(el).textContent).toContain('Reenviar código en 60 s');
  });

  it('counts down and enables the resend button when the wait is over', async () => {
    vi.useFakeTimers();
    window.history.replaceState(STATE, '');
    const fixture = TestBed.createComponent(VerifyEmailPage);
    const el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();

    vi.advanceTimersByTime(3000);
    fixture.detectChanges();
    expect(resendButton(el).textContent).toContain('Reenviar código en 57 s');

    vi.advanceTimersByTime(57_000);
    fixture.detectChanges();
    expect(resendButton(el).disabled).toBe(false);
    expect(resendButton(el).textContent?.trim()).toBe('Reenviar código');
  });

  it('keeps only digits, at most six', async () => {
    const { el } = await render();
    const code = control(el, 'input[formcontrolname="code"]');

    type(code, '482-915 99');

    expect(code.value).toBe('482915');
  });

  it('does not call the server with an incomplete code', async () => {
    const { fixture, el } = await render();
    type(control(el, 'input[formcontrolname="code"]'), '123');

    submit(el);
    await settle(fixture);

    expect(accounts.verifyEmail).not.toHaveBeenCalled();
    expect(control(el, 'input[formcontrolname="code"]').getAttribute('aria-invalid')).toBe('true');
  });

  it('activates the account with the right code', async () => {
    accounts.verifyEmail.mockResolvedValue(undefined);
    const { fixture, el } = await render();
    type(control(el, 'input[formcontrolname="code"]'), '482915');

    submit(el);
    await settle(fixture);

    expect(accounts.verifyEmail).toHaveBeenCalledWith({ email: 'ana@example.com', code: '482915' });
    expect(el.textContent).toContain('¡Cuenta activada!');
  });

  it('says so when the code is wrong and lets the person try again', async () => {
    accounts.verifyEmail.mockRejectedValue(problem(422, 'VERIFICATION_CODE_INVALID', 'El código no es correcto. Revísalo e inténtalo de nuevo.'));
    const { fixture, el } = await render();
    type(control(el, 'input[formcontrolname="code"]'), '000000');

    submit(el);
    await settle(fixture);

    expect(el.textContent).toContain('El código no es correcto');
    expect(control(el, 'input[formcontrolname="code"]').getAttribute('aria-invalid')).toBe('true');
    expect(control(el, 'input[formcontrolname="code"]').disabled).toBe(false);
    expect(el.textContent).not.toContain('¡Cuenta activada!');
  });

  it('says so when the code has expired', async () => {
    accounts.verifyEmail.mockRejectedValue(problem(422, 'VERIFICATION_CODE_EXPIRED', 'El código ha caducado. Pide uno nuevo.'));
    const { fixture, el } = await render();
    type(control(el, 'input[formcontrolname="code"]'), '482915');

    submit(el);
    await settle(fixture);

    expect(el.textContent).toContain('El código ha caducado');
  });

  it('blocks the code after too many attempts until a new one is requested', async () => {
    accounts.verifyEmail.mockRejectedValue(problem(429, 'VERIFICATION_TOO_MANY_ATTEMPTS', 'Has probado demasiados códigos. Pide uno nuevo.'));
    accounts.resendVerification.mockResolvedValue({ resendAfterSeconds: 60 });
    const { fixture, el } = await render(null);
    type(control(el, 'input[formcontrolname="email"]'), 'ana@example.com');
    type(control(el, 'input[formcontrolname="code"]'), '482915');

    submit(el);
    await settle(fixture);
    expect(el.textContent).toContain('Has probado demasiados códigos');
    expect(control(el, 'input[formcontrolname="code"]').disabled).toBe(true);
    expect(control(el, 'button[type="submit"]').disabled).toBe(true);

    resendButton(el).click();
    await settle(fixture);

    expect(accounts.resendVerification).toHaveBeenCalledWith({ email: 'ana@example.com' });
    expect(control(el, 'input[formcontrolname="code"]').disabled).toBe(false);
    expect(control(el, 'button[type="submit"]').disabled).toBe(false);
    expect(el.textContent).toContain('te llegará un código nuevo');
  });

  it('shows a neutral message when a new code is requested and starts the wait again', async () => {
    accounts.resendVerification.mockResolvedValue({ resendAfterSeconds: 60 });
    const { fixture, el } = await render(null);
    type(control(el, 'input[formcontrolname="email"]'), 'ana@example.com');

    resendButton(el).click();
    await settle(fixture);

    expect(el.textContent).toContain('Si hay una cuenta pendiente con ese correo');
    expect(resendButton(el).disabled).toBe(true);
  });

  it('tells the person when the server cannot be reached', async () => {
    accounts.verifyEmail.mockRejectedValue(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }));
    const { fixture, el } = await render();
    type(control(el, 'input[formcontrolname="code"]'), '482915');

    submit(el);
    await settle(fixture);

    expect(el.querySelector('lv-alert')?.textContent).toContain('No hemos podido conectar con el servidor');
  });
});
