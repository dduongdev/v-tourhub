import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    // Clone request and add authorization header
    const token = authService.getAccessToken();
    let authReq = req;

    if (token && !req.url.includes('keycloak')) {
        authReq = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });
    }

    // Add X-User-Id header when user identity is available and request targets our API only
    try {
        const current = authService.getCurrentUser();
        const userId = current?.sub || current?.userId || null;
        const isApiRequest = typeof environment.apiUrl === 'string'
            ? req.url.startsWith(environment.apiUrl) || req.url.startsWith('/api')
            : req.url.startsWith('/api');
        if (userId && isApiRequest && !authReq.headers.has('X-User-Id')) {
            authReq = authReq.clone({ setHeaders: { 'X-User-Id': String(userId) } });
        }
    } catch (e) {
        // ignore
    }

    // Handle response
    return next(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
            if (error.status === 401) {
                // Unauthorized - redirect to login
                authService.logout();
            } else if (error.status === 403) {
                // Forbidden - redirect to access denied
                router.navigate(['/access-denied']);
            }

            return throwError(() => error);
        })
    );
};
