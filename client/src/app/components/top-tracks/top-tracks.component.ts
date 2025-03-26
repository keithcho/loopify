import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { SpotifyService } from '../../services/spotify.service';

@Component({
  selector: 'app-top-tracks',
  standalone: false,
  templateUrl: './top-tracks.component.html',
  styleUrls: ['./top-tracks.component.css']
})
export class TopTracksComponent implements OnInit, OnDestroy {
  timeRanges = [
    { value: 'short_term', label: 'Last 4 Weeks' },
    { value: 'medium_term', label: 'Last 6 Months' },
    { value: 'long_term', label: 'All Time' }
  ];
  
  selectedTimeRange = 'medium_term';
  topTracks: any[] = [];
  loading = true;
  error: string | null = null;
  
  private subscriptions = new Subscription();
  private spotifyService = inject(SpotifyService);
  private router = inject(Router);
  
  ngOnInit(): void {
    // Fetch top tracks when component is initialized
    this.fetchTopTracks();
  }
  
  ngOnDestroy(): void {
    // Clean up subscriptions to prevent memory leaks
    this.subscriptions.unsubscribe();
  }
  
  fetchTopTracks(): void {
    this.loading = true;
    this.error = null;
    
    this.spotifyService.getUserTopTracks(this.selectedTimeRange)
      .subscribe({
        next: (tracks) => {
          this.topTracks = tracks.items;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error fetching top tracks:', err);
          this.error = 'Failed to load your top tracks. Please try again.';
          this.loading = false;
        }
      });
  }
  
  onTimeRangeChange(): void {
    this.fetchTopTracks();
  }
  
  getRecommendations(): void {
    // Navigate to recommendations page based on top tracks
    this.router.navigate(['/recommendations/top-tracks', this.selectedTimeRange]);
  }
  
  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}