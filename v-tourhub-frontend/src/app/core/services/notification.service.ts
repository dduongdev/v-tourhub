import { Injectable } from '@angular/core';

export type NotificationType = 'success' | 'error' | 'info' | 'warning';

@Injectable({
    providedIn: 'root'
})
export class NotificationService {
    show(message: string, type: NotificationType = 'info', duration: number = 3000): void {
        // Simple implementation - can be enhanced with Angular Material Snackbar later
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 16px 24px;
      background: ${this.getBackgroundColor(type)};
      color: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      z-index: 9999;
      animation: slideIn 0.3s ease-out;
    `;

        document.body.appendChild(notification);

        setTimeout(() => {
            notification.style.animation = 'slideOut 0.3s ease-in';
            setTimeout(() => notification.remove(), 300);
        }, duration);
    }

    success(message: string): void {
        this.show(message, 'success');
    }

    error(message: string): void {
        this.show(message, 'error', 5000);
    }

    info(message: string): void {
        this.show(message, 'info');
    }

    warning(message: string): void {
        this.show(message, 'warning');
    }

    private getBackgroundColor(type: NotificationType): string {
        switch (type) {
            case 'success': return '#22c55e';
            case 'error': return '#ef4444';
            case 'warning': return '#f59e0b';
            case 'info': return '#0ea5e9';
            default: return '#64748b';
        }
    }
}
