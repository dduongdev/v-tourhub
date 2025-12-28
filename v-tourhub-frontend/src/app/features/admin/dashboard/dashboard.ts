import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/api/api.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit {
  totalBookings = 0;

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.apiService.getAllBookings({ size: 1 }).subscribe({
      next: (page) => this.totalBookings = page.totalElements
    });
  }
}
