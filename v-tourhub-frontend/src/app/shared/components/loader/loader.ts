import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-loader',
    imports: [CommonModule],
    template: `
    <div class="d-flex justify-content-center py-5">
        <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
        </div>
    </div>
  `
})
export class LoaderComponent { }
