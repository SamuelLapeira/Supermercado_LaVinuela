import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AccountService } from './account.service';
import { RegisterPage } from './register.page';

const VALID = { username: 'Ana.Lopez', email: ' Ana@Example.com ', password: 'Una-contraseña-larga-2026' };

function problem(status: number, code: string, detail: string, errors?: { field: string; message: string }[]): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { type: 'urn:test', title: 'Error', status, detail, code, errors } });
}

describe('RegisterPage', () => {
  const accounts = { register: vi.fn(), verifyEmail: vi.fn(), resendVerification: vi.fn() };

  beforeEach(async () => {
    accounts.register.mockReset();
    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideRouter([]), { provide: AccountService, useValue: accounts }],
    }).compileComponents();
  });

  async function render(): Promise<{ fixture: ComponentFixture<RegisterPage>; el: HTMLElement }> {
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  /** Deja que terminen las promesas pendientes (la llamada al servidor, la navegación) y se pinte el resultado. */
  async function settle(fixture: ComponentFixture<RegisterPage>): Promise<void> {
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

  function fill(el: HTMLElement, acceptTerms = true): void {
    type(control(el, 'input[formcontrolname="username"]'), VALID.username);
    type(control(el, 'input[formcontrolname="email"]'), VALID.email);
    type(control(el, 'input[formcontrolname="password"]'), VALID.password);
    if (acceptTerms) {
      control(el, 'input[type="checkbox"]').click();
    }
  }

  function submit(el: HTMLElement): void {
    el.querySelector('form')?.dispatchEvent(new Event('submit'));
  }

  function spyNavigate() {
    return vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  it('ticks the password conditions as they are met', async () => {
    const { fixture, el } = await render();
    expect(el.querySelectorAll('.rules li').length).toBe(4);
    expect(el.querySelectorAll('.rules li.met').length).toBe(0);

    type(control(el, 'input[formcontrolname="password"]'), 'Abc');
    await fixture.whenStable();
    expect(el.querySelectorAll('.rules li.met').length).toBe(2);

    type(control(el, 'input[formcontrolname="password"]'), 'Abcdefghi1');
    await fixture.whenStable();
    expect(el.querySelectorAll('.rules li.met').length).toBe(4);
  });

  it('does not call the server when something is missing and points at the problems', async () => {
    const { fixture, el } = await render();

    submit(el);
    await settle(fixture);

    expect(accounts.register).not.toHaveBeenCalled();
    expect(el.querySelectorAll('[aria-invalid="true"]').length).toBeGreaterThan(0);
    expect(el.textContent).toContain('Tienes que aceptar las condiciones');
  });

  it('sends the normalised data and goes to the verification screen', async () => {
    accounts.register.mockResolvedValue({ email: 'a***@example.com', codeValiditySeconds: 900, resendAfterSeconds: 60 });
    const navigate = spyNavigate();
    const { fixture, el } = await render();

    fill(el);
    submit(el);
    await settle(fixture);

    expect(accounts.register).toHaveBeenCalledWith({
      username: 'ana.lopez',
      email: 'ana@example.com',
      password: VALID.password,
      acceptTerms: true,
    });
    expect(navigate).toHaveBeenCalledWith(['/verificar'], {
      state: { email: 'ana@example.com', maskedEmail: 'a***@example.com', resendAfterSeconds: 60 },
    });
  });

  it('shows the message when the username is already taken and keeps the person on the form', async () => {
    accounts.register.mockRejectedValue(problem(409, 'USERNAME_TAKEN', 'Ese nombre de usuario ya está en uso. Elige otro.'));
    const navigate = spyNavigate();
    const { fixture, el } = await render();

    fill(el);
    submit(el);
    await settle(fixture);

    expect(el.textContent).toContain('Ese nombre de usuario ya está en uso. Elige otro.');
    expect(control(el, 'input[formcontrolname="username"]').getAttribute('aria-invalid')).toBe('true');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('removes the server error from a field when the person edits it', async () => {
    accounts.register.mockRejectedValue(problem(409, 'USERNAME_TAKEN', 'Ese nombre de usuario ya está en uso. Elige otro.'));
    const { fixture, el } = await render();
    fill(el);
    submit(el);
    await settle(fixture);

    type(control(el, 'input[formcontrolname="username"]'), 'ana.lopez2');
    await fixture.whenStable();

    expect(control(el, 'input[formcontrolname="username"]').getAttribute('aria-invalid')).toBeNull();
  });

  it('puts the validation errors of the server next to their fields', async () => {
    accounts.register.mockRejectedValue(
      problem(400, 'VALIDATION_FAILED', 'Hay datos que no son correctos.', [
        { field: 'password', message: 'Esa contraseña es demasiado común. Elige otra más difícil de adivinar.' },
      ]),
    );
    const { fixture, el } = await render();

    fill(el);
    submit(el);
    await settle(fixture);

    expect(el.textContent).toContain('Esa contraseña es demasiado común');
    expect(control(el, 'input[formcontrolname="password"]').getAttribute('aria-invalid')).toBe('true');
  });

  it('tells the person when the server cannot be reached', async () => {
    accounts.register.mockRejectedValue(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }));
    const { fixture, el } = await render();

    fill(el);
    submit(el);
    await settle(fixture);

    expect(el.querySelector('lv-alert')?.textContent).toContain('No hemos podido conectar con el servidor');
  });

  it('disables the button while the account is being created', async () => {
    accounts.register.mockReturnValue(new Promise(() => undefined));
    const { fixture, el } = await render();

    fill(el);
    submit(el);
    await settle(fixture);

    const button = control(el, 'button[type="submit"]');
    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Creando tu cuenta');
  });

  it('can show and hide the password', async () => {
    const { fixture, el } = await render();
    const password = control(el, 'input[formcontrolname="password"]');
    expect(password.type).toBe('password');

    const toggle = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Mostrar contraseña'));
    toggle?.click();
    await fixture.whenStable();

    expect(password.type).toBe('text');
  });
});
