import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/api/api.service';

@Component({
    selector: 'app-inventory-setup',
    imports: [CommonModule, FormsModule],
    template: `
    <div class="modal fade show d-block" style="background: rgba(0,0,0,0.5);" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Setup Inventory</h5>
                    <button type="button" class="btn-close" (click)="close.emit()"></button>
                </div>
                <div class="modal-body">
                    <div class="alert alert-warning small">
                        <i class="bi bi-exclamation-triangle"></i> This will reset/override inventory for the selected range.
                    </div>
                    <form #f="ngForm">
                        <div class="mb-3">
                            <label class="form-label">Start Date</label>
                            <input type="date" class="form-control" [(ngModel)]="startDate" name="startDate" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">End Date</label>
                            <input type="date" class="form-control" [(ngModel)]="endDate" name="endDate" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Total Stock (per day)</label>
                            <input type="number" class="form-control" [(ngModel)]="stock" name="stock" required min="0">
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" (click)="close.emit()">Cancel</button>
                    <button type="button" class="btn btn-primary" (click)="save()" [disabled]="!f.valid || saving">
                        {{ saving ? 'Processing...' : 'Setup Inventory' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
  `
})
export class InventorySetupComponent {
    @Input() serviceId!: number;
    @Output() close = new EventEmitter<void>();
    @Output() saved = new EventEmitter<void>();

    startDate = new Date().toISOString().split('T')[0];
    endDate = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().split('T')[0];
    stock = 10;
    saving = false;

    constructor(private api: ApiService) { }

    save() {
        this.saving = true;
        this.api.setupInventory(this.serviceId, this.stock, this.startDate, this.endDate).subscribe({
            next: () => {
                this.saved.emit();
                this.close.emit();
            },
            error: (err) => {
                alert('Failed to setup inventory');
                this.saving = false;
            }
        });
    }
}
