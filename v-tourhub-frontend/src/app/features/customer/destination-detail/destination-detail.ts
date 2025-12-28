import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { Destination } from '../../../core/models/destination.model';

@Component({
  selector: 'app-destination-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './destination-detail.html',
  styleUrl: './destination-detail.scss'
})
export class DestinationDetailComponent implements OnInit {
  destination: Destination | null = null;
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private apiService: ApiService,
    private cd: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDestination(+id);
    }
  }

  loadDestination(id: number): void {
    this.loading = true;

    // ForkJoin or nested subscription? Nested is simpler for error handling in this context
    this.apiService.getDestinationById(id).pipe(finalize(() => {
    })).subscribe({
      next: (destination) => {
        this.destination = destination;
        // Fetch services separately to get filtered list
        this.loadServices(id);
      },
      error: (err) => {
        console.error('Error loading destination:', err);
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  loadServices(destId: number): void {
    this.apiService.getServicesForDestination(destId).pipe(finalize(() => {
      this.loading = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (services) => {
        if (this.destination) {
          this.destination.services = services;
        }
      },
      error: (err) => console.error('Error loading services:', err)
    });
  }
}
