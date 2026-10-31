import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PROBLEM_CODES, fieldErrors, problemFrom } from 'api';
import { LvAlertComponent, LvButtonDirective, LvControlDirective, LvFieldComponent } from 'ui';
import {
  EMAIL_MESSAGE,
  PASSWORD_MESSAGE,
  TERMS_MESSAGE,
  USERNAME_MESSAGE,
  isEmailValid,
  isPasswordValid,
  isUsernameValid,
  normalizeEmail,
  normalizeUsername,
  passwordChecks,
} from './account-rules';
import { AccountService } from './account.service';
import { AuthLayout } from './auth-layout';
import type { VerificationState } from './verification-state';

const CONNECTION_MESSAGE = 'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';

/**
 * Registro: nombre de usuario, correo y contraseña. Avisa mientras se escribe (con las condiciones de la contraseña a la
 * vista) y, al enviar, lleva a la pantalla donde se escribe el código que llega por correo.
 */
@Component({
  selector: 'lv-register-page',
  imports: [ReactiveFormsModule, AuthLayout, LvAlertComponent, LvButtonDirective, LvControlDirective, LvFieldComponent],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage {
  private readonly accounts = inject(AccountService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  protected readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true }),
    email: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true }),
    acceptTerms: new FormControl(false, { nonNullable: true }),
  });

  protected readonly showPassword = signal(false);
  protected readonly submitted = signal(false);
  protected readonly busy = signal(false);
  protected readonly generalError = signal('');
  private readonly serverErrors = signal<Record<string, string>>({});

  private readonly username = toSignal(this.form.controls.username.valueChanges, { initialValue: '' });
  private readonly email = toSignal(this.form.controls.email.valueChanges, { initialValue: '' });
  private readonly password = toSignal(this.form.controls.password.valueChanges, { initialValue: '' });
  private readonly acceptTerms = toSignal(this.form.controls.acceptTerms.valueChanges, { initialValue: false });

  protected readonly passwordChecks = computed(() => passwordChecks(this.password()));

  protected readonly usernameError = computed(
    () => this.serverErrors()['username'] ?? (this.submitted() && !isUsernameValid(this.username()) ? USERNAME_MESSAGE : ''),
  );
  protected readonly emailError = computed(
    () => this.serverErrors()['email'] ?? (this.submitted() && !isEmailValid(this.email()) ? EMAIL_MESSAGE : ''),
  );
  protected readonly passwordError = computed(
    () => this.serverErrors()['password'] ?? (this.submitted() && !isPasswordValid(this.password()) ? PASSWORD_MESSAGE : ''),
  );
  protected readonly termsError = computed(
    () => this.serverErrors()['acceptTerms'] ?? (this.submitted() && !this.acceptTerms() ? TERMS_MESSAGE : ''),
  );

  constructor() {
    // Cuando la persona corrige un campo, desaparece el error que el servidor le había puesto a ese campo.
    this.clearServerErrorOnEdit(this.form.controls.username, 'username');
    this.clearServerErrorOnEdit(this.form.controls.email, 'email');
    this.clearServerErrorOnEdit(this.form.controls.password, 'password');
    this.clearServerErrorOnEdit(this.form.controls.acceptTerms, 'acceptTerms');
  }

  /** Solo se llama desde el constructor (necesita el contexto de inyección para limpiar la suscripción al destruirse). */
  private clearServerErrorOnEdit(control: AbstractControl, field: string): void {
    control.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.clearServerError(field));
  }

  protected toggleShowPassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    this.generalError.set('');

    const raw = this.form.getRawValue();
    const valid =
      isUsernameValid(raw.username) && isEmailValid(raw.email) && isPasswordValid(raw.password) && raw.acceptTerms;
    if (!valid) {
      this.focusFirstError();
      return;
    }

    this.busy.set(true);
    try {
      const email = normalizeEmail(raw.email);
      const result = await this.accounts.register({
        username: normalizeUsername(raw.username),
        email,
        password: raw.password,
        acceptTerms: true,
      });
      const state: VerificationState = {
        email,
        maskedEmail: result.email,
        resendAfterSeconds: result.resendAfterSeconds,
      };
      await this.router.navigate(['/verificar'], { state });
    } catch (error) {
      this.showServerError(error);
    } finally {
      this.busy.set(false);
    }
  }

  private showServerError(error: unknown): void {
    const problem = problemFrom(error);
    if (problem === null) {
      // No es un error estándar del servidor (sin conexión, fallo del navegador...): se deja constancia para poder investigarlo.
      console.error('Registro: error que no es del servidor', error);
      this.generalError.set(CONNECTION_MESSAGE);
      return;
    }
    if (problem.code === PROBLEM_CODES.usernameTaken) {
      this.serverErrors.set({ username: problem.detail });
    } else if (problem.code === PROBLEM_CODES.validationFailed) {
      const errors = fieldErrors(problem);
      this.serverErrors.set(errors);
      if (Object.keys(errors).length === 0) {
        this.generalError.set(problem.detail);
      }
    } else {
      this.generalError.set(problem.detail);
    }
    this.focusFirstError();
  }

  private clearServerError(field: string): void {
    if (field in this.serverErrors()) {
      this.serverErrors.update((errors) => Object.fromEntries(Object.entries(errors).filter(([name]) => name !== field)));
    }
  }

  /** Mueve el foco al primer campo con error, para que quien usa el teclado o un lector de pantalla sepa dónde corregir. */
  private focusFirstError(): void {
    afterNextRender(
      () => this.host.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
      { injector: this.injector },
    );
  }
}
