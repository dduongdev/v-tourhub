import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Booking, BookingCreateRequest } from '../models/booking.model';
import { Destination } from '../models/destination.model';
import { Service } from '../models/service.model';
import { User } from '../models/user.model';
import { Page } from '../models/pagination.model';

@Injectable({
    providedIn: 'root'
})
export class ApiService {
    private readonly apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    // ========== CATALOG SERVICE ==========

    getDestinations(page: number = 0, size: number = 20): Observable<Page<Destination>> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<Page<Destination>>(`${this.apiUrl}/catalog/destinations`, { params });
    }

    getDestinationById(id: number): Observable<Destination> {
        return this.http.get<Destination>(`${this.apiUrl}/catalog/destinations/${id}`);
    }

    createDestination(destination: Partial<Destination>): Observable<Destination> {
        return this.http.post<Destination>(`${this.apiUrl}/catalog/destinations`, destination);
    }

    updateDestination(id: number, destination: Partial<Destination>): Observable<Destination> {
        return this.http.put<Destination>(`${this.apiUrl}/catalog/destinations/${id}`, destination);
    }

    deleteDestination(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/catalog/destinations/${id}`);
    }

    getServiceById(id: number): Observable<Service> {
        return this.http.get<Service>(`${this.apiUrl}/catalog/services/${id}`);
    }

    getServicesByType(type: string, page: number = 0, size: number = 10): Observable<Page<Service>> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<Page<Service>>(`${this.apiUrl}/catalog/services/type/${type}`, { params });
    }

    createService(service: Partial<Service>): Observable<Service> {
        return this.http.post<Service>(`${this.apiUrl}/catalog/services`, service);
    }

    updateService(id: number, service: Partial<Service>): Observable<Service> {
        return this.http.put<Service>(`${this.apiUrl}/catalog/services/${id}`, service);
    }

    addServiceToDestination(destinationId: number, service: Partial<Service>): Observable<Service> {
        return this.http.post<Service>(`${this.apiUrl}/catalog/destinations/${destinationId}/services`, service);
    }

    // ========== BOOKING SERVICE ==========

    getAllBookings(filters?: {
        status?: string;
        fromDate?: string;
        toDate?: string;
        page?: number;
        size?: number;
    }): Observable<Page<Booking>> {
        let params = new HttpParams()
            .set('page', (filters?.page || 0).toString())
            .set('size', (filters?.size || 20).toString());

        if (filters?.status) params = params.set('status', filters.status);
        if (filters?.fromDate) params = params.set('fromDate', filters.fromDate);
        if (filters?.toDate) params = params.set('toDate', filters.toDate);

        return this.http.get<Page<Booking>>(`${this.apiUrl}/bookings`, { params });
    }

    getMyBookings(): Observable<Booking[]> {
        return this.http.get<Booking[]>(`${this.apiUrl}/bookings/my-bookings`);
    }

    createBooking(booking: BookingCreateRequest): Observable<Booking> {
        return this.http.post<Booking>(`${this.apiUrl}/bookings`, booking);
    }

    updateBookingStatus(id: number, status: string): Observable<Booking> {
        return this.http.put<Booking>(`${this.apiUrl}/bookings/${id}/status`, { status });
    }

    cancelBooking(id: number): Observable<Booking> {
        return this.updateBookingStatus(id, 'CANCELLED');
    }

    // ========== USER PROFILE SERVICE ==========

    getUserProfile(): Observable<User> {
        return this.http.get<User>(`${this.apiUrl}/users/profile`);
    }

    updateUserProfile(user: Partial<User>): Observable<User> {
        return this.http.put<User>(`${this.apiUrl}/users/profile`, user);
    }
}
