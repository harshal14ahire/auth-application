import { Component, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-mfa-challenge',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-container">
      <div class="auth-card glass-card animate-in">
        <h1>🛡️ MFA Required</h1>
        <p class="subtitle">Complete verification to continue</p>
        @if (error()) { <div style="background:rgba(239,68,68,0.12);color:var(--danger);padding:10px;border-radius:8px;margin-bottom:16px;font-size:13px">{{ error() }}</div> }
        @for (m of methods(); track m) {
          <div class="mfa-option" [class.selected]="selectedMethod() === m" (click)="selectedMethod.set(m)" role="radio" [attr.aria-checked]="selectedMethod() === m" tabindex="0">
            <div class="icon">{{ m === 'TOTP' ? '📱' : m === 'WEBAUTHN' ? '🔑' : m === 'EMAIL_OTP' ? '📧' : '💬' }}</div>
            <div class="info"><h3>{{ m }}</h3><p>{{ m === 'TOTP' ? 'Google Authenticator' : m === 'WEBAUTHN' ? 'Passkey / Biometrics' : m === 'EMAIL_OTP' ? 'Email code' : 'SMS code' }}</p></div>
          </div>
        }
        @if (selectedMethod() === 'EMAIL_OTP' || selectedMethod() === 'SMS_OTP') {
          <button class="btn btn-outline" style="width:100%;margin:12px 0" (click)="sendOtp()">📨 Send Code</button>
        }
        @if (selectedMethod() !== 'WEBAUTHN') {
          <div class="form-group"><label for="ch-code">Verification Code</label>
            <input id="ch-code" type="text" [ngModel]="code()" (ngModelChange)="code.set($event)" placeholder="Enter 6-digit code" maxlength="6" autocomplete="one-time-code" style="text-align:center;font-size:24px;letter-spacing:8px" (keyup.enter)="verify()">
          </div>
        }
        <button class="btn btn-primary" style="width:100%" [disabled]="loading() || (!code() && selectedMethod() !== 'WEBAUTHN')" (click)="verify()">
          @if (loading()) { ⏳ Verifying... } @else { ✅ Verify }
        </button>
      </div>
    </div>
  `
})
export class MfaChallengeComponent implements OnInit {
  mfaToken = signal('');
  methods = signal<string[]>([]);
  selectedMethod = signal('');
  code = signal('');
  loading = signal(false);
  error = signal('');

  constructor(private route: ActivatedRoute, private router: Router, private auth: AuthService) {}

  ngOnInit(): void {
    this.mfaToken.set(this.route.snapshot.queryParamMap.get('token') || '');
    const m = this.route.snapshot.queryParamMap.get('methods')?.split(',') || [];
    this.methods.set(m);
    if (m.length) this.selectedMethod.set(m[0]);
    if (!this.mfaToken()) this.router.navigate(['/login']);
  }

  sendOtp(): void { this.auth.sendOtp(this.selectedMethod()).subscribe(); }

  verify(): void {
    this.loading.set(true); this.error.set('');
    this.auth.verifyMfa({ mfaToken: this.mfaToken(), code: this.code(), method: this.selectedMethod() }).subscribe({
      next: () => { this.loading.set(false); this.router.navigate(['/dashboard']); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Verification failed'); }
    });
  }
}
