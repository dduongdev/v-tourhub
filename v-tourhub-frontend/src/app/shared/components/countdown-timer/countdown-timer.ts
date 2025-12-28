import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-countdown-timer',
  imports: [CommonModule],
  templateUrl: './countdown-timer.html',
  styleUrl: './countdown-timer.scss'
})
export class CountdownTimerComponent implements OnInit, OnDestroy {
  @Input() expiresAt!: string;

  timeLeft = '00:00';
  isWarning = false;
  private intervalId: any;

  ngOnInit(): void {
    this.startCountdown();
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  private startCountdown(): void {
    this.updateTime();
    this.intervalId = setInterval(() => this.updateTime(), 1000);
  }

  private updateTime(): void {
    const now = new Date().getTime();
    const expires = new Date(this.expiresAt).getTime();
    const diff = expires - now;

    if (diff <= 0) {
      this.timeLeft = 'EXPIRED';
      this.isWarning = true;
      if (this.intervalId) {
        clearInterval(this.intervalId);
      }
      return;
    }

    const minutes = Math.floor(diff / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);
    this.timeLeft = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    this.isWarning = minutes < 5;
  }
}
