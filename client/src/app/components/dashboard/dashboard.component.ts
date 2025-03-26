import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { SpotifyService } from '../../services/spotify.service';
import { Subscription } from 'rxjs';
import { PlaylistCollection } from '../../models/playlist.model';

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  profile: any = null;
  playlists: PlaylistCollection | null = null;
  recentlyPlayed: any[] = [];
  loading = true;
  error: string | null = null;
  currentYear = new Date().getFullYear();
  
  private subscriptions = new Subscription();
  private authService = inject(AuthService);
  private spotifyService = inject(SpotifyService);
  private router = inject(Router);

  ngOnInit(): void {
    // Subscribe to the service observables
    this.subscriptions.add(
      this.spotifyService.profile$.subscribe(profile => this.profile = profile)
    );
    
    this.subscriptions.add(
      this.spotifyService.playlists$.subscribe(playlists => this.playlists = playlists)
    );
    
    this.subscriptions.add(
      this.spotifyService.loading$.subscribe(loading => this.loading = loading)
    );
    
    this.subscriptions.add(
      this.spotifyService.error$.subscribe(error => this.error = error)
    );
    
    // Fetch the profile data
    this.fetchUserProfile();
    
    // Fetch recently played tracks if available
    this.fetchRecentlyPlayed();
    
    // Fetch a few playlists for the dashboard preview
    this.preloadPlaylists();
  }
  
  ngOnDestroy(): void {
    // Clean up subscriptions to prevent memory leaks
    this.subscriptions.unsubscribe();
  }
  
  fetchUserProfile(): void {
    this.spotifyService.fetchUserProfile();
  }

  preloadPlaylists(): void {
    // Load just a small sample of playlists for the dashboard
    this.spotifyService.fetchPlaylists(6, 0);
  }

  fetchPlaylists(): void {
    // Navigate to the playlists page where more playlists will be loaded
    this.router.navigate(['/playlists']);
  }
  
  fetchRecentlyPlayed(): void {
    // This would require implementing a method in your SpotifyService
    // For now, we'll mock it with an empty array as it's not implemented yet
    this.recentlyPlayed = [];
    
    // Uncomment and implement this in your SpotifyService later
    /*
    this.spotifyService.getRecentlyPlayed().subscribe({
      next: (tracks) => {
        this.recentlyPlayed = tracks.items;
      },
      error: (err) => {
        console.error('Error fetching recently played tracks:', err);
      }
    });
    */
  }
  
  goToTopTracks(): void {
    this.router.navigate(['/top-tracks']);
  }
  
  selectPlaylist(playlist: any): void {
    this.router.navigate(['/player', playlist.id, playlist.name]);
  }
  
  getPlaylistItems(): any[] {
    return this.playlists?.items || [];
  }
  
  logout(): void {
    this.authService.logout();
  }
}