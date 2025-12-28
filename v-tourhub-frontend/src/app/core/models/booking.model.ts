// Booking response shape as returned by backend `BookingResponse`
export interface Booking {
    bookingId: number;
    status: 'INITIATED' | 'PENDING_PAYMENT' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED';
    serviceId: number;
    serviceName: string;
    totalPrice: number;
    expiresAt?: string; // ISO datetime string
    isPaymentReady: boolean;
    paymentUrl?: string | null;

    // Entity fields that might be returned by specific ID endpoints (Admin)
    // but are NOT in the standard BookingResponse list.
    // Making them optional to support both response types.
    checkInDate?: string;
    checkOutDate?: string;
    guests?: number;
    quantity?: number;
    customerName?: string | null;
    customerEmail?: string | null;
    customerPhone?: string | null;
    inventoryLockToken?: string;
    cancelledAt?: string;
    cancellationReason?: string;
}

// Request used to create a booking
export interface BookingCreateRequest {
    serviceId: number;
    checkInDate: string;
    checkOutDate?: string; // Optional but used for ranges
    guests: number;
    quantity?: number;
    customerName?: string;
    customerEmail?: string;
    customerPhone?: string;
}
