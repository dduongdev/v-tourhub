import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { Destination } from '../../../core/models/destination.model';
import { LoaderComponent } from '../../../shared/components/loader/loader';

@Component({
    selector: 'app-admin-destinations',
    imports: [CommonModule, RouterLink, LoaderComponent],
    template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2>Destinations</h2>
            <p class="text-secondary">Manage travel destinations and their services.</p>
        </div>
        <a routerLink="new" class="btn btn-primary">
            <i class="bi bi-plus-lg"></i> Add Destination
        </a>
    </div>

    @if (loading) {
        <app-loader></app-loader>
    } @else {
        <div class="grid-cards three-cols">
            @for (dest of destinations; track dest.id) {
            <div class="card h-100 shadow-sm">
                @if(dest.mediaList && dest.mediaList.length > 0) {
                    <img [src]="dest.mediaList[0].url" class="card-img-top" style="height: 180px; object-fit: cover;" alt="{{dest.name}}">
                } @else {
                    <div class="bg-secondary text-white d-flex align-items-center justify-content-center" style="height: 180px;">
                        <i class="bi bi-image fs-1 opacity-50"></i>
                    </div>
                }
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start">
                        <h5 class="card-title fw-bold mb-1">{{ dest.name }}</h5>
                        <span class="badge bg-light text-dark border">ID: {{ dest.id }}</span>
                    </div>
                    <p class="text-secondary small mb-2">
                        <i class="bi bi-geo-alt-fill text-danger"></i> 
                        {{ dest.address ? dest.address + ', ' : '' }}{{ dest.city }}{{ dest.province ? ', ' + dest.province : '' }}
                    </p>
                    
                    <div class="d-flex gap-2 mb-3">
                         <span class="badge bg-info bg-opacity-10 text-info">
                            <i class="bi bi-grid"></i> {{ dest.services?.length || 0 }} Services
                         </span>
                         <span class="badge bg-warning bg-opacity-10 text-warning">
                            <i class="bi bi-images"></i> {{ dest.mediaList?.length || 0 }} Photos
                         </span>
                    </div>

                    <p class="card-text text-secondary small text-truncate-3" style="min-height: 3.6em;">
                        {{ dest.description }}
                    </p>
                    
                    <div class="d-grid gap-2 mt-auto">
                        <a [routerLink]="[dest.id, 'services']" class="btn btn-primary btn-sm">
                            <i class="bi bi-list-check"></i> Manage Services
                        </a>
                        <a [routerLink]="['edit', dest.id]" class="btn btn-outline-secondary btn-sm">
                            <i class="bi bi-pencil"></i> Edit Details
                        </a>
                    </div>
                </div>
            </div>
            }
        </div>
    }
  `,
    styles: [`
    .grid-cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 1.5rem;
    }
  `]
})
export class AdminDestinationsComponent implements OnInit {
    destinations: Destination[] = [];
    loading = false;

    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }

    ngOnInit() {
        this.loadDestinations();
    }

    loadDestinations() {
        this.loading = true;
        this.api.getDestinations(0, 100).subscribe({ // Fetch ample amount for admin list
            next: (page) => {
                this.destinations = page.content;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => this.loading = false
        });
    }

    deleteDestination(id: number) {
        if (confirm('Delete this destination?')) {
            this.api.deleteDestination(id).subscribe(() => this.loadDestinations());
        }
    }
}
