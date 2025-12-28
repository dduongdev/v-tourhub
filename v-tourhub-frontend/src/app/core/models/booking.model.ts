export interface Booking {
    bookingId: number;
    serviceName: string;
    totalPrice: number;
    status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';
    expiresAt: string;
    checkInDate: string;
    checkOutDate?: string;
    numberOfGuests: number;
    quantity: number;
    customerName: string;
    customerEmail: string;
    customerPhone: string;
}

export interface BookingCreateRequest {
    serviceId: number;
    checkInDate: string;
    checkOutDate?: string;
    numberOfGuests: number;
    quantity: number;
    customerName: string;
    customerEmail: string;
    customerPhone: string;
}
