import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/api/api.service';
import { Booking } from '../../../core/models/booking.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge';
import { CountdownTimerComponent } from '../../../shared/components/countdown-timer/countdown-timer';

@Component({
  selector: 'app-my-bookings',
  imports: [CommonModule, RouterLink, StatusBadgeComponent, CountdownTimerComponent],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.scss'
})
export class MyBookingsComponent implements OnInit, OnDestroy {
  bookings: Booking[] = [];
  loading = false;
  private pollInterval: any;

  constructor(private apiService: ApiService, private cd: ChangeDetectorRef) { }

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
    this.apiService.getMyBookings().pipe(finalize(() => {
      this.loading = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (bookings) => {
        this.bookings = bookings;
      },
      error: () => console.error('my-bookings load error')
    });
  }

  pay(bookingId: number): void {
    this.apiService.createVnPayUrl(bookingId).subscribe({
      next: (url) => {
        if (url) window.location.href = url;
      },
      error: (err) => console.error('Failed to get payment URL', err)
    });
  }
}
