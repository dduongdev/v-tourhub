import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { ExploreComponent } from './features/customer/explore/explore';
import { MyBookingsComponent } from './features/customer/my-bookings/my-bookings';
import { DashboardComponent } from './features/admin/dashboard/dashboard';

export const routes: Routes = [
    { path: '', redirectTo: '/explore', pathMatch: 'full' },

    // Customer routes
    {
        path: 'explore',
        component: ExploreComponent,
        canActivate: [authGuard]
    },
    {
        path: 'my-bookings',
        component: MyBookingsComponent,
        canActivate: [authGuard, roleGuard],
        data: { roles: ['customer'] }
    },

    // Admin routes
    {
        path: 'admin/dashboard',
        component: DashboardComponent,
        canActivate: [authGuard, roleGuard],
        data: { roles: ['admin'] }
    }
];
