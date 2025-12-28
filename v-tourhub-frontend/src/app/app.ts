import { Component, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { ThemeService } from './core/services/theme';
import { NavbarComponent } from './shared/components/navbar/navbar';
@Component({
  selector: 'app-root',
  standalone: true, // Đánh dấu là Standalone
  imports: [CommonModule, RouterOutlet, NavbarComponent], // Import các dependency needed
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class AppComponent {
  constructor(public authService: AuthService, private theme: ThemeService) {
    this.theme.init();
  }

  ngAfterViewInit(): void {
    // Measure header height and set body padding/spacer to match so content isn't covered
    setTimeout(() => {
      try {
        const hdr = document.querySelector('header');
        const spacer = document.querySelector('.app-header-spacer') as HTMLElement | null;
        const isAdmin = this.authService.isAdmin();
        const h = (hdr && hdr.clientHeight && !isAdmin) ? hdr.clientHeight : 0;
        document.body.style.paddingTop = `${h}px`;
        if (spacer) spacer.style.height = `${h}px`;
      } catch (e) {
        // ignore
      }
    }, 0);
  }
}

// Export App alias used by main bootstrap
export const App = AppComponent;