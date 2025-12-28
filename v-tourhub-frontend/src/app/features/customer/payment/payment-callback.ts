import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';

@Component({
  selector: 'app-payment-callback',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container py-5 mt-5">
      <div class="row justify-content-center">
        <div class="col-md-6 col-lg-5">
          <div class="card shadow-sm border-0 text-center p-4">
            
            <!-- Loading State -->
            <div *ngIf="loading" class="py-5">
              <div class="spinner-border text-primary mb-3" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
              <h4 class="mb-2">Processing Payment...</h4>
              <p class="text-muted">Please wait while we connect to the payment gateway.</p>
            </div>

            <!-- Success State -->
            <div *ngIf="!loading && status === 'success'" class="py-4">
              <div class="mb-3">
                <i class="bi bi-check-circle-fill text-success" style="font-size: 4rem;"></i>
              </div>
              <h3 class="mb-3">Payment Successful!</h3>
              <p class="text-muted mb-4">Your booking has been confirmed. Thank you for choosing us.</p>
              <div class="d-grid gap-2">
                <a routerLink="/my-bookings" class="btn btn-primary">View My Bookings</a>
                <a routerLink="/explore" class="btn btn-outline-secondary">Explore More</a>
              </div>
            </div>

            <!-- Failure State -->
            <div *ngIf="!loading && status === 'failed'" class="py-4">
              <div class="mb-3">
                <i class="bi bi-x-circle-fill text-danger" style="font-size: 4rem;"></i>
              </div>
              <h3 class="mb-3">Payment Failed</h3>
              <p class="text-muted mb-4">We couldn't process your payment. Please try again or contact support.</p>
              <div class="d-grid gap-2">
                <button (click)="retryPayment()" class="btn btn-primary">Retry Payment</button>
                <a routerLink="/my-bookings" class="btn btn-outline-secondary">Go to My Bookings</a>
              </div>
            </div>

            <!-- Init Error State -->
            <div *ngIf="!loading && error" class="py-4">
               <div class="mb-3">
                <i class="bi bi-exclamation-triangle-fill text-warning" style="font-size: 4rem;"></i>
              </div>
              <h3 class="mb-3">Something went wrong</h3>
              <p class="text-muted mb-4">{{ error }}</p>
              <a routerLink="/explore" class="btn btn-primary">Go Home</a>
            </div>

          </div>
        </div>
      </div>
    </div>
  `
})
export class PaymentCallbackComponent implements OnInit {
  loading = true;
  status: 'success' | 'failed' | null = null;
  bookingId: number | null = null;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService
  ) { }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      console.log('PaymentCallback: Query Params:', params);
      this.loading = true;
      this.error = null;
      this.status = params['status'] || null;
      const bId = params['bookingId'];
      this.bookingId = bId && bId.trim() ? +bId : null;

      console.log('PaymentCallback: status=', this.status, 'bookingId=', this.bookingId);

      if (this.status) {
        // Callback from Payment Gateway
        this.handleCallback();
      } else if (this.bookingId && this.bookingId > 0) {
        // Request to initiate payment
        this.initiatePayment(this.bookingId);
      } else {
        // Invalid state
        this.loading = false;
        this.error = 'Invalid payment request. Please provide a valid Booking ID.';
      }
    });
  }

  handleCallback() {
    // Just display the result. 
    // Backend creates the redirect URL with status=success/failed, so the state is already determined.
    // We might want to refresh booking status if needed, but display is sufficient for now.
    this.loading = false;
  }

  initiatePayment(id: number) {
    console.log('PaymentCallback: Initiating payment for booking:', id);
    this.api.createVnPayUrl(id).subscribe({
      next: (url) => {
        console.log('PaymentCallback: Received VNPay URL:', url);
        if (url) {
          // Redirect to VNPay
          window.location.href = url;
        } else {
          this.loading = false;
          this.error = 'Could not generate payment URL. The server returned an empty response.';
        }
      },
      error: (err) => {
        console.error('Payment Init Error:', err);
        this.loading = false;
        const errorMsg = err?.error?.message || err?.message || 'Unknown error';
        this.error = `Failed to initiate payment: ${errorMsg}`;
      }
    });
  }

  retryPayment() {
    if (this.bookingId) {
      this.loading = true;
      this.error = null;
      this.initiatePayment(this.bookingId);
    } else {
      // If we don't have booking ID in URL for some reason (e.g. just status=failed), redirect to my bookings
      this.router.navigate(['/my-bookings']);
    }
  }
}
