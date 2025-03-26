import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { RecommendationWithPreview } from '../../services/spotify.service';

@Component({
  selector: 'app-music-carousel',
  standalone: false,
  templateUrl: './music-carousel.component.html',
  styleUrls: ['./music-carousel.component.css']
})
export class MusicCarouselComponent implements OnInit {
  @Input() recommendations: RecommendationWithPreview[] = [];
  @Input() currentIndex: number = 0;
  
  @Output() prevSong = new EventEmitter<void>();
  @Output() nextSong = new EventEmitter<void>();
  @Output() songSelected = new EventEmitter<number>();
  
  constructor() { }

  ngOnInit(): void {
  }
  
  // Navigate to previous song
  onPrevSong(): void {
    if (this.currentIndex > 0) {
      this.prevSong.emit();
    }
  }
  
  // Navigate to next song
  onNextSong(): void {
    if (this.currentIndex < this.recommendations.length - 1) {
      this.nextSong.emit();
    }
  }
  
  // Get the album image URL with fallback
  getAlbumImageUrl(recommendation: RecommendationWithPreview | null): string {
    if (recommendation?.track?.album?.images?.length) {
      return recommendation.track.album.images[0].url;
    }
    return ''; // Will use background color as fallback
  }
  
  // Get background color for placeholder when no image available
  getBackgroundColor(index: number): string {
    // Array of nice colors to use as fallbacks
    const colors = [
      '#8b5cf6', '#ec4899', '#3b82f6', 
      '#10b981', '#f59e0b', '#ef4444'
    ];
    return colors[index % colors.length];
  }
  
  // Get song title
  getSongTitle(recommendation: RecommendationWithPreview | null): string {
    if (!recommendation) return 'Unknown Song';
    return recommendation.recommendation.song_title || 'Unknown Song';
  }
  
  // Get artist name with fallback
  getArtistName(recommendation: RecommendationWithPreview | null): string {
    if (!recommendation) return 'Unknown Artist';
    
    // Try to get artist from Spotify data
    if (recommendation.track?.artists?.length) {
      return recommendation.track.artists[0].name;
    }
    
    // Fallback to recommendation data
    return recommendation.recommendation.artist || 'Unknown Artist';
  }
}