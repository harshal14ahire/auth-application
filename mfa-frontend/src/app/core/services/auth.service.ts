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

  sendLoginOtp(mfaToken: string, method: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/mfa/otp/send`, { mfaToken, method });
  }

  webAuthnLoginOptions(mfaToken: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/mfa/webauthn/options`, { mfaToken, method: 'WEBAUTHN' });
  }

  enableOtp(method: string, code: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/mfa/otp/enable?code=${code}`, { method });
  }

  getMfaMethods(): Observable<ApiResponse> {
    return this.http.get<ApiResponse>(`${this.apiUrl}/mfa/methods`);
  }

  webAuthnRegisterOptions(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/mfa/webauthn/register/options`, {});
  }

  webAuthnRegisterVerify(deviceName: string, attestationObject: string, clientDataJSON: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/mfa/webauthn/register/verify`, {
      deviceName, attestationObject, clientDataJSON
    });
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
