import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  imports: [CommonModule],
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.scss'
})
export class StatusBadgeComponent {
  @Input() status!: string;

  get badgeClass(): string {
    switch (this.status) {
      case 'INITIATED': return 'bg-info';
      case 'PENDING_PAYMENT': return 'bg-warning';
      case 'CONFIRMED': return 'bg-success';
      case 'COMPLETED': return 'bg-primary';
      case 'CANCELLED': return 'bg-danger';
      case 'REFUNDED': return 'bg-dark';
      default: return 'bg-secondary';
    }
  }
}
