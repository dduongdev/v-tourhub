import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { adminGuard } from './core/guards/admin.guard';
import { HomeComponent } from './features/home/home';
import { ExploreComponent } from './features/customer/explore/explore';
import { MyBookingsComponent } from './features/customer/my-bookings/my-bookings';
import { AccessDeniedComponent } from './features/common/access-denied/access-denied';

export const routes: Routes = [
    { path: '', component: HomeComponent }, // No auth required

    // Customer routes - all require auth
    {
        path: 'explore',
        component: ExploreComponent,
        canActivate: [authGuard]
    },
    {
        path: 'destination/:id',
        loadComponent: () => import('./features/customer/destination-detail/destination-detail').then(m => m.DestinationDetailComponent),
        canActivate: [authGuard]
    },
    {
        path: 'service/:id',
        loadComponent: () => import('./features/customer/service-detail/service-detail').then(m => m.ServiceDetailComponent),
        canActivate: [authGuard]
    },
    {
        path: 'booking',
        loadComponent: () => import('./features/customer/booking/booking').then(m => m.BookingComponent),
        canActivate: [authGuard]
    },
    {
        path: 'my-bookings',
        component: MyBookingsComponent,
        canActivate: [authGuard]
    },
    {
        path: 'profile',
        loadComponent: () => import('./features/customer/profile/profile').then(m => m.ProfileComponent),
        canActivate: [authGuard]
    },
    {
        path: 'payment',
        loadComponent: () => import('./features/customer/payment/payment-callback').then(m => m.PaymentCallbackComponent),
        canActivate: [authGuard]
    },

    // Admin routes
    // Admin routes
    {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES),
        canActivate: [authGuard, adminGuard]
    }
    ,
    { path: 'access-denied', component: AccessDeniedComponent }
];
