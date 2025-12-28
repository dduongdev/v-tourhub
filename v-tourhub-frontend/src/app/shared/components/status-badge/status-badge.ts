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
      case 'PENDING': return 'bg-primary';
      case 'CONFIRMED': return 'bg-success';
      case 'CANCELLED': return 'bg-danger';
      case 'EXPIRED': return 'bg-secondary';
      default: return 'bg-secondary';
    }
  }
}
