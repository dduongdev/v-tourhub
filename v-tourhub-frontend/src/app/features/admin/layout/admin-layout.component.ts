import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-admin-layout',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="d-flex" style="min-height: 100vh;">
      <!-- Sidebar -->
      <div class="d-flex flex-column flex-shrink-0 p-3 text-white bg-dark" style="width: 280px;">
        <a href="/" class="d-flex align-items-center mb-3 mb-md-0 me-md-auto text-white text-decoration-none">
          <span class="fs-4 fw-bold">v-tourhub Admin</span>
        </a>
        <hr>
        <ul class="nav nav-pills flex-column mb-auto">
          <li class="nav-item">
            <a routerLink="/admin/dashboard" routerLinkActive="active" class="nav-link text-white">
              <i class="bi bi-speedometer2 me-2"></i>
              Dashboard
            </a>
          </li>
          <li>
            <a routerLink="/admin/bookings" routerLinkActive="active" class="nav-link text-white" style="cursor: pointer;">
              <i class="bi bi-calendar-check me-2"></i>
              Bookings
            </a>
          </li>
          <li>
            <a routerLink="/admin/destinations" routerLinkActive="active" class="nav-link text-white">
              <i class="bi bi-map me-2"></i>
              Destinations
            </a>
          </li>
        </ul>
        <hr>
        <div class="dropdown">
          <a href="#" class="d-flex align-items-center text-white text-decoration-none dropdown-toggle" id="dropdownUser1" data-bs-toggle="dropdown" aria-expanded="false">
            <img src="https://github.com/mdo.png" alt="" width="32" height="32" class="rounded-circle me-2">
            <strong>{{ userName }}</strong>
          </a>
          <ul class="dropdown-menu dropdown-menu-dark text-small shadow" aria-labelledby="dropdownUser1">
            <li><a class="dropdown-item" href="/">Go to Customer Site</a></li>
            <li><hr class="dropdown-divider"></li>
            <li><a class="dropdown-item" href="#" (click)="logout($event)">Sign out</a></li>
          </ul>
        </div>
      </div>

      <!-- Main Content -->
      <div class="flex-grow-1 bg-light">
        <div class="container-fluid p-4">
            <router-outlet></router-outlet>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .nav-link.active {
        background-color: #0d6efd;
    }
    .nav-link:hover:not(.active) {
        background-color: rgba(255,255,255,0.1);
    }
  `]
})
export class AdminLayoutComponent {
  userName: string = 'Admin';

  constructor(private authService: AuthService) {
    this.userName = this.authService.getUserName();
  }

  logout(event: Event) {
    event.preventDefault();
    this.authService.logout();
  }
}
