import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
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
