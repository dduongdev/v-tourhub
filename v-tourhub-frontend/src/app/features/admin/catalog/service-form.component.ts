import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { Service } from '../../../core/models/service.model';

@Component({
    selector: 'app-service-form',
    imports: [CommonModule, FormsModule, RouterLink],
    template: `
    <div class="container-fluid px-0">
        <div class="d-flex align-items-center mb-4">
            <button (click)="goBack()" class="btn btn-outline-secondary me-3"><i class="bi bi-arrow-left"></i> Back</button>
            <h2 class="mb-0">{{ isEdit ? 'Edit' : 'New' }} Service</h2>
        </div>

        <div class="card shadow-sm border-0" style="max-width: 800px;">
            <div class="card-body">
                <form (ngSubmit)="save()" #f="ngForm">
                    <div class="mb-3">
                        <label class="form-label">Name</label>
                        <input type="text" class="form-control" [(ngModel)]="model.name" name="name" required>
                    </div>
                    
                    <div class="row">
                         <div class="col-md-6 mb-3">
                            <label class="form-label">Type</label>
                            <select class="form-select" [(ngModel)]="model.type" name="type" required>
                                <option value="TOUR">Tour</option>
                                <option value="HOTEL">Hotel</option>
                                <option value="ACTIVITY">Activity</option>
                                <option value="RESTAURANT">Restaurant</option>
                            </select>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Price</label>
                            <input type="number" class="form-control" [(ngModel)]="model.price" name="price" required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Description</label>
                         <textarea class="form-control" rows="5" [(ngModel)]="model.description" name="description"></textarea>
                    </div>

                    <div class="row align-items-center mb-4">
                         <div class="col-md-6">
                            <div class="form-check form-switch">
                                <input class="form-check-input" type="checkbox" [(ngModel)]="model.availability" name="availability" id="availSwitch">
                                <label class="form-check-label" for="availSwitch">Visible to Customers</label>
                            </div>
                         </div>
                    </div>

                     <!-- Attributes Section -->
                     <div class="mb-4">
                        <label class="form-label d-block">Additional Attributes</label>
                        <div class="card bg-light border-0">
                            <div class="card-body">
                                <div class="row g-2 mb-2" *ngFor="let row of attributeRows; let i = index">
                                    <div class="col-5">
                                        <input type="text" class="form-control form-control-sm" placeholder="Key (e.g. Duration)" [(ngModel)]="row.key" [name]="'attrKey' + i">
                                    </div>
                                    <div class="col-5">
                                        <input type="text" class="form-control form-control-sm" placeholder="Value (e.g. 3 Days)" [(ngModel)]="row.value" [name]="'attrVal' + i">
                                    </div>
                                    <div class="col-2 text-end">
                                        <button type="button" class="btn btn-sm btn-outline-danger" (click)="removeAttribute(i)" *ngIf="attributeRows.length > 1">
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </div>
                                </div>
                                <button type="button" class="btn btn-sm btn-outline-primary mt-2" (click)="addAttribute()">
                                    <i class="bi bi-plus"></i> Add Attribute
                                </button>
                            </div>
                        </div>
                     </div>

                     <!-- Inventory Prompt -->
                     <div class="alert alert-info" *ngIf="!isEdit">
                        <i class="bi bi-info-circle-fill"></i> You can set inventory availability after saving.
                     </div>

                    <div class="d-flex justify-content-end gap-2">
                        <button type="button" (click)="goBack()" class="btn btn-light">Cancel</button>
                        <button type="submit" class="btn btn-primary" [disabled]="!f.valid || saving">
                            {{ saving ? 'Saving...' : 'Save Service' }}
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Media Management (Edit Mode) -->
        <div class="card shadow-sm border-0 mt-4" style="max-width: 800px;" *ngIf="isEdit">
            <div class="card-header bg-white">
                <h5 class="mb-0">Media Gallery</h5>
            </div>
            <div class="card-body">
                @if (model.mediaList && model.mediaList.length > 0) {
                    <div class="row g-3 mb-3">
                        @for (media of model.mediaList; track media.id) {
                            <div class="col-6 col-md-3">
                                <div class="position-relative">
                                    <img [src]="media.url" class="img-thumbnail" style="width: 100%; height: 120px; object-fit: cover;">
                                    <button (click)="removeMedia(media.id)" class="btn btn-danger position-absolute top-0 end-0 m-1" style="width: 30px; height: 30px; padding: 0; line-height: 28px;">
                                        <i class="bi bi-x-lg"></i>
                                    </button>
                                </div>
                            </div>
                        }
                    </div>
                } @else {
                    <p class="text-secondary small">No images uploaded yet.</p>
                }

                 <div class="input-group">
                    <input type="file" class="form-control" (change)="onFileSelected($event)" accept="image/*">
                    <button class="btn btn-outline-primary" type="button" [disabled]="!selectedFile || uploading" (click)="uploadMedia()">
                        <i class="bi bi-cloud-upload"></i> {{ uploading ? 'Uploading...' : 'Upload' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
  `
})
export class ServiceFormComponent implements OnInit {
    model: Partial<Service> = { availability: true }; // Default to visible
    isEdit = false;
    saving = false;
    destinationId!: number;

    // Media
    selectedFile: File | null = null;
    uploading = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private api: ApiService,
        private cdr: ChangeDetectorRef
    ) { }

    // Attributes Management
    attributeRows: { key: string; value: string }[] = [];

    ngOnInit() {
        // Route: destinations/:destId/services/new OR destinations/:destId/services/edit/:id
        this.destinationId = +this.route.snapshot.paramMap.get('destId')!;
        const id = this.route.snapshot.paramMap.get('id');

        if (id) {
            this.isEdit = true;
            this.api.getServiceById(+id).subscribe(s => {
                this.model = s;
                this.initAttributes();
                this.cdr.detectChanges();
            });
        } else {
            this.initAttributes();
        }
    }

    initAttributes() {
        if (this.model.attributes) {
            this.attributeRows = Object.entries(this.model.attributes).map(([key, value]) => ({ key, value }));
        }
        if (this.attributeRows.length === 0) {
            this.addAttribute();
        }
    }

    addAttribute() {
        this.attributeRows.push({ key: '', value: '' });
    }

    removeAttribute(index: number) {
        this.attributeRows.splice(index, 1);
    }

    save() {
        this.saving = true;

        // Convert attributes array back to object
        const attrs: { [key: string]: string } = {};
        this.attributeRows.forEach(row => {
            if (row.key.trim()) {
                attrs[row.key.trim()] = row.value.trim();
            }
        });
        this.model.attributes = attrs;

        if (!this.isEdit) {
            this.model.destination = { id: this.destinationId } as any;
        }

        const req = this.isEdit
            ? this.api.updateService(this.model.id!, this.model)
            : this.api.createService(this.model);

        req.subscribe({
            next: (saved) => {
                if (!this.isEdit) {
                    // Redirect to edit to upload media
                    this.router.navigate(['/admin/destinations', this.destinationId, 'services', 'edit', saved.id]);
                } else {
                    this.goBack();
                }
            },
            error: () => this.saving = false
        });
    }

    // ... (rest of methods)

    goBack() {
        this.router.navigate(['/admin/destinations', this.destinationId, 'services']);
    }

    // ... (rest of methods like onFileSelected, uploadMedia, removeMedia)

    onFileSelected(event: any) {
        const file = event.target.files[0];
        if (file) {
            this.selectedFile = file;
        }
    }

    uploadMedia() {
        if (!this.selectedFile || !this.model.id) return;

        this.uploading = true;
        const formData = new FormData();
        formData.append('file', this.selectedFile);

        this.api.uploadServiceMedia(this.model.id, formData).subscribe({
            next: () => {
                this.uploading = false;
                this.selectedFile = null;
                alert('Image uploaded successfully!');
                this.refreshData();
                const input = document.querySelector('input[type="file"]') as HTMLInputElement;
                if (input) input.value = '';
            },
            error: (err) => {
                console.error(err);
                this.uploading = false;
                alert('Failed to upload image.');
            }
        });
    }

    refreshData() {
        if (this.model.id) {
            this.api.getServiceById(this.model.id).subscribe(s => {
                this.model = s;
                // re-init attributes? usually attributes don't change from other sources during edit, skipping to avoid overwriting user input if any
                // actually, for refreshData (after media upload), we might just want to keep attributes as is in UI
                // or reload everything. Let's strictly reload model but keep UI rows if dirty?
                // For simplicity, just update model props.
                // Attributes might need reloading if backend normalized them.
                this.initAttributes();
                this.cdr.detectChanges();
            });
        }
    }

    removeMedia(mediaId: number) {
        if (!confirm('Are you sure you want to delete this image?')) return;

        this.api.deleteMedia(mediaId).subscribe({
            next: () => {
                alert('Media deleted successfully');
                this.refreshData();
            },
            error: (err) => {
                console.error(err);
                alert('Failed to delete media');
            }
        });
    }
}
