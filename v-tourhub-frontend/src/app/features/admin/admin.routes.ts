import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './layout/admin-layout.component';
import { AdminDashboardComponent } from './dashboard/admin-dashboard.component';

export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        component: AdminLayoutComponent,
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: AdminDashboardComponent },
            { path: 'bookings', loadComponent: () => import('./all-bookings/all-bookings').then(m => m.AllBookingsComponent) },
            { path: 'destinations', loadComponent: () => import('./catalog/destination-list.component').then(m => m.AdminDestinationsComponent) },
            { path: 'destinations/new', loadComponent: () => import('./catalog/destination-form.component').then(m => m.DestinationFormComponent) },
            { path: 'destinations/edit/:id', loadComponent: () => import('./catalog/destination-form.component').then(m => m.DestinationFormComponent) },

            // Service Routes
            { path: 'destinations/:id/services', loadComponent: () => import('./catalog/service-list.component').then(m => m.AdminServiceListComponent) },
            { path: 'destinations/:destId/services/new', loadComponent: () => import('./catalog/service-form.component').then(m => m.ServiceFormComponent) },
            { path: 'destinations/:destId/services/edit/:id', loadComponent: () => import('./catalog/service-form.component').then(m => m.ServiceFormComponent) }
        ]
    }
];
