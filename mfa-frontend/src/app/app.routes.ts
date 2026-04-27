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
