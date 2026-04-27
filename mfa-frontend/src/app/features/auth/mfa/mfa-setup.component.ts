import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

import { create } from '@github/webauthn-json';

@Component({
  selector: 'app-mfa-setup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-container" style="align-items:flex-start;padding-top:60px">
      <div class="glass-card animate-in" style="width:100%;max-width:560px">
        <h1 style="margin-bottom:4px">🔐 MFA Setup</h1>
        <p class="subtitle">Add extra security to your account</p>

        @if (message()) { <div style="background:rgba(16,185,129,0.12);color:var(--success);padding:10px 16px;border-radius:8px;margin-bottom:16px;font-size:13px">{{ message() }}</div> }
        @if (error()) { <div style="background:rgba(239,68,68,0.12);color:var(--danger);padding:10px 16px;border-radius:8px;margin-bottom:16px;font-size:13px">{{ error() }}</div> }

        <!-- MFA Method Selection -->
        <div class="mfa-option" (click)="setupTotp()" role="button" tabindex="0" aria-label="Set up TOTP authenticator">
          <div class="icon">📱</div>
          <div class="info"><h3>Authenticator App (TOTP)</h3><p>Use Google Authenticator, Authy, etc.</p></div>
        </div>
        <div class="mfa-option" (click)="setupEmailOtp()" role="button" tabindex="0" aria-label="Set up email OTP">
          <div class="icon">📧</div>
          <div class="info"><h3>Email OTP</h3><p>Receive codes via email</p></div>
        </div>
        <div class="mfa-option" (click)="setupSmsOtp()" role="button" tabindex="0" aria-label="Set up SMS OTP">
          <div class="icon">💬</div>
          <div class="info"><h3>SMS OTP</h3><p>Receive codes via text message</p></div>
        </div>
        <div class="mfa-option" (click)="setupWebAuthn()" role="button" tabindex="0" aria-label="Set up WebAuthn passkey">
          <div class="icon">🔑</div>
          <div class="info"><h3>Passkey / WebAuthn</h3><p>Biometric or security key</p></div>
        </div>

        <!-- TOTP Setup Panel -->
        @if (showTotpSetup()) {
          <div class="glass-card" style="margin-top:20px">
            <h3 style="margin-bottom:12px">📱 Scan with Authenticator</h3>
            @if (qrCodeUri()) { <div style="text-align:center;margin-bottom:16px"><img [src]="qrCodeUri()" alt="TOTP QR Code" style="max-width:200px;border-radius:8px"></div> }
            <p style="font-size:12px;color:var(--text-secondary);margin-bottom:12px;word-break:break-all">Manual key: {{ totpSecret() }}</p>
            <div class="form-group"><label for="totp-code">Verification Code</label>
              <input id="totp-code" type="text" [ngModel]="verifyCode()" (ngModelChange)="verifyCode.set($event)" placeholder="Enter 6-digit code" maxlength="6" style="text-align:center;font-size:20px;letter-spacing:6px" (keyup.enter)="confirmTotp()">
            </div>
            <button class="btn btn-primary" style="width:100%" [disabled]="verifyCode().length < 6" (click)="confirmTotp()">✅ Confirm & Enable</button>
          </div>
        }

        <!-- OTP Verification Panel -->
        @if (showOtpVerify()) {
          <div class="glass-card" style="margin-top:20px">
            <h3 style="margin-bottom:12px">{{ otpMethod() === 'EMAIL_OTP' ? '📧' : '💬' }} Verify {{ otpMethod() === 'EMAIL_OTP' ? 'Email' : 'SMS' }} OTP</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin-bottom:12px">A code has been sent. Enter it below.</p>
            <div class="form-group"><label for="otp-code">OTP Code</label>
              <input id="otp-code" type="text" [ngModel]="verifyCode()" (ngModelChange)="verifyCode.set($event)" placeholder="Enter 6-digit code" maxlength="6" style="text-align:center;font-size:20px;letter-spacing:6px" (keyup.enter)="confirmOtp()">
            </div>
            <button class="btn btn-primary" style="width:100%" [disabled]="verifyCode().length < 6" (click)="confirmOtp()">✅ Confirm & Enable</button>
          </div>
        }

        <button class="btn btn-outline" style="width:100%;margin-top:20px" (click)="goBack()">← Back to Dashboard</button>
      </div>
    </div>
  `
})
export class MfaSetupComponent {
  showTotpSetup = signal(false);
  showOtpVerify = signal(false);
  otpMethod = signal('');
  qrCodeUri = signal('');
  totpSecret = signal('');
  verifyCode = signal('');
  message = signal('');
  error = signal('');

  constructor(private auth: AuthService, private router: Router) {}

  setupTotp(): void {
    this.resetPanels();
    this.auth.setupTotp().subscribe({
      next: (res) => { this.qrCodeUri.set(res.qrCodeUri); this.totpSecret.set(res.secret); this.showTotpSetup.set(true); },
      error: (err) => this.error.set(err.error?.message || 'Failed to setup TOTP')
    });
  }

  confirmTotp(): void {
    this.auth.verifyTotpSetup(this.verifyCode()).subscribe({
      next: (res) => { this.message.set(res.message); this.showTotpSetup.set(false); this.verifyCode.set(''); },
      error: (err) => this.error.set(err.error?.message || 'Invalid code')
    });
  }

  setupEmailOtp(): void { this.initOtp('EMAIL_OTP'); }
  setupSmsOtp(): void { this.initOtp('SMS_OTP'); }

  private initOtp(method: string): void {
    this.resetPanels();
    this.otpMethod.set(method);
    this.auth.sendOtp(method).subscribe({
      next: () => this.showOtpVerify.set(true),
      error: (err) => this.error.set(err.error?.message || 'Failed to send OTP')
    });
  }

  confirmOtp(): void {
    this.auth.enableOtp(this.otpMethod(), this.verifyCode()).subscribe({
      next: (res) => { this.message.set(res.message); this.showOtpVerify.set(false); this.verifyCode.set(''); },
      error: (err) => this.error.set(err.error?.message || 'Invalid OTP')
    });
  }

  private resetPanels(): void {
    this.showTotpSetup.set(false); this.showOtpVerify.set(false);
    this.message.set(''); this.error.set(''); this.verifyCode.set('');
  }

  async setupWebAuthn() {
    this.resetPanels();
    this.message.set('Please follow the prompt to register your passkey...');
    
    this.auth.webAuthnRegisterOptions().subscribe({
      next: async (optionsRes) => {
        try {
          const credential = await create({ publicKey: optionsRes.options });
          
          const attestationObject = credential.response.attestationObject;
          const clientDataJSON = credential.response.clientDataJSON;
          
          this.auth.webAuthnRegisterVerify('My Passkey', attestationObject, clientDataJSON).subscribe({
            next: (res) => {
              if (res.success) {
                this.message.set(res.message);
              } else {
                this.error.set(res.message || 'Passkey registration failed');
              }
            },
            error: (err) => this.error.set(err.error?.message || 'Failed to verify passkey on server')
          });
        } catch (e: any) {
          this.message.set('');
          this.error.set(e.message || 'Passkey registration cancelled or failed');
        }
      },
      error: (err) => {
        this.message.set('');
        this.error.set(err.error?.message || 'Failed to fetch passkey options')
      }
    });
  }

  goBack(): void { this.router.navigate(['/dashboard']); }
}
