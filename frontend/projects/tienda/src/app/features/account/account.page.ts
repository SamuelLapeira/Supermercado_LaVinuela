import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SessionService } from 'core';
import { LvAlertComponent, LvButtonDirective } from 'ui';
import { AuthLayout } from './auth-layout';

/** Pantalla provisional de «Mi cuenta»: confirma quién ha entrado y permite salir. Se ampliará con pedidos, direcciones y datos. */
@Component({
  selector: 'lv-account-page',
  imports: [AuthLayout, LvAlertComponent, LvButtonDirective],
  templateUrl: './account.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  private readonly router = inject(Router);
  protected readonly session = inject(SessionService);
  protected readonly leaving = signal(false);

  protected async logout(): Promise<void> {
    this.leaving.set(true);
    try {
      await this.session.logout();
      await this.router.navigateByUrl('/entrar');
    } finally {
      this.leaving.set(false);
    }
  }
}
