import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  constructor() {}

  init(): void {
    // Minimal theme initialization: read preferred theme from localStorage
    try {
      const theme = localStorage.getItem('app_theme') || 'light';
      document.documentElement.setAttribute('data-theme', theme);
    } catch (e) {
      // ignore
    }
  }
}
