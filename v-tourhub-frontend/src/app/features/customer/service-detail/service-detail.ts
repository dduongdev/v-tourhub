import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { Service } from '../../../core/models/service.model';
import { finalize } from 'rxjs/operators';

@Component({
    selector: 'app-service-detail',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './service-detail.html'
})
export class ServiceDetailComponent implements OnInit {
    service: Service | null = null;
    loading = false;
    loadingMore = false;

    // Inventory State
    inventoryList: any[] = []; // Using any temporary, better to use InventoryInfo
    startDate: Date = new Date();
    endDate: Date = new Date();

    constructor(
        private route: ActivatedRoute,
        private apiService: ApiService,
        private cd: ChangeDetectorRef
    ) {
        // Initialize endDate to today + 14 days
        this.endDate.setDate(this.startDate.getDate() + 14);
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            const id = params['id'];
            if (id) {
                this.loadService(id, true);
            }
        });
    }

    loadService(id: number, reset: boolean = false): void {
        console.log('Loading service:', id, 'Reset:', reset);

        if (reset) {
            this.loading = true;
            this.inventoryList = [];
            this.startDate = new Date();
            this.endDate = new Date();
            this.endDate.setDate(this.startDate.getDate() + 14);
        } else {
            this.loadingMore = true;
            // Advance dates for next batch
            this.startDate = new Date(this.endDate);
            this.startDate.setDate(this.startDate.getDate() + 1); // Start next day
            this.endDate = new Date(this.startDate);
            this.endDate.setDate(this.endDate.getDate() + 14); // +14 days
        }

        const startStr = this.formatDate(this.startDate);
        const endStr = this.formatDate(this.endDate);

        this.apiService.getServiceById(id, startStr, endStr)
            .pipe(finalize(() => {
                this.loading = false;
                this.loadingMore = false;
                this.cd.detectChanges();
            }))
            .subscribe({
                next: (data) => {
                    if (reset) {
                        this.service = data;
                        this.inventoryList = (data.inventoryCalendar || []).filter((inv: any) => inv.availableStock > 0);
                    } else {
                        // Merge inventory
                        if (data.inventoryCalendar) {
                            const newItems = data.inventoryCalendar.filter((inv: any) => inv.availableStock > 0);
                            this.inventoryList = [...this.inventoryList, ...newItems];
                        }
                    }
                    console.log('Inventory loaded:', this.inventoryList.length);
                    this.cd.detectChanges();
                },
                error: (err) => {
                    console.error('Failed to load service', err);
                    this.cd.detectChanges();
                }
            });
    }

    loadMore(): void {
        if (this.service) {
            this.loadService(this.service.id, false);
        }
    }

    private formatDate(date: Date): string {
        return date.toISOString().split('T')[0];
    }
}
