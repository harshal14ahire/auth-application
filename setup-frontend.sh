#!/bin/bash
set -e

PROJECT_DIR="/Users/harshalahire/Leanings/auth-application/mfa-frontend"
echo "🚀 Creating Angular 21 MFA Frontend at $PROJECT_DIR"

# Clean if exists
rm -rf "$PROJECT_DIR"
mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

# ── package.json ──
cat > package.json << 'PKGJSON'
{
  "name": "mfa-frontend",
  "version": "0.0.0",
  "scripts": { "ng": "ng", "start": "ng serve", "build": "ng build" },
  "private": true,
  "dependencies": {
    "@angular/animations": "^21.0.0", "@angular/common": "^21.0.0",
    "@angular/compiler": "^21.0.0", "@angular/core": "^21.0.0",
    "@angular/forms": "^21.0.0", "@angular/platform-browser": "^21.0.0",
    "@angular/router": "^21.0.0", "rxjs": "~7.8.0", "tslib": "^2.6.0"
  },
  "devDependencies": {
    "@angular/build": "^21.0.0", "@angular/cli": "^21.0.0",
    "@angular/compiler-cli": "^21.0.0", "typescript": "~5.9.0"
  }
}
PKGJSON

# ── tsconfig.json ──
cat > tsconfig.json << 'TSCONF'
{
  "compileOnSave": false,
  "compilerOptions": {
    "outDir": "./dist/out-tsc", "strict": true, "sourceMap": true,
    "declaration": false, "experimentalDecorators": true,
    "moduleResolution": "bundler", "target": "ES2022", "module": "ES2022",
    "lib": ["ES2022", "dom"], "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true
  },
  "angularCompilerOptions": { "strictTemplates": true }
}
TSCONF

cat > tsconfig.app.json << 'TSAPP'
{ "extends": "./tsconfig.json", "compilerOptions": { "outDir": "./out-tsc/app" }, "files": ["src/main.ts"], "include": ["src/**/*.d.ts"] }
TSAPP

# ── angular.json ──
cat > angular.json << 'ANGJSON'
{
  "$schema": "./node_modules/@angular/cli/lib/config/schema.json", "version": 1,
  "projects": { "mfa-frontend": {
    "projectType": "application", "root": "", "sourceRoot": "src", "prefix": "app",
    "architect": { "build": {
      "builder": "@angular/build:application",
      "options": { "outputPath": "dist/mfa-frontend", "index": "src/index.html",
        "browser": "src/main.ts", "polyfills": [], "tsConfig": "tsconfig.app.json",
        "assets": [{"glob":"**/*","input":"public"}], "styles": ["src/styles.scss"], "scripts": [] },
      "configurations": {
        "production": { "budgets": [{"type":"initial","maximumWarning":"500kB","maximumError":"1MB"}], "outputHashing": "all" },
        "development": { "optimization": false, "sourceMap": true }
      }, "defaultConfiguration": "production"
    }, "serve": {
      "builder": "@angular/build:dev-server",
      "configurations": { "production": {"buildTarget":"mfa-frontend:build:production"}, "development": {"buildTarget":"mfa-frontend:build:development"} },
      "defaultConfiguration": "development", "options": { "proxyConfig": "proxy.conf.json" }
    }}
  }}
}
ANGJSON

# ── proxy.conf.json ──
cat > proxy.conf.json << 'PROXY'
{ "/api": {"target":"http://localhost:8080","secure":false,"changeOrigin":true}, "/oauth2": {"target":"http://localhost:8080","secure":false,"changeOrigin":true} }
PROXY

# ── Create directory structure ──
mkdir -p public src/app/core/services src/app/core/guards src/app/core/models
mkdir -p src/app/features/auth/login src/app/features/auth/register src/app/features/auth/mfa
mkdir -p src/app/features/dashboard src/environments

# ── src/index.html ──
cat > src/index.html << 'HTML'
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Passway MFA — Secure Authentication</title>
  <meta name="description" content="Multi-factor authentication system with TOTP, OTP, and WebAuthn passkeys">
  <base href="/">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
</head>
<body>
  <app-root></app-root>
</body>
</html>
HTML

# ── src/main.ts (Zoneless) ──
cat > src/main.ts << 'MAINTS'
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
MAINTS

# ── src/environments ──
cat > src/environments/environment.ts << 'ENV'
export const environment = { production: false, apiUrl: '/api' };
ENV
cat > src/environments/environment.prod.ts << 'ENVP'
export const environment = { production: true, apiUrl: '/api' };
ENVP

# ── src/styles.scss ──
cat > src/styles.scss << 'STYLES'
:root {
  --bg-primary: #0a0a1a;
  --bg-secondary: #12122a;
  --bg-card: rgba(255,255,255,0.04);
  --bg-card-hover: rgba(255,255,255,0.08);
  --glass-border: rgba(255,255,255,0.1);
  --text-primary: #f0f0ff;
  --text-secondary: #8888aa;
  --accent: #7c3aed;
  --accent-glow: rgba(124,58,237,0.4);
  --success: #10b981;
  --warning: #f59e0b;
  --danger: #ef4444;
  --radius: 16px;
  --radius-sm: 10px;
  --transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html, body { height: 100%; }
body {
  font-family: 'Inter', system-ui, sans-serif;
  background: var(--bg-primary);
  color: var(--text-primary);
  line-height: 1.6;
  overflow-x: hidden;
  background-image:
    radial-gradient(ellipse at 20% 50%, rgba(124,58,237,0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(59,130,246,0.06) 0%, transparent 50%);
}
a { color: var(--accent); text-decoration: none; transition: var(--transition); }
a:hover { color: #a78bfa; }

.glass-card {
  background: var(--bg-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  backdrop-filter: blur(20px);
  padding: 32px;
  transition: var(--transition);
}
.glass-card:hover { background: var(--bg-card-hover); border-color: rgba(255,255,255,0.15); }

.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  padding: 12px 24px; border-radius: var(--radius-sm); border: none;
  font-family: inherit; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: var(--transition);
  text-transform: uppercase; letter-spacing: 0.5px;
}
.btn-primary {
  background: linear-gradient(135deg, var(--accent) 0%, #6d28d9 100%);
  color: white; box-shadow: 0 4px 20px var(--accent-glow);
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 8px 30px var(--accent-glow); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.btn-outline {
  background: transparent; color: var(--text-primary);
  border: 1px solid var(--glass-border);
}
.btn-outline:hover { border-color: var(--accent); color: var(--accent); }
.btn-social {
  background: var(--bg-card); color: var(--text-primary);
  border: 1px solid var(--glass-border); width: 100%; padding: 14px;
}
.btn-social:hover { border-color: var(--accent); background: var(--bg-card-hover); }
.btn-google { border-left: 3px solid #ea4335; }
.btn-github { border-left: 3px solid #f0f0ff; }

.form-group { margin-bottom: 20px; }
.form-group label {
  display: block; margin-bottom: 6px; font-size: 13px;
  font-weight: 500; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px;
}
.form-group input, .form-group select {
  width: 100%; padding: 12px 16px; border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border); background: rgba(255,255,255,0.03);
  color: var(--text-primary); font-family: inherit; font-size: 15px;
  transition: var(--transition); outline: none;
}
.form-group input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-glow); }
.form-group input.invalid { border-color: var(--danger); }
.form-error { color: var(--danger); font-size: 12px; margin-top: 4px; }

.divider { display: flex; align-items: center; gap: 16px; margin: 24px 0; color: var(--text-secondary); font-size: 13px; }
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: var(--glass-border); }

.badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600;
}
.badge-success { background: rgba(16,185,129,0.15); color: var(--success); }
.badge-warning { background: rgba(245,158,11,0.15); color: var(--warning); }
.badge-danger { background: rgba(239,68,68,0.15); color: var(--danger); }

.page-container {
  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px;
}
.auth-card { width: 100%; max-width: 440px; }
.auth-card h1 { font-size: 28px; font-weight: 800; margin-bottom: 4px; }
.auth-card .subtitle { color: var(--text-secondary); margin-bottom: 28px; font-size: 14px; }

@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
.animate-in { animation: fadeInUp 0.5s ease-out forwards; }
.loading { animation: pulse 1.5s ease-in-out infinite; }

.step-indicator {
  display: flex; gap: 8px; margin-bottom: 24px;
}
.step-dot {
  width: 40px; height: 4px; border-radius: 2px; background: var(--glass-border); transition: var(--transition);
}
.step-dot.active { background: var(--accent); box-shadow: 0 0 10px var(--accent-glow); }
.step-dot.completed { background: var(--success); }

.mfa-option {
  display: flex; align-items: center; gap: 16px;
  padding: 16px; border-radius: var(--radius-sm); border: 1px solid var(--glass-border);
  background: var(--bg-card); cursor: pointer; transition: var(--transition); margin-bottom: 10px;
}
.mfa-option:hover { border-color: var(--accent); background: var(--bg-card-hover); }
.mfa-option.selected { border-color: var(--accent); box-shadow: 0 0 0 2px var(--accent-glow); }
.mfa-option .icon { font-size: 28px; width: 44px; text-align: center; }
.mfa-option .info h3 { font-size: 15px; font-weight: 600; }
.mfa-option .info p { font-size: 12px; color: var(--text-secondary); }

.toast {
  position: fixed; top: 24px; right: 24px; padding: 14px 20px;
  border-radius: var(--radius-sm); font-size: 14px; z-index: 9999;
  animation: fadeInUp 0.3s ease-out;
}
.toast-success { background: rgba(16,185,129,0.9); color: white; }
.toast-error { background: rgba(239,68,68,0.9); color: white; }
STYLES

# ── App Config (Zoneless + Routes) ──
cat > src/app/app.config.ts << 'APPCONF'
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/services/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor]))
  ]
};
APPCONF

# ── App Routes ──
cat > src/app/app.routes.ts << 'ROUTES'
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'auth/mfa', loadComponent: () => import('./features/auth/mfa/mfa-challenge.component').then(m => m.MfaChallengeComponent) },
  { path: 'auth/oauth2/callback', loadComponent: () => import('./features/auth/login/oauth-callback.component').then(m => m.OAuthCallbackComponent) },
  { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: 'mfa/setup', loadComponent: () => import('./features/auth/mfa/mfa-setup.component').then(m => m.MfaSetupComponent), canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' }
];
ROUTES

# ── App Component ──
cat > src/app/app.component.ts << 'APPCOMP'
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />',
  styles: [':host { display: block; min-height: 100vh; }']
})
export class AppComponent {}
APPCOMP

# ── Models ──
cat > src/app/core/models/auth.model.ts << 'MODELS'
export interface LoginRequest { username: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; phoneNumber?: string; }
export interface MfaVerifyRequest { mfaToken: string; code: string; method: string; }
export interface AuthResponse { token: string; tokenType: string; mfaRequired: boolean; mfaToken: string; mfaMethods: string[]; message: string; }
export interface ApiResponse { success: boolean; message: string; data: any; }
export interface UserProfile { id: string; username: string; email: string; phoneNumber: string; provider: string; mfaEnabled: boolean; mfaMethods: string[]; }
export interface TotpSetup { secret: string; qrCodeUri: string; message: string; }
MODELS

# ── Auth Service ──
cat > src/app/core/services/auth.service.ts << 'AUTHSVC'
import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, MfaVerifyRequest, ApiResponse, UserProfile, TotpSetup } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = '/api';
  private tokenSignal = signal<string | null>(localStorage.getItem('auth_token'));

  isAuthenticated = computed(() => !!this.tokenSignal());
  token = computed(() => this.tokenSignal());

  constructor(private http: HttpClient, private router: Router) {}

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, req)
      .pipe(tap(res => { if (!res.mfaRequired && res.token) this.setToken(res.token); }));
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, req);
  }

  verifyMfa(req: MfaVerifyRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/mfa/verify`, req)
      .pipe(tap(res => { if (res.token) this.setToken(res.token); }));
  }

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/user/profile`);
  }

  setupTotp(): Observable<TotpSetup> {
    return this.http.post<TotpSetup>(`${this.apiUrl}/mfa/totp/setup`, {});
  }

  verifyTotpSetup(code: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/mfa/totp/verify-setup?code=${code}`, {});
  }

  sendOtp(method: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/mfa/otp/send`, { method });
  }

  enableOtp(method: string, code: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/mfa/otp/enable?code=${code}`, { method });
  }

  getMfaMethods(): Observable<ApiResponse> {
    return this.http.get<ApiResponse>(`${this.apiUrl}/mfa/methods`);
  }

  disableMfa(method: string): Observable<ApiResponse> {
    return this.http.delete<ApiResponse>(`${this.apiUrl}/mfa/${method}`);
  }

  setToken(token: string): void {
    localStorage.setItem('auth_token', token);
    this.tokenSignal.set(token);
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    this.tokenSignal.set(null);
    this.router.navigate(['/login']);
  }

  loginWithGoogle(): void { window.location.href = '/oauth2/authorize/google'; }
  loginWithGithub(): void { window.location.href = '/oauth2/authorize/github'; }
}
AUTHSVC

# ── JWT Interceptor ──
cat > src/app/core/services/jwt.interceptor.ts << 'JWTINT'
import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
JWTINT

# ── Auth Guard ──
cat > src/app/core/guards/auth.guard.ts << 'GUARD'
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  router.navigate(['/login']);
  return false;
};
GUARD

# ── Login Component ──
cat > src/app/features/auth/login/login.component.ts << 'LOGINCOMP'
import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

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
    this.authService.sendOtp(this.selectedMethod()).subscribe();
  }

  loginGoogle(): void { this.authService.loginWithGoogle(); }
  loginGithub(): void { this.authService.loginWithGithub(); }
}
LOGINCOMP

cat > src/app/features/auth/login/login.component.html << 'LOGINHTML'
<div class="page-container">
  <div class="auth-card glass-card animate-in">
    <div class="step-indicator">
      <div class="step-dot" [class.active]="step() === 1" [class.completed]="step() > 1"></div>
      <div class="step-dot" [class.active]="step() === 2"></div>
    </div>

    <!-- STEP 1: Identify -->
    @if (step() === 1) {
      <h1>Welcome Back</h1>
      <p class="subtitle">Sign in to your secure account</p>

      @if (error()) {
        <div class="toast-inline toast-error" role="alert" aria-live="polite">{{ error() }}</div>
      }

      <div class="form-group">
        <label for="username" id="lbl-username">Username</label>
        <input id="username" type="text" [ngModel]="username()" (ngModelChange)="username.set($event)"
               placeholder="Enter your username" aria-labelledby="lbl-username" autocomplete="username">
      </div>
      <div class="form-group">
        <label for="password" id="lbl-password">Password</label>
        <input id="password" type="password" [ngModel]="password()" (ngModelChange)="password.set($event)"
               placeholder="Enter your password" aria-labelledby="lbl-password" autocomplete="current-password"
               (keyup.enter)="onLogin()">
      </div>
      <button class="btn btn-primary" style="width:100%; margin-bottom:16px"
              [disabled]="loading() || !username() || !password()" (click)="onLogin()"
              aria-label="Sign in with credentials">
        @if (loading()) { <span class="loading">⏳</span> Signing in... } @else { 🔐 Sign In }
      </button>

      <div class="divider">or continue with</div>

      <div style="display:flex; gap:10px">
        <button class="btn btn-social btn-google" (click)="loginGoogle()" aria-label="Sign in with Google">
          <span>🔴</span> Google
        </button>
        <button class="btn btn-social btn-github" (click)="loginGithub()" aria-label="Sign in with GitHub">
          <span>⚫</span> GitHub
        </button>
      </div>

      <p style="text-align:center; margin-top:20px; font-size:14px; color:var(--text-secondary)">
        Don't have an account? <a routerLink="/register">Register</a>
      </p>
    }

    <!-- STEP 2: MFA Challenge -->
    @if (step() === 2) {
      <h1>🛡️ Verify Identity</h1>
      <p class="subtitle">Complete multi-factor authentication</p>

      @if (error()) {
        <div class="toast-inline toast-error" role="alert">{{ error() }}</div>
      }

      @if (mfaMethods().length > 1) {
        <div class="form-group">
          <label for="mfa-method">Authentication Method</label>
          <select id="mfa-method" [ngModel]="selectedMethod()" (ngModelChange)="selectedMethod.set($event)">
            @for (m of mfaMethods(); track m) { <option [value]="m">{{ m }}</option> }
          </select>
        </div>
      }

      @if (selectedMethod() === 'EMAIL_OTP' || selectedMethod() === 'SMS_OTP') {
        <button class="btn btn-outline" style="width:100%; margin-bottom:16px" (click)="sendOtp()"
                aria-label="Send verification code">
          📨 Send Code via {{ selectedMethod() === 'EMAIL_OTP' ? 'Email' : 'SMS' }}
        </button>
      }

      @if (selectedMethod() === 'TOTP') {
        <p style="font-size:13px; color:var(--text-secondary); margin-bottom:12px">
          Open your Google Authenticator app and enter the 6-digit code
        </p>
      }

      <div class="form-group">
        <label for="mfa-code">Verification Code</label>
        <input id="mfa-code" type="text" [ngModel]="mfaCode()" (ngModelChange)="mfaCode.set($event)"
               placeholder="Enter 6-digit code" maxlength="6" autocomplete="one-time-code"
               aria-label="MFA verification code" (keyup.enter)="onVerifyMfa()"
               style="text-align:center; font-size:24px; letter-spacing:8px">
      </div>
      <button class="btn btn-primary" style="width:100%" [disabled]="loading() || mfaCode().length < 6"
              (click)="onVerifyMfa()" aria-label="Verify MFA code">
        @if (loading()) { <span class="loading">⏳</span> Verifying... } @else { ✅ Verify }
      </button>
      <button class="btn btn-outline" style="width:100%; margin-top:10px" (click)="step.set(1)">← Back</button>
    }
  </div>
</div>
LOGINHTML

cat > src/app/features/auth/login/login.component.scss << 'LOGINSCSS'
.toast-inline {
  padding: 10px 16px; border-radius: 8px; margin-bottom: 16px; font-size: 13px;
  background: rgba(239,68,68,0.12); color: var(--danger); border: 1px solid rgba(239,68,68,0.2);
}
LOGINSCSS

# ── OAuth Callback Component ──
cat > src/app/features/auth/login/oauth-callback.component.ts << 'OAUTHCB'
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  template: '<div class="page-container"><p class="loading" style="color:var(--text-secondary)">Authenticating...</p></div>'
})
export class OAuthCallbackComponent implements OnInit {
  constructor(private route: ActivatedRoute, private router: Router, private auth: AuthService) {}
  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) { this.auth.setToken(token); this.router.navigate(['/dashboard']); }
    else { this.router.navigate(['/login']); }
  }
}
OAUTHCB

# ── Register Component ──
cat > src/app/features/auth/register/register.component.ts << 'REGCOMP'
import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  username = signal('');
  email = signal('');
  password = signal('');
  phoneNumber = signal('');
  loading = signal(false);
  error = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onRegister(): void {
    this.loading.set(true);
    this.error.set('');
    this.authService.register({
      username: this.username(), email: this.email(),
      password: this.password(), phoneNumber: this.phoneNumber()
    }).subscribe({
      next: () => { this.loading.set(false); this.router.navigate(['/dashboard']); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Registration failed'); }
    });
  }
}
REGCOMP

cat > src/app/features/auth/register/register.component.html << 'REGHTML'
<div class="page-container">
  <div class="auth-card glass-card animate-in">
    <h1>Create Account</h1>
    <p class="subtitle">Join Passway — Secure multi-factor authentication</p>

    @if (error()) { <div class="toast-inline" style="background:rgba(239,68,68,0.12);color:var(--danger);padding:10px 16px;border-radius:8px;margin-bottom:16px;font-size:13px">{{ error() }}</div> }

    <div class="form-group">
      <label for="reg-username">Username</label>
      <input id="reg-username" type="text" [ngModel]="username()" (ngModelChange)="username.set($event)" placeholder="Choose a username" autocomplete="username" aria-label="Username">
    </div>
    <div class="form-group">
      <label for="reg-email">Email</label>
      <input id="reg-email" type="email" [ngModel]="email()" (ngModelChange)="email.set($event)" placeholder="your@email.com" autocomplete="email" aria-label="Email address">
    </div>
    <div class="form-group">
      <label for="reg-phone">Phone Number (optional, for SMS OTP)</label>
      <input id="reg-phone" type="tel" [ngModel]="phoneNumber()" (ngModelChange)="phoneNumber.set($event)" placeholder="+1234567890" autocomplete="tel" aria-label="Phone number">
    </div>
    <div class="form-group">
      <label for="reg-password">Password</label>
      <input id="reg-password" type="password" [ngModel]="password()" (ngModelChange)="password.set($event)" placeholder="Min 8 characters" autocomplete="new-password" aria-label="Password">
    </div>
    <button class="btn btn-primary" style="width:100%" [disabled]="loading() || !username() || !email() || !password()" (click)="onRegister()" aria-label="Create account">
      @if (loading()) { <span class="loading">⏳</span> Creating... } @else { 🚀 Create Account }
    </button>
    <p style="text-align:center;margin-top:20px;font-size:14px;color:var(--text-secondary)">
      Already have an account? <a routerLink="/login">Sign In</a>
    </p>
  </div>
</div>
REGHTML

cat > src/app/features/auth/register/register.component.scss << 'REGSCSS'
:host { display: block; }
REGSCSS

# ── MFA Challenge Component ──
cat > src/app/features/auth/mfa/mfa-challenge.component.ts << 'MFACHAL'
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
MFACHAL

# ── MFA Setup Component ──
cat > src/app/features/auth/mfa/mfa-setup.component.ts << 'MFASETUP'
import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

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
        <div class="mfa-option" role="button" tabindex="0" aria-label="Set up WebAuthn passkey" style="opacity:0.6">
          <div class="icon">🔑</div>
          <div class="info"><h3>Passkey / WebAuthn</h3><p>Biometric or security key (Coming Soon)</p></div>
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

  goBack(): void { this.router.navigate(['/dashboard']); }
}
MFASETUP

# ── Dashboard Component ──
cat > src/app/features/dashboard/dashboard.component.ts << 'DASHCOMP'
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
DASHCOMP

# ── Create public directory placeholder ──
mkdir -p public
echo '{}' > public/manifest.json

# ── Install dependencies ──
echo ""
echo "📦 Installing npm dependencies..."
npm install

echo ""
echo "✅ Angular 21 MFA Frontend created at: $PROJECT_DIR"
echo "   Run: cd $PROJECT_DIR && npm start"
echo "   Backend API proxy configured for http://localhost:8080"
