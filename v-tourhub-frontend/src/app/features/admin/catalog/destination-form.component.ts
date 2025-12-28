import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { Destination } from '../../../core/models/destination.model';

@Component({
    selector: 'app-destination-form',
    imports: [CommonModule, FormsModule, RouterLink],
    template: `
    <div class="container-fluid px-0">
        <div class="d-flex align-items-center mb-4">
            <a routerLink="/admin/destinations" class="btn btn-outline-secondary me-3"><i class="bi bi-arrow-left"></i> Back</a>
            <h2 class="mb-0">{{ isEdit ? 'Edit' : 'New' }} Destination</h2>
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
                            <label class="form-label">City</label>
                            <input type="text" class="form-control" [(ngModel)]="model.city" name="city">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Province</label>
                            <input type="text" class="form-control" [(ngModel)]="model.province" name="province">
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Description</label>
                        <textarea class="form-control" rows="5" [(ngModel)]="model.description" name="description"></textarea>
                    </div>

                     <div class="d-flex justify-content-end gap-2">
                        <button type="button" routerLink="/admin/destinations" class="btn btn-light">Cancel</button>
                        <button type="submit" class="btn btn-primary" [disabled]="!f.valid || saving">
                            {{ saving ? 'Saving...' : 'Save Destination' }}
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Media Management Section (Only in Edit Mode) -->
        <div class="card shadow-sm border-0 mt-4" style="max-width: 800px;" *ngIf="isEdit">
            <div class="card-header bg-white">
                <h5 class="mb-0">Media Gallery</h5>
            </div>
            <div class="card-body">
                <!-- Existing Media -->
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

                <!-- Upload New -->
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
export class DestinationFormComponent implements OnInit {
    model: Partial<Destination> = {};
    isEdit = false;
    saving = false;

    // Media Upload
    selectedFile: File | null = null;
    uploading = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private api: ApiService,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit() {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.isEdit = true;
            this.api.getDestinationById(+id).subscribe(d => {
                this.model = d;
                this.cdr.detectChanges();
            });
        }
    }

    save() {
        this.saving = true;
        const req = this.isEdit
            ? this.api.updateDestination(this.model.id!, this.model)
            : this.api.createDestination(this.model);

        req.subscribe({
            next: (saved) => {
                if (!this.isEdit) {
                    // Redirect to edit mode to allow media upload
                    this.router.navigate(['/admin/destinations/edit', saved.id]);
                } else {
                    this.router.navigate(['/admin/destinations']);
                }
            },
            error: () => this.saving = false
        });
    }

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

        this.api.uploadDestinationMedia(this.model.id, formData).subscribe({
            next: (url) => {
                this.uploading = false;
                this.selectedFile = null;
                // Refresh data to show new image
                this.refreshData();
                alert('Image uploaded successfully!');
                // Reset file input
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
            this.api.getDestinationById(this.model.id).subscribe(d => {
                this.model = d;
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
