import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { get } from '@github/webauthn-json';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  username = signal('');
  password = signal('');
  loading = signal(false);
  error = signal('');
  step = signal(1);

  // MFA state
  mfaToken = signal('');
  mfaMethods = signal<string[]>([]);
  selectedMethod = signal('');
  mfaCode = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onLogin(): void {
    this.loading.set(true);
    this.error.set('');
    this.authService.login({ username: this.username(), password: this.password() }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.mfaRequired) {
          this.mfaToken.set(res.mfaToken);
          this.mfaMethods.set(res.mfaMethods);
          this.selectedMethod.set(res.mfaMethods[0]);
          this.step.set(2);
        } else {
          this.authService.setToken(res.token);
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Login failed'); }
    });
  }

  onVerifyMfa(): void {
    this.loading.set(true);
    this.error.set('');
    this.authService.verifyMfa({
      mfaToken: this.mfaToken(), code: this.mfaCode(), method: this.selectedMethod()
    }).subscribe({
      next: () => { this.loading.set(false); this.router.navigate(['/dashboard']); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Verification failed'); }
    });
  }

  sendOtp(): void {
    this.authService.sendLoginOtp(this.mfaToken(), this.selectedMethod()).subscribe();
  }

  async webAuthnLogin() {
    this.loading.set(true);
    this.error.set('');
    
    this.authService.webAuthnLoginOptions(this.mfaToken()).subscribe({
      next: async (optionsRes) => {
        try {
          // get the passkey credential from the browser
          const credential = await get({ publicKey: optionsRes.options });
          
          // Use the credential.id as the "code" for verifyMfa
          this.authService.verifyMfa({
            mfaToken: this.mfaToken(),
            code: credential.id, // the base64url encoded credential ID
            method: 'WEBAUTHN'
          }).subscribe({
            next: () => { this.loading.set(false); this.router.navigate(['/dashboard']); },
            error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Verification failed'); }
          });
          
        } catch (e: any) {
          this.loading.set(false);
          this.error.set(e.message || 'Passkey authentication cancelled or failed');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to fetch passkey options');
      }
    });
  }

  loginGoogle(): void { this.authService.loginWithGoogle(); }
  loginGithub(): void { this.authService.loginWithGithub(); }
}
