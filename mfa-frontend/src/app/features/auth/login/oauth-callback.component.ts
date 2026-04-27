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
