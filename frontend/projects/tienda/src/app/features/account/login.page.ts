import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SessionService } from 'core';
import { PROBLEM_CODES, problemFrom } from 'api';
import { LvAlertComponent, LvButtonDirective, LvControlDirective, LvFieldComponent } from 'ui';
import { EMAIL_MESSAGE, isEmailValid, maskEmail, normalizeEmail, safeReturnUrl } from './account-rules';
import { AccountService } from './account.service';
import { AuthLayout } from './auth-layout';
import type { VerificationState } from './verification-state';

const CONNECTION_MESSAGE = 'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
const PASSWORD_REQUIRED = 'Escribe tu contraseña.';

/**
 * Inicio de sesión con el correo y la contraseña.
 *
 * - Credenciales incorrectas y bloqueo temporal se explican con el mensaje del servidor, que es el mismo tanto si el correo
 *   existe como si no (la pantalla nunca insinúa si la cuenta existe).
 * - Si la contraseña es correcta pero falta confirmar el correo, se pide un código nuevo y se lleva a la pantalla donde se
 *   escribe, en vez de mostrar un error.
 * - Tras entrar, vuelve a la pantalla que se había pedido (solo si es de esta web) o va a la cuenta.
 */
@Component({
  selector: 'lv-login-page',
  imports: [ReactiveFormsModule, RouterLink, AuthLayout, LvAlertComponent, LvButtonDirective, LvControlDirective, LvFieldComponent],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly session = inject(SessionService);
  private readonly accounts = inject(AccountService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true }),
  });

  protected readonly showPassword = signal(false);
  protected readonly submitted = signal(false);
  protected readonly busy = signal(false);
  protected readonly generalError = signal('');

  private readonly email = toSignal(this.form.controls.email.valueChanges, { initialValue: '' });
  private readonly password = toSignal(this.form.controls.password.valueChanges, { initialValue: '' });

  protected readonly emailError = computed(() => (this.submitted() && !isEmailValid(this.email()) ? EMAIL_MESSAGE : ''));
  protected readonly passwordError = computed(() => (this.submitted() && this.password() === '' ? PASSWORD_REQUIRED : ''));

  protected toggleShowPassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    this.generalError.set('');
    const raw = this.form.getRawValue();
    if (!isEmailValid(raw.email) || raw.password === '') {
      this.focusFirstError();
      return;
    }

    this.busy.set(true);
    try {
      await this.session.login(normalizeEmail(raw.email), raw.password);
      await this.router.navigateByUrl(safeReturnUrl(this.route.snapshot.queryParamMap.get('volver')));
    } catch (error) {
      await this.showError(error, normalizeEmail(raw.email));
    } finally {
      this.busy.set(false);
    }
  }

  private async showError(error: unknown, email: string): Promise<void> {
    const problem = problemFrom(error);
    if (problem === null) {
      console.error('Inicio de sesión: error que no es del servidor', error);
      this.generalError.set(CONNECTION_MESSAGE);
      return;
    }
    if (problem.code === PROBLEM_CODES.emailNotVerified) {
      await this.continueWithVerification(email);
      return;
    }
    this.generalError.set(problem.detail);
  }

  /** La contraseña era correcta: se pide un código nuevo y se lleva a la pantalla donde se escribe. */
  private async continueWithVerification(email: string): Promise<void> {
    let resendAfterSeconds = 0;
    try {
      resendAfterSeconds = (await this.accounts.resendVerification({ email })).resendAfterSeconds;
    } catch (error) {
      // No es grave: en la pantalla del código se puede pedir otro.
      if (!(error instanceof HttpErrorResponse)) {
        console.error('Inicio de sesión: no se ha podido pedir el código', error);
      }
    }
    const state: VerificationState = { email, maskedEmail: maskEmail(email), resendAfterSeconds };
    await this.router.navigate(['/verificar'], { state });
  }

  private focusFirstError(): void {
    afterNextRender(
      () => this.host.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
      { injector: this.injector },
    );
  }
}
