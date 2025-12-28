import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container mt-5 text-center">
      <h1>Access Denied</h1>
      <p>You do not have permission to view this page.</p>
      <a routerLink="/" class="btn btn-primary">Go Home</a>
    </div>
  `,
  styles: [`.container { max-width: 720px; }`]
})
export class AccessDeniedComponent {}
