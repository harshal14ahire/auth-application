import { Component, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models/auth.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div style="min-height:100vh;padding:24px">
      <nav style="display:flex;justify-content:space-between;align-items:center;margin-bottom:40px;max-width:900px;margin:0 auto 40px">
        <h2 style="font-size:20px;font-weight:700">🔐 Passway</h2>
        <button class="btn btn-outline" (click)="logout()" aria-label="Sign out">Sign Out</button>
      </nav>

      <div style="max-width:900px;margin:0 auto">
        @if (user()) {
          <div class="glass-card animate-in" style="margin-bottom:20px">
            <h1 style="font-size:24px;margin-bottom:4px">Welcome, {{ user()!.username }} 👋</h1>
            <p style="color:var(--text-secondary);font-size:14px">{{ user()!.email }} · Provider: {{ user()!.provider }}</p>
          </div>

          <div class="glass-card animate-in" style="animation-delay:0.1s">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
              <h2 style="font-size:18px">🛡️ Security Status</h2>
              <span class="badge" [class.badge-success]="user()!.mfaEnabled" [class.badge-warning]="!user()!.mfaEnabled">
                {{ user()!.mfaEnabled ? '✅ MFA Active' : '⚠️ MFA Disabled' }}
              </span>
            </div>

            @if (user()!.mfaMethods.length) {
              <p style="font-size:14px;color:var(--text-secondary);margin-bottom:12px">Active methods:</p>
              <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:16px">
                @for (m of user()!.mfaMethods; track m) {
                  <span class="badge badge-success">{{ m === 'TOTP' ? '📱 TOTP' : m === 'EMAIL_OTP' ? '📧 Email' : m === 'SMS_OTP' ? '💬 SMS' : '🔑 Passkey' }}</span>
                }
              </div>
            }

            <a routerLink="/mfa/setup" class="btn btn-primary" aria-label="Manage MFA settings">
              ⚙️ Manage MFA
            </a>
          </div>
        } @else {
          <div class="glass-card"><p class="loading">Loading profile...</p></div>
        }
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  user = signal<UserProfile | null>(null);

  constructor(private auth: AuthService) {}

  ngOnInit(): void {
    this.auth.getProfile().subscribe({ next: (u) => this.user.set(u) });
  }

  logout(): void { this.auth.logout(); }
}
