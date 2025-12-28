import { Component, OnInit } from '@angular/core';
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

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.loadDestinations();
  }

  loadDestinations(): void {
    this.loading = true;
    this.apiService.getDestinations().subscribe({
      next: (page) => {
        this.destinations = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
