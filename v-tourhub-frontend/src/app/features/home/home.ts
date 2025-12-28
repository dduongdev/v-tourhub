import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class HomeComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Auto-redirect based on role and saved URL
    const isAuth = this.isAuthenticated();
    console.log('HomeComponent params check. Auth:', isAuth);
    if (isAuth) {
      // First check if there's a saved redirect URL from auth guard
      const savedUrl = this.authService.getRedirectUrlAfterLogin();
      if (savedUrl && savedUrl !== '/') {
        console.log('HomeComponent: Redirecting to saved URL:', savedUrl);
        this.router.navigateByUrl(savedUrl);
        return;
      }

      // Otherwise, redirect based on role
      console.log('Is Admin?', this.authService.isAdmin());
      if (this.authService.isAdmin()) {
        this.router.navigate(['/admin']);
      } else {
        this.router.navigate(['/explore']);
      }
    }
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  login(): void {
    this.authService.login(); // Let initAuth or Home/Guard handle redirection based on role
  }
}
