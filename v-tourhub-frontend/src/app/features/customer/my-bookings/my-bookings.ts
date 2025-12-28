import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/api/api.service';
import { Booking } from '../../../core/models/booking.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge';
import { CountdownTimerComponent } from '../../../shared/components/countdown-timer/countdown-timer';

@Component({
  selector: 'app-my-bookings',
  imports: [CommonModule, StatusBadgeComponent, CountdownTimerComponent],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.scss'
})
export class MyBookingsComponent implements OnInit, OnDestroy {
  bookings: Booking[] = [];
  loading = false;
  private pollInterval: any;

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.loadBookings();
    // Poll every 10 seconds for status updates
    this.pollInterval = setInterval(() => this.loadBookings(), 10000);
  }

  ngOnDestroy(): void {
    if (this.pollInterval) clearInterval(this.pollInterval);
  }

  loadBookings(): void {
    this.loading = true;
    this.apiService.getMyBookings().subscribe({
      next: (bookings) => {
        this.bookings = bookings;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}
