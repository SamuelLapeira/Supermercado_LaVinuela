import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PROBLEM_CODES, fieldErrors, problemFrom } from 'api';
import { LvAlertComponent, LvButtonDirective, LvControlDirective, LvFieldComponent } from 'ui';
import { CODE_MESSAGE, EMAIL_MESSAGE, isCodeValid, isEmailValid, normalizeEmail } from './account-rules';
import { AccountService } from './account.service';
import { AuthLayout } from './auth-layout';
import { parseVerificationState } from './verification-state';
import type { VerificationState } from './verification-state';

const CONNECTION_MESSAGE = 'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
const RESENT_MESSAGE =
  'Si hay una cuenta pendiente con ese correo, te llegará un código nuevo en unos minutos. Revisa también la carpeta de spam.';

/**
 * Verificación del correo: se escribe el código de 6 cifras que llegó por correo. Permite pedir un código nuevo (con una
 * cuenta atrás, porque el servidor exige esperar) y avisa claramente si el código es incorrecto, ha caducado o se han
 * agotado los intentos.
 *
 * Si se llega desde el registro, el correo ya se conoce y no se vuelve a pedir; si se entra directamente (por ejemplo,
 * desde un enlace guardado), se pide el correo además del código.
 */
@Component({
  selector: 'lv-verify-email-page',
  imports: [ReactiveFormsModule, RouterLink, AuthLayout, LvAlertComponent, LvButtonDirective, LvControlDirective, LvFieldComponent],
  templateUrl: './verify-email.page.html',
  styleUrl: './verify-email.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyEmailPage {
  private readonly accounts = inject(AccountService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);

  /** Lo que trae la pantalla de registro, si se llega desde ella. */
  private readonly known: VerificationState | null = parseVerificationState(
    inject(DOCUMENT).defaultView?.history.state,
  );

  protected readonly emailKnown = this.known !== null;
  protected readonly maskedEmail = this.known?.maskedEmail ?? '';

  protected readonly form = new FormGroup({
    email: new FormControl(this.known?.email ?? '', { nonNullable: true }),
    code: new FormControl('', { nonNullable: true }),
  });

  protected readonly busy = signal(false);
  protected readonly done = signal(false);
  protected readonly blocked = signal(false);
  protected readonly submitted = signal(false);
  protected readonly cooldown = signal(0);
  protected readonly generalError = signal('');
  protected readonly notice = signal('');
  private readonly serverErrors = signal<Record<string, string>>({});

  private readonly email = toSignal(this.form.controls.email.valueChanges, { initialValue: this.known?.email ?? '' });
  private readonly code = toSignal(this.form.controls.code.valueChanges, { initialValue: '' });

  protected readonly emailError = computed(
    () => this.serverErrors()['email'] ?? (this.submitted() && !isEmailValid(this.email()) ? EMAIL_MESSAGE : ''),
  );
  protected readonly codeError = computed(
    () => this.serverErrors()['code'] ?? (this.submitted() && !isCodeValid(this.code()) ? CODE_MESSAGE : ''),
  );
  protected readonly resendLabel = computed(() =>
    this.cooldown() > 0 ? `Reenviar código en ${this.cooldown()} s` : 'Reenviar código',
  );

  private timer: ReturnType<typeof setInterval> | undefined;

  constructor() {
    // Solo cifras, máximo 6: si se pega «482 915» o «482-915», queda 482915.
    this.form.controls.code.valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
      const digits = value.replace(/\D/g, '').slice(0, 6);
      if (digits !== value) {
        this.form.controls.code.setValue(digits, { emitEvent: false });
      }
      this.serverErrors.update((errors) => Object.fromEntries(Object.entries(errors).filter(([name]) => name !== 'code')));
    });
    this.destroyRef.onDestroy(() => this.stopTimer());

    // El código acaba de enviarse: hay que esperar antes de pedir otro.
    if (this.known !== null) {
      this.startCooldown(this.known.resendAfterSeconds);
    }
  }

  protected async verify(): Promise<void> {
    this.submitted.set(true);
    this.generalError.set('');
    this.notice.set('');
    const raw = this.form.getRawValue();
    if (!isEmailValid(raw.email) || !isCodeValid(raw.code)) {
      this.focusFirstError();
      return;
    }

    this.busy.set(true);
    try {
      await this.accounts.verifyEmail({ email: normalizeEmail(raw.email), code: raw.code });
      this.done.set(true);
    } catch (error) {
      this.showVerifyError(error);
    } finally {
      this.busy.set(false);
    }
  }

  protected async resend(): Promise<void> {
    this.generalError.set('');
    const email = this.form.controls.email.value;
    if (!isEmailValid(email)) {
      this.submitted.set(true);
      this.focusFirstError();
      return;
    }

    this.busy.set(true);
    try {
      const result = await this.accounts.resendVerification({ email: normalizeEmail(email) });
      this.startCooldown(result.resendAfterSeconds);
      this.blocked.set(false);
      this.submitted.set(false);
      this.serverErrors.set({});
      this.form.controls.code.enable({ emitEvent: false });
      this.form.controls.code.setValue('');
      this.notice.set(RESENT_MESSAGE);
    } catch (error) {
      const problem = problemFrom(error);
      if (problem === null) {
        console.error('Reenvío del código: error que no es del servidor', error);
      }
      this.generalError.set(problem === null ? CONNECTION_MESSAGE : problem.detail);
    } finally {
      this.busy.set(false);
    }
  }

  private showVerifyError(error: unknown): void {
    const problem = problemFrom(error);
    if (problem === null) {
      console.error('Verificación: error que no es del servidor', error);
      this.generalError.set(CONNECTION_MESSAGE);
      return;
    }
    switch (problem.code) {
      case PROBLEM_CODES.verificationCodeInvalid:
      case PROBLEM_CODES.verificationCodeExpired:
        this.serverErrors.set({ code: problem.detail });
        break;
      case PROBLEM_CODES.verificationTooManyAttempts:
        this.serverErrors.set({ code: problem.detail });
        this.blocked.set(true);
        // Con los intentos agotados este código ya no sirve: se bloquea el campo hasta que se pida otro.
        this.form.controls.code.disable({ emitEvent: false });
        break;
      case PROBLEM_CODES.validationFailed: {
        const errors = fieldErrors(problem);
        this.serverErrors.set(errors);
        if (Object.keys(errors).length === 0) {
          this.generalError.set(problem.detail);
        }
        break;
      }
      default:
        this.generalError.set(problem.detail);
    }
    this.focusFirstError();
  }

  private startCooldown(seconds: number): void {
    this.stopTimer();
    this.cooldown.set(Math.max(0, seconds));
    if (seconds <= 0) {
      return;
    }
    this.timer = setInterval(() => {
      const next = this.cooldown() - 1;
      this.cooldown.set(Math.max(0, next));
      if (next <= 0) {
        this.stopTimer();
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timer !== undefined) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }

  private focusFirstError(): void {
    afterNextRender(
      () => this.host.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
      { injector: this.injector },
    );
  }
}
