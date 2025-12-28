import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Booking, BookingCreateRequest } from '../models/booking.model';
import { Destination } from '../models/destination.model';
import { Service } from '../models/service.model';
import { User } from '../models/user.model';
import { Page } from '../models/pagination.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
    providedIn: 'root'
})
export class ApiService {
    private readonly apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    private toAbsoluteUrl(path?: string | null): string | null | undefined {
        if (!path) return path;
        const s = path.trim();
        if (s.startsWith('http://') || s.startsWith('https://') || s.startsWith('//')) return s;
        // Remove trailing /api if present to get backend base
        const base = this.apiUrl.replace(/\/api\/?$/i, '');
        return (s.startsWith('/') ? base + s : base + '/' + s);
    }

    private normalizeBooking(b?: Booking | null): Booking | null | undefined {
        if (!b) return b;
        if ((b as any).paymentUrl) {
            (b as any).paymentUrl = this.toAbsoluteUrl((b as any).paymentUrl) as string;
        }
        return b;
    }

    private unwrap<T>(): (source: Observable<ApiResponse<T>>) => Observable<T> {
        return (source: Observable<ApiResponse<T>>) => source.pipe(
            map(response => {
                if (response.status !== 200 && response.status !== 201) {
                    // Optional: Throw error if status is not success, though HTTP error handler might catch non-2xx
                }
                return response.data;
            })
        );
    }

    // ========== CATALOG SERVICE ==========

    getDestinations(page: number = 0, size: number = 20): Observable<Page<Destination>> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<ApiResponse<Page<Destination> | Destination[]>>(`${this.apiUrl}/catalog/destinations`, { params })
            .pipe(this.unwrap(), map(result => {
                // Support both Page<Destination> and unwrapped Destination[]
                if (!result) return { content: [], totalElements: 0, totalPages: 0, size: size, number: page } as Page<Destination>;
                if (Array.isArray(result)) {
                    return { content: result, totalElements: result.length, totalPages: Math.ceil(result.length / size), size: size, number: page } as Page<Destination>;
                }
                // assume Page-like
                return result as Page<Destination>;
            }));
    }

    getDestinationById(id: number): Observable<Destination> {
        return this.http.get<ApiResponse<Destination>>(`${this.apiUrl}/catalog/destinations/${id}`)
            .pipe(this.unwrap());
    }

    getServicesForDestination(destinationId: number): Observable<Service[]> {
        return this.http.get<ApiResponse<Service[]>>(`${this.apiUrl}/catalog/destinations/${destinationId}/services`)
            .pipe(this.unwrap());
    }

    createDestination(destination: Partial<Destination>): Observable<Destination> {
        return this.http.post<ApiResponse<Destination>>(`${this.apiUrl}/catalog/destinations`, destination)
            .pipe(this.unwrap());
    }

    updateDestination(id: number, destination: Partial<Destination>): Observable<Destination> {
        return this.http.put<ApiResponse<Destination>>(`${this.apiUrl}/catalog/destinations/${id}`, destination)
            .pipe(this.unwrap());
    }

    deleteDestination(id: number): Observable<void> {
        return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/catalog/destinations/${id}`)
            .pipe(this.unwrap());
    }

    getServiceById(id: number, startDate?: string, endDate?: string): Observable<Service> {
        let params = new HttpParams();
        if (startDate) params = params.set('startDate', startDate);
        if (endDate) params = params.set('endDate', endDate);

        return this.http.get<ApiResponse<Service>>(`${this.apiUrl}/catalog/services/${id}`, { params })
            .pipe(
                tap(res => console.log('ApiService raw response:', res)),
                this.unwrap()
            );
    }

    getServicesByType(type: string, location?: string, page: number = 0, size: number = 10): Observable<Page<Service>> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (location) {
            params = params.set('location', location);
        }

        return this.http.get<ApiResponse<Page<Service>>>(`${this.apiUrl}/catalog/services/type/${type}`, { params })
            .pipe(this.unwrap());
    }

    createService(service: Partial<Service>): Observable<Service> {
        // Backend requires destinationId when creating a service: POST /catalog/destinations/{id}/services
        const destId = (service as any)?.destination?.id;
        if (!destId) {
            throw new Error('createService requires destinationId on service.destination.id');
        }
        return this.addServiceToDestination(destId, service);
    }

    updateService(id: number, service: Partial<Service>): Observable<Service> {
        return this.http.put<ApiResponse<Service>>(`${this.apiUrl}/catalog/services/${id}`, service)
            .pipe(this.unwrap());
    }

    addServiceToDestination(destinationId: number, service: Partial<Service>): Observable<Service> {
        // Sanitize payload to match CreateServiceRequest DTO
        const payload = {
            name: service.name,
            description: service.description,
            price: service.price,
            type: service.type,
            availability: service.availability,
            attributes: service.attributes,
            // inventory is optional in DTO but not in UI form yet (handled separately), so omit or null
        };
        return this.http.post<ApiResponse<Service>>(`${this.apiUrl}/catalog/destinations/${destinationId}/services`, payload)
            .pipe(this.unwrap());
    }

    // ========== UPLOADS & MEDIA ==========

    uploadAvatar(form: FormData): Observable<string> {
        return this.http.post<ApiResponse<string>>(`${this.apiUrl}/users/avatar`, form)
            .pipe(this.unwrap());
    }

    uploadDestinationMedia(destinationId: number, form: FormData): Observable<string> {
        return this.http.post<ApiResponse<string>>(`${this.apiUrl}/catalog/destinations/${destinationId}/media`, form)
            .pipe(this.unwrap());
    }

    uploadServiceMedia(serviceId: number, form: FormData): Observable<string> {
        return this.http.post<ApiResponse<string>>(`${this.apiUrl}/catalog/services/${serviceId}/media`, form)
            .pipe(this.unwrap());
    }

    uploadDestinationMediaBatch(destinationId: number, form: FormData): Observable<string[]> {
        return this.http.post<ApiResponse<string[]>>(`${this.apiUrl}/catalog/destinations/${destinationId}/media/batch`, form)
            .pipe(this.unwrap());
    }

    uploadServiceMediaBatch(serviceId: number, form: FormData): Observable<string[]> {
        return this.http.post<ApiResponse<string[]>>(`${this.apiUrl}/catalog/services/${serviceId}/media/batch`, form)
            .pipe(this.unwrap());
    }

    deleteMedia(mediaId: number): Observable<void> {
        return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/catalog/media/${mediaId}`)
            .pipe(this.unwrap());
    }

    // ========== INVENTORY ==========

    setupInventory(serviceId: number, total: number, start: string, end: string): Observable<void> {
        return this.http.post<ApiResponse<void>>(`${this.apiUrl}/catalog/services/${serviceId}/inventory?total=${total}&start=${start}&end=${end}`, null)
            .pipe(this.unwrap());
    }

    updateInventory(inventoryId: number, newTotalStock: number): Observable<any> {
        return this.http.put<ApiResponse<any>>(`${this.apiUrl}/catalog/inventory/${inventoryId}?newTotalStock=${newTotalStock}`, null)
            .pipe(this.unwrap());
    }

    // ========== BOOKING SERVICE ==========

    // Backend @GetMapping public ApiResponse<List<BookingResponse>> getAllBookings()
    getAllBookings(page: number = 0, size: number = 20): Observable<Page<Booking>> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString())
            .set('sort', 'createdAt')
            .set('direction', 'desc');

        return this.http.get<ApiResponse<Page<Booking> | Booking[]>>(`${this.apiUrl}/bookings`, { params })
            .pipe(this.unwrap(), map(result => {
                if (Array.isArray(result)) {
                    // Fallback if backend not paginated yet (though I just updated it)
                    return { content: result.map(b => this.normalizeBooking(b) as Booking), totalElements: result.length, totalPages: 1, size: size, number: page } as Page<Booking>;
                }
                // Handle Page
                const pageResult = result as Page<Booking>;
                pageResult.content = pageResult.content.map(b => this.normalizeBooking(b) as Booking);
                return pageResult;
            }));
    }

    getMyBookings(): Observable<Booking[]> {
        return this.http.get<ApiResponse<Booking[]>>(`${this.apiUrl}/bookings/my-bookings`)
            .pipe(this.unwrap(), map(list => Array.isArray(list) ? list.map(b => this.normalizeBooking(b) as Booking) : list));
    }

    createBooking(booking: BookingCreateRequest): Observable<Booking> {
        return this.http.post<ApiResponse<Booking>>(`${this.apiUrl}/bookings`, booking)
            .pipe(this.unwrap(), map(b => this.normalizeBooking(b) as Booking));
    }

    // cancelBooking expects PUT /bookings/{id}/cancel
    cancelBooking(id: number): Observable<void> {
        return this.http.put<ApiResponse<void>>(`${this.apiUrl}/bookings/${id}/cancel`, {})
            .pipe(this.unwrap());
    }

    createVnPayUrl(bookingId: number): Observable<string> {
        return this.http.post<ApiResponse<string>>(`${this.apiUrl}/payments/vnpay/url/${bookingId}`, {})
            .pipe(this.unwrap());
    }

    // ========== USER PROFILE SERVICE ==========

    getUserProfile(): Observable<User> {
        return this.http.get<ApiResponse<User>>(`${this.apiUrl}/users/profile`)
            .pipe(this.unwrap());
    }

    updateUserProfile(user: Partial<User>): Observable<User> {
        return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/profile`, user)
            .pipe(this.unwrap());
    }
}
