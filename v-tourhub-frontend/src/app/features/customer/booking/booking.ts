import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-booking',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './booking.html',
  styleUrl: './booking.scss'
})
export class BookingComponent implements OnInit {
  bookingForm: FormGroup;
  submitting = false;
  serviceId: number | null = null;
  service: any = null;
  isHotel = false;

  // Inventory State
  inventoryList: any[] = [];
  loadingInventory = false;
  loadingMore = false;
  startDate: Date = new Date();
  endDate: Date = new Date();

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private notification: NotificationService,
    private cd: ChangeDetectorRef
  ) {
    this.bookingForm = this.fb.group({
      checkInDate: ['', Validators.required],
      checkOutDate: [''],
      guests: [1, [Validators.required, Validators.min(1)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      customerName: ['', Validators.required],
      customerEmail: ['', [Validators.required, Validators.email]],
      customerPhone: ['', Validators.required]
    });

    // Initialize endDate to today + 14 days
    this.endDate.setDate(this.startDate.getDate() + 14);

    // Sync guests = quantity for non-hotel services
    this.bookingForm.get('guests')?.valueChanges.subscribe(value => {
      if (!this.isHotel && value) {
        this.bookingForm.patchValue({ quantity: value }, { emitEvent: false });
      }
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.serviceId = params['serviceId'] ? +params['serviceId'] : null;
      if (params['checkInDate']) {
        this.bookingForm.patchValue({ checkInDate: params['checkInDate'] });
      }

      if (this.serviceId) {
        this.loadServiceDetails(this.serviceId, true);
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

  loadServiceDetails(id: number, reset: boolean = false): void {
    if (reset) {
      this.loadingInventory = true;
      this.inventoryList = [];
      this.startDate = new Date();
      this.endDate = new Date();
      this.endDate.setDate(this.startDate.getDate() + 14);
    } else {
      this.loadingMore = true;
      this.startDate = new Date(this.endDate);
      this.startDate.setDate(this.startDate.getDate() + 1);
      this.endDate = new Date(this.startDate);
      this.endDate.setDate(this.endDate.getDate() + 14);
    }

    const startStr = this.formatDate(this.startDate);
    const endStr = this.formatDate(this.endDate);

    this.apiService.getServiceById(id, startStr, endStr)
      .pipe(finalize(() => {
        this.loadingInventory = false;
        this.loadingMore = false;
        this.cd.detectChanges();
      }))
      .subscribe({
        next: (service) => {
          this.service = service;
          this.isHotel = service.type === 'HOTEL';

          // Update inventory
          if (reset) {
            this.inventoryList = (service.inventoryCalendar || []).filter((inv: any) => inv.availableStock > 0);
          } else {
            if (service.inventoryCalendar) {
              const newItems = service.inventoryCalendar.filter((inv: any) => inv.availableStock > 0);
              this.inventoryList = [...this.inventoryList, ...newItems];
            }
          }

          const checkOutCtrl = this.bookingForm.get('checkOutDate');
          if (this.isHotel) {
            checkOutCtrl?.setValidators([Validators.required]);
          } else {
            checkOutCtrl?.clearValidators();
            checkOutCtrl?.setValue(null);
            // Sync guests = quantity for non-hotel
            const guests = this.bookingForm.get('guests')?.value || 1;
            this.bookingForm.patchValue({ quantity: guests }, { emitEvent: false });
          }
          checkOutCtrl?.updateValueAndValidity();
          this.cd.detectChanges();
        },
        error: (err) => console.error('Failed to load service details for booking', err)
      });
  }

  loadMore(): void {
    if (this.serviceId) {
      this.loadServiceDetails(this.serviceId, false);
    }
  }

  selectDate(date: string): void {
    this.bookingForm.patchValue({ checkInDate: date });
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
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
