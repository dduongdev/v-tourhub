import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-booking',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './booking.html',
  styleUrl: './booking.scss'
})
export class BookingComponent implements OnInit {
  bookingForm: FormGroup;
  submitting = false;
  serviceId: number | null = null;
  service: any = null;
  isHotel = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private notification: NotificationService
  ) {
    this.bookingForm = this.fb.group({
      checkInDate: ['', Validators.required],
      checkOutDate: [''], // Optional depending on service type
      guests: [1, [Validators.required, Validators.min(1)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      customerName: ['', Validators.required],
      customerEmail: ['', [Validators.required, Validators.email]],
      customerPhone: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.serviceId = params['serviceId'] ? +params['serviceId'] : null;
      if (params['checkInDate']) {
        this.bookingForm.patchValue({ checkInDate: params['checkInDate'] });
      }

      if (this.serviceId) {
        this.loadServiceDetails(this.serviceId);
      }
    });

    // Auto-fill user details
    this.apiService.getUserProfile().subscribe({
      next: (user) => {
        if (user) {
          const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ');
          this.bookingForm.patchValue({
            customerName: fullName || user.email,
            customerEmail: user.email,
            customerPhone: user.phone
          });
        }
      },
      error: () => console.log('Could not fetch user profile for auto-fill')
    });
  }

  loadServiceDetails(id: number): void {
    this.apiService.getServiceById(id).subscribe({
      next: (service) => {
        this.service = service;
        this.isHotel = service.type === 'HOTEL';

        const checkOutCtrl = this.bookingForm.get('checkOutDate');
        if (this.isHotel) {
          checkOutCtrl?.setValidators([Validators.required]);
        } else {
          checkOutCtrl?.clearValidators();
          checkOutCtrl?.setValue(null);
        }
        checkOutCtrl?.updateValueAndValidity();
      },
      error: (err) => console.error('Failed to load service details for booking', err)
    });
  }

  onSubmit(): void {
    if (this.bookingForm.valid && this.serviceId) {
      this.submitting = true;
      const booking = {
        serviceId: this.serviceId,
        ...this.bookingForm.value
      };

      this.apiService.createBooking(booking).subscribe({
        next: (response) => {
          this.notification.success('Booking created successfully!');
          this.router.navigate(['/my-bookings']);
        },
        error: (err) => {
          this.notification.error('Failed to create booking: ' + (err.error?.message || 'Unknown error'));
          this.submitting = false;
        }
      });
    }
  }
}
