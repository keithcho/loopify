import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { SpotifyService, RecommendationWithPreview } from '../../services/spotify.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-top-tracks-recommendations',
  standalone: false,
  templateUrl: './top-tracks-recommendations.component.html',
  styleUrl: './top-tracks-recommendations.component.css'
})
export class TopTracksRecommendationsComponent implements OnInit, OnDestroy {
  timeRange: string | null = null;
  timeRangeLabel: string = '';
  currentYear = new Date().getFullYear();
  
  loading = true;
  error: string | null = null;
  
  // Custom prompt fields
  activeCustomPrompt: string | null = null;
  customPrompt: string = '';
  isApplyingPrompt = false;
  
  // Recommendations
  enhancedRecommendations: RecommendationWithPreview[] = [];
  isLoadingRecommendations = false;
  
  // Audio state
  currentlyPlayingUrl: string | null = null;
  audioElement: HTMLAudioElement | null = null;
  currentSongIndex = 0;
  volume = 0.7; // Default volume (0-1)
  currentTime = 0;
  duration = 30; // Default duration for preview clips (typically 30 seconds)
  audioProgressInterval: any = null;
  
  // Add to playlist
  playlists: any[] = []; // To store user playlists for the dropdown
  selectedPlaylistId: string | null = null;
  addToPlaylistSuccess: string | null = null;
  addToPlaylistError: string | null = null;
  isAddingToPlaylist = false;
  
  // Buffer management
  isFetchingMoreRecommendations = false;
  recommendationBatchSize = 10;
  bufferThreshold = 5; // When to fetch more songs (when this many left)
  totalRecommendationsFetched = 0;
  
  private subscriptions = new Subscription();
  private spotifyService = inject(SpotifyService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  
  // Time range mapping for display
  private timeRangeMap = {
    'short_term': 'Last 4 Weeks',
    'medium_term': 'Last 6 Months',
    'long_term': 'All Time'
  };
  
  ngOnInit(): void {
    // Get time range from route params
    this.route.paramMap.subscribe(params => {
      this.timeRange = params.get('timeRange');
      
      if (!this.timeRange) {
        this.error = 'No time range selected';
        this.loading = false;
        return;
      }
      
      // Set the display label for the time range
      this.timeRangeLabel = this.timeRangeMap[this.timeRange as keyof typeof this.timeRangeMap] || this.timeRange;
      
      // Subscribe to enhanced recommendations
      this.subscriptions.add(
        this.spotifyService.enhancedRecommendations$.subscribe(recommendations => {
          // Handle new recommendations batch
          if (recommendations.length > 0) {
            // If this is the first batch, replace the array
            if (this.enhancedRecommendations.length === 0) {
              this.enhancedRecommendations = [...recommendations];
              this.currentSongIndex = 0;
              this.stopAudio();
            } else {
              // Otherwise, add to the existing array (avoiding duplicates)
              const newRecommendations = recommendations.filter(newRec => 
                !this.enhancedRecommendations.some(existingRec => 
                  existingRec.recommendation.song_title === newRec.recommendation.song_title && 
                  existingRec.recommendation.artist === newRec.recommendation.artist
                )
              );
              this.enhancedRecommendations = [...this.enhancedRecommendations, ...newRecommendations];
            }
            
            this.totalRecommendationsFetched += recommendations.length;
            this.isLoadingRecommendations = false;
            this.isFetchingMoreRecommendations = false;
            this.loading = false;
          }
        })
      );
      
      // Subscribe to errors
      this.subscriptions.add(
        this.spotifyService.error$.subscribe(error => {
          this.error = error;
          this.loading = false;
        })
      );
      
      // Fetch user playlists for the dropdown
      this.fetchUserPlaylists();
      
      // Start fetching recommendations
      this.getRecommendations();
    });
  }
  
  ngOnDestroy(): void {
    // Clean up subscriptions to prevent memory leaks
    this.subscriptions.unsubscribe();
    
    // Stop audio playback when component is destroyed
    this.stopAudio();
    
    // Clear any remaining intervals
    this.clearProgressInterval();
  }
  
  fetchUserPlaylists(): void {
    this.spotifyService.getPlaylistsObservable(50, 0).subscribe({
      next: (playlists) => {
        if (playlists) {
          this.playlists = playlists.items;
          if (this.playlists.length > 0) {
            this.selectedPlaylistId = this.playlists[0].id;
          }
        }
      },
      error: (err) => {
        console.error('Error fetching playlists for dropdown:', err);
      }
    });
  }
  
  // Custom prompt methods
  submitPrompt(): void {
    if (!this.customPrompt || this.customPrompt.trim() === '') {
      return;
    }
    this.applyCustomPrompt(this.customPrompt.trim());
    this.customPrompt = '';
  }
  
  applyCustomPrompt(promptText: string): void {
    if (!promptText || promptText.trim() === '') {
      return;
    }
    
    this.isApplyingPrompt = true;
    this.activeCustomPrompt = promptText.trim();
    
    // Reset recommendations to get new ones with the custom prompt
    this.enhancedRecommendations = [];
    this.totalRecommendationsFetched = 0;
    this.currentSongIndex = 0;
    this.stopAudio();
    
    // Get new recommendations with the custom prompt
    this.getRecommendations();
    this.isApplyingPrompt = false;
  }
  
  resetPrompt(): void {
    this.activeCustomPrompt = null;
    
    // Reset recommendations to get default ones without the custom prompt
    this.enhancedRecommendations = [];
    this.totalRecommendationsFetched = 0;
    this.currentSongIndex = 0;
    this.stopAudio();
    
    // Get new recommendations without custom prompt
    this.getRecommendations();
  }
  
  getRecommendations(): void {
    if (!this.timeRange) {
      this.error = 'No time range selected';
      return;
    }
    
    // Reset state for new recommendations
    this.enhancedRecommendations = [];
    this.totalRecommendationsFetched = 0;
    this.currentSongIndex = 0;
    this.isLoadingRecommendations = true;
    this.error = null;
    this.stopAudio();
    
    // Get initial batch of recommendations - set clearCache to true for first fetch
    this.fetchMoreRecommendations(true);
  }
  
  fetchMoreRecommendations(clearCache: boolean = false): void {
    if (!this.timeRange) {
      return;
    }
    
    // Add an early return if we're already fetching recommendations
    if (this.isFetchingMoreRecommendations) {
      console.log('Request to fetch more recommendations ignored - fetch already in progress');
      return;
    }
    
    this.isFetchingMoreRecommendations = true;
    console.log(`Fetching more recommendations (batch ${Math.floor(this.totalRecommendationsFetched / this.recommendationBatchSize) + 1})`);
    
    // Pass the clearCache parameter and the custom prompt if available
    this.spotifyService.getTopTracksRecommendations(
      this.timeRange, 
      true, 
      this.recommendationBatchSize, 
      this.totalRecommendationsFetched,
      clearCache,
      this.activeCustomPrompt
    );
  }
  
  checkBuffer(): void {
    // If we're nearing the end of our recommendations, fetch more
    if (this.enhancedRecommendations.length === 0) {
      return;
    }
    
    // Only check buffer if we're not already fetching recommendations
    if (!this.isFetchingMoreRecommendations) {
      const remainingSongs = this.enhancedRecommendations.length - (this.currentSongIndex + 1);
      
      if (remainingSongs <= this.bufferThreshold) {
        console.log(`Buffer running low (${remainingSongs} songs left). Fetching more...`);
        this.fetchMoreRecommendations(false); // Don't clear cache for buffer refills
      }
    } else {
      console.log('Already fetching more recommendations, skipping buffer check');
    }
  }
  
  // Carousel navigation methods
  nextSong(): void {
    if (this.currentSongIndex < this.enhancedRecommendations.length - 1) {
      this.stopAudio();
      this.currentSongIndex++;
      // Reset progress tracking values
      this.currentTime = 0;
      this.duration = 30; // Default until new audio metadata is loaded
      
      // Check if we need to fetch more songs
      this.checkBuffer();
      
      // Auto-play the next song if it has a preview
      const nextSong = this.getCurrentSong();
      if (nextSong && this.hasPreview(nextSong)) {
        this.playPreview(nextSong);
      }
    }
  }
  
  previousSong(): void {
    if (this.currentSongIndex > 0) {
      this.stopAudio();
      this.currentSongIndex--;
      // Reset progress tracking values
      this.currentTime = 0;
      this.duration = 30; // Default until new audio metadata is loaded
      
      // Auto-play the previous song if it has a preview
      const prevSong = this.getCurrentSong();
      if (prevSong && this.hasPreview(prevSong)) {
        this.playPreview(prevSong);
      }
    }
  }
  
  getCurrentSong(): RecommendationWithPreview | null {
    if (this.enhancedRecommendations.length === 0 || this.currentSongIndex >= this.enhancedRecommendations.length) {
      return null;
    }
    return this.enhancedRecommendations[this.currentSongIndex];
  }
  
  // Music player methods
  togglePlayPause(): void {
    const currentSong = this.getCurrentSong();
    
    if (!currentSong) {
      return;
    }
    
    if (this.isPlaying(currentSong)) {
      this.pauseAudio();
    } else {
      this.resumeAudio(currentSong);
    }
  }

  pauseAudio(): void {
    if (this.audioElement) {
      this.audioElement.pause();
      // We don't reset currentlyPlayingUrl to null so we know which track is paused
      this.clearProgressInterval();
    }
  }

  resumeAudio(recommendation: RecommendationWithPreview | null): void {
    if (!recommendation) {
      return;
    }
    
    // If this is the same track that was paused, resume it
    if (this.audioElement && this.currentlyPlayingUrl && 
        recommendation.previewUrls && 
        recommendation.previewUrls.includes(this.currentlyPlayingUrl)) {
      this.audioElement.play()
        .then(() => {
          this.startProgressTracking();
        })
        .catch(err => {
          console.error('Failed to resume audio:', err);
        });
      return;
    }
    
    // Otherwise, start playing a new track
    this.playPreview(recommendation);
  }
  
  playPreview(recommendation: RecommendationWithPreview | null): void {
    // Stop any currently playing audio
    this.stopAudio();
    
    if (!recommendation || !recommendation.previewUrls || recommendation.previewUrls.length === 0) {
      console.warn('No preview URLs available for this track');
      return;
    }
    
    const previewUrl = recommendation.previewUrls[0];
    
    // Create and configure audio element
    this.audioElement = new Audio(previewUrl);
    this.currentlyPlayingUrl = previewUrl;
    
    // Set volume
    this.audioElement.volume = this.volume;
    
    // Reset current time
    this.currentTime = 0;
    
    // Set up event handlers
    this.audioElement.addEventListener('loadedmetadata', () => {
      // Update duration once audio metadata is loaded
      this.duration = this.audioElement?.duration || 30;
    });
    
    this.audioElement.addEventListener('ended', () => {
      this.currentlyPlayingUrl = null;
      this.clearProgressInterval();
      
      // Automatically play next song if available
      if (this.currentSongIndex < this.enhancedRecommendations.length - 1) {
        this.nextSong();
        this.playPreview(this.getCurrentSong());
      }
    });
    
    this.audioElement.addEventListener('error', (e) => {
      console.error('Error playing audio:', e);
      this.currentlyPlayingUrl = null;
      this.clearProgressInterval();
      
      // If there are other preview URLs available, try the next one
      if (recommendation.previewUrls && recommendation.previewUrls.length > 1) {
        const nextIndex = recommendation.previewUrls.indexOf(previewUrl) + 1;
        if (nextIndex < recommendation.previewUrls.length) {
          const nextUrl = recommendation.previewUrls[nextIndex];
          this.audioElement = new Audio(nextUrl);
          this.audioElement.volume = this.volume;
          this.currentlyPlayingUrl = nextUrl;
          this.audioElement.play();
          this.startProgressTracking();
        }
      }
    });
    
    // Start playback
    this.audioElement.play()
      .then(() => {
        // Start tracking progress once playback begins
        this.startProgressTracking();
      })
      .catch(err => {
        console.error('Failed to play audio:', err);
      });
  }
  
  stopAudio(): void {
    if (this.audioElement) {
      this.audioElement.pause();
      this.audioElement = null;
    }
    this.currentlyPlayingUrl = null;
    this.clearProgressInterval();
    
    // Reset progress tracking to start position
    this.currentTime = 0;
  }
  
  // Helper methods for audio progress tracking
  startProgressTracking(): void {
    // Clear any existing interval
    this.clearProgressInterval();
    
    // Update progress every 100ms
    this.audioProgressInterval = setInterval(() => {
      if (this.audioElement) {
        this.currentTime = this.audioElement.currentTime;
      }
    }, 100);
  }
  
  clearProgressInterval(): void {
    if (this.audioProgressInterval) {
      clearInterval(this.audioProgressInterval);
      this.audioProgressInterval = null;
    }
  }
  
  /**
   * Formats seconds into MM:SS format
   * @param seconds Time in seconds
   */
  formatTime(seconds: number): string {
    if (isNaN(seconds)) return '0:00';
    
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
  
  // Handle seek events from the progress slider
  onSeek(event: Event): void {
    const input = event.target as HTMLInputElement;
    const seekTime = parseFloat(input.value);
    
    if (this.audioElement) {
      this.audioElement.currentTime = seekTime;
      this.currentTime = seekTime;
    }
  }
  
  isPlaying(recommendation: RecommendationWithPreview | null): boolean {
    if (!recommendation) return false;
    if (!recommendation.previewUrls) return false;
    if (recommendation.previewUrls.length === 0) return false;
    
    return recommendation.previewUrls.includes(this.currentlyPlayingUrl || '') && 
           this.audioElement !== null && 
           !this.audioElement.paused;
  }
  
  hasPreview(recommendation: RecommendationWithPreview | null): boolean {
    if (!recommendation) return false;
    if (!recommendation.previewUrls) return false;
    return recommendation.previewUrls.length > 0;
  }
  
  onVolumeChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.volume = parseFloat(input.value);
    
    if (this.audioElement) {
      this.audioElement.volume = this.volume;
    }
  }

  addCurrentSongToPlaylist(): void {
    const currentSong = this.getCurrentSong();
    
    if (!currentSong || !this.selectedPlaylistId || !currentSong.track) {
      this.addToPlaylistError = 'Unable to add song to playlist';
      return;
    }
    
    this.isAddingToPlaylist = true;
    this.addToPlaylistSuccess = null;
    this.addToPlaylistError = null;
    
    const trackUri = currentSong.track.uri;
    
    this.spotifyService.addTrackToPlaylist(this.selectedPlaylistId, trackUri)
      .subscribe({
        next: (response) => {
          this.isAddingToPlaylist = false;
          const playlistName = this.playlists.find(p => p.id === this.selectedPlaylistId)?.name || 'playlist';
          this.addToPlaylistSuccess = `"${currentSong.track.name}" has been added to "${playlistName}"`;
          
          // Auto-hide success message after 5 seconds
          setTimeout(() => {
            if (this.addToPlaylistSuccess) {
              this.addToPlaylistSuccess = null;
            }
          }, 5000);
        },
        error: (err) => {
          this.isAddingToPlaylist = false;
          this.addToPlaylistError = 'Failed to add song to playlist. ' + 
            (err.error && typeof err.error === 'string' ? err.error : 'Please try again.');
          console.error('Error adding track to playlist:', err);
        }
      });
  }
  
  switchTimeRange(timeRange: string): void {
    if (this.timeRange === timeRange) {
      return; // No change needed
    }
    this.router.navigate(['/recommendations/top-tracks', timeRange]);
  }
  
  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Set the current song directly by index
   * Used when the user clicks a song in the carousel
   */
  setCurrentSong(index: number): void {
    if (index >= 0 && index < this.enhancedRecommendations.length) {
      // Stop any currently playing audio first
      this.stopAudio();
      
      // Update the index
      this.currentSongIndex = index;
      
      // Reset progress tracking values
      this.currentTime = 0;
      this.duration = 30; // Default until new audio metadata is loaded
      
      // Check if we need to fetch more songs based on new position
      this.checkBuffer();
      
      // Auto-play the selected song if it has a preview
      const selectedSong = this.getCurrentSong();
      if (selectedSong && this.hasPreview(selectedSong)) {
        this.playPreview(selectedSong);
      }
    }
  }

  /**
   * Gets the width of the progress track element
   * Used for accurate positioning of the slider handle
   */
  getTrackWidth(): number {
    const progressTrack = document.querySelector('.bg-gray-800.rounded-full.overflow-hidden') as HTMLElement;
    return progressTrack ? progressTrack.offsetWidth : 100;
  }

  /**
   * Gets the width of the volume slider track
   * Used for accurate positioning of the volume handle
   */
  getVolumeTrackWidth(): number {
    const volumeTrack = document.querySelector('.flex-1.h-2.bg-gray-800') as HTMLElement;
    return volumeTrack ? volumeTrack.offsetWidth : 100;
  }
}