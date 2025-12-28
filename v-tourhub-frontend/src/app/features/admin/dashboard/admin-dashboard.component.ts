import { Component } from '@angular/core';

@Component({
    selector: 'app-admin-dashboard',
    template: `
    <h2>Dashboard Overview</h2>
    <div class="row mt-4">
        <div class="col-md-4">
            <div class="card text-white bg-primary mb-3">
                <div class="card-header">Bookings</div>
                <div class="card-body">
                    <h5 class="card-title">Manage Reservations</h5>
                    <p class="card-text">View and manage customer bookings.</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card text-white bg-success mb-3">
                <div class="card-header">Destinations</div>
                <div class="card-body">
                    <h5 class="card-title">Catalog Management</h5>
                    <p class="card-text">Add or edit destinations and services.</p>
                </div>
            </div>
        </div>
         <div class="col-md-4">
            <div class="card text-white bg-warning text-dark mb-3">
                <div class="card-header">Inventory</div>
                <div class="card-body">
                    <h5 class="card-title">Stock Control</h5>
                    <p class="card-text">Monitor service availability.</p>
                </div>
            </div>
        </div>
    </div>
  `
})
export class AdminDashboardComponent { }
