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
