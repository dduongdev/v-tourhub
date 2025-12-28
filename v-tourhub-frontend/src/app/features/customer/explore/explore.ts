import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';
import { Destination } from '../../../core/models/destination.model';

@Component({
  selector: 'app-explore',
  imports: [CommonModule, RouterLink],
  templateUrl: './explore.html',
  styleUrl: './explore.scss'
})
export class ExploreComponent implements OnInit {
  destinations: Destination[] = [];
  loading = false;

  constructor(private apiService: ApiService, private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.loadDestinations();
  }

  loadDestinations(): void {
    this.loading = true;
    this.apiService.getDestinations().pipe(finalize(() => {
      this.loading = false;
      this.cd.detectChanges();
      console.log('explore loading after finalize:', this.loading);
    })).subscribe({
      next: (response) => {
        this.destinations = response.content || [];
        console.log('Loaded destinations:', this.destinations);
      },
      error: (err) => {
        console.error('Error loading destinations:', err);
      }
    });
  }
}
