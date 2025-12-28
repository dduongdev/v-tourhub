import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/api/api.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  loading = false;

  constructor(private apiService: ApiService, private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.refreshProfile();
  }

  refreshProfile(): void {
    this.loading = true;
    this.apiService.getUserProfile().pipe(finalize(() => {
      this.loading = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (user) => {
        this.user = user;
      },
      error: () => console.error('profile load error')
    });
  }

  onSubmit(): void {
    if (!this.user) return;
    this.loading = true;
    this.apiService.updateUserProfile(this.user).pipe(finalize(() => {
      this.loading = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (updatedUser) => {
        this.user = updatedUser;
        alert('Profile updated successfully!');
      },
      error: (err) => alert('Failed to update profile')
    });
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      const formData = new FormData();
      formData.append('file', file);

      this.loading = true;
      this.apiService.uploadAvatar(formData).pipe(finalize(() => {
        this.loading = false;
        this.cd.detectChanges();
      })).subscribe({
        next: (url) => {
          if (this.user) this.user.avatarUrl = url;
          this.refreshProfile();
        },
        error: () => alert('Failed to upload avatar')
      });
    }
  }
}
