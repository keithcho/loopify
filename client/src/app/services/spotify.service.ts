import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, tap, throwError } from 'rxjs';
import { PlaylistCollection } from '../models/playlist.model';
import { environment } from '../../environments/environment';

export interface RecommendationWithPreview {
  success: boolean;
  recommendation: {
    song_title: string;
    artist: string;
  };
  track?: any;
  previewUrls?: string[];
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SpotifyService {
  private profileSubject = new BehaviorSubject<any>(null);
  private playlistsSubject = new BehaviorSubject<PlaylistCollection | null>(null);
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<string | null>(null);
  private recommendationsSubject = new BehaviorSubject<any[]>([]);
  private enhancedRecommendationsSubject = new BehaviorSubject<RecommendationWithPreview[]>([]);
  private topTracksSubject = new BehaviorSubject<any[]>([]);

  profile$ = this.profileSubject.asObservable();
  playlists$ = this.playlistsSubject.asObservable();
  loading$ = this.loadingSubject.asObservable();
  error$ = this.errorSubject.asObservable();
  recommendations$ = this.recommendationsSubject.asObservable();
  enhancedRecommendations$ = this.enhancedRecommendationsSubject.asObservable();
  topTracks$ = this.topTracksSubject.asObservable();
  
  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient)

  constructor() { }

  fetchUserProfile(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);
    
    this.http.get(`${this.apiUrl}/spotify/profile`, { withCredentials: true })
      .subscribe({
        next: (profile) => {
          this.profileSubject.next(profile);
          this.loadingSubject.next(false);
        },
        error: (err) => {
          this.errorSubject.next('Failed to load Spotify profile. Please try again.');
          this.loadingSubject.next(false);
          console.error('Error fetching profile:', err);
        }
      });
  }

  fetchPlaylists(limit: number = 5, offset: number = 0): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);
    
    this.http.get<PlaylistCollection>(
      `${this.apiUrl}/spotify/playlists?limit=${limit}&offset=${offset}`, 
      { withCredentials: true }
    ).subscribe({
      next: (playlists) => {
        console.log('Playlists received:', playlists);
        this.playlistsSubject.next(playlists);
        this.loadingSubject.next(false);
      },
      error: (err) => {
        console.error('Error fetching playlists:', err);
        this.errorSubject.next('Failed to load Spotify playlists. Please try again.');
        this.loadingSubject.next(false);
      }
    });
  }
  
  getPlaylistsObservable(limit: number = 20, offset: number = 0): Observable<PlaylistCollection> {
    const setGlobalLoading = limit <= 20 && offset === 0;
    
    if (setGlobalLoading) {
      this.loadingSubject.next(true);
      this.errorSubject.next(null);
    }
    
    return this.http.get<PlaylistCollection>(
      `${this.apiUrl}/spotify/playlists?limit=${limit}&offset=${offset}`, 
      { withCredentials: true }
    ).pipe(
      tap(playlists => {
        console.log(`Playlists received (offset ${offset}, limit ${limit}):`, 
          playlists ? `${playlists.items.length} items` : 'No playlists');
        
        // Only update the main subject for the primary playlist fetch
        if (setGlobalLoading) {
          this.playlistsSubject.next(playlists);
          this.loadingSubject.next(false);
        }
      }),
      catchError(err => {
        console.error(`Error fetching playlists (offset ${offset}, limit ${limit}):`, err);
        
        // Only update error subject for the primary playlist fetch
        if (setGlobalLoading) {
          this.errorSubject.next('Failed to load Spotify playlists. Please try again.');
          this.loadingSubject.next(false);
        }
        
        return throwError(() => err);
      })
    );
  }

  getUserTopTracks(timeRange: string = 'medium_term', limit: number = 20, offset: number = 0): Observable<any> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);
    
    const observable = new Observable<any>(observer => {
      this.http.get<any>(
        `${this.apiUrl}/spotify/top-tracks?timeRange=${timeRange}&limit=${limit}&offset=${offset}`,
        { withCredentials: true }
      ).subscribe({
        next: (response) => {
          this.topTracksSubject.next(response.items);
          this.loadingSubject.next(false);
          observer.next(response);
          observer.complete();
        },
        error: (err) => {
          console.error('Error fetching top tracks:', err);
          this.errorSubject.next('Failed to load your top tracks. Please try again.');
          this.loadingSubject.next(false);
          observer.error(err);
        }
      });
    });
    
    return observable;
  }

  getRecommendations(
    playlistId: string, 
    includePreviewUrls: boolean = true,
    limit: number = 10, 
    offset: number = 0,
    clearCache: boolean = false,
    customPrompt: string | null = null
  ): Observable<any[]> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);
    
    // Build the URL with query parameters
    let url = `${this.apiUrl}/gemini/recommendations?playlistId=${playlistId}&includePreviewUrls=${includePreviewUrls}&limit=${limit}&offset=${offset}&clearCache=${clearCache}`;
    
    // Add custom prompt if available
    if (customPrompt && customPrompt.trim() !== '') {
      url += `&customPrompt=${encodeURIComponent(customPrompt.trim())}`;
    }
    
    this.http.get<any[]>(
      url,
      { withCredentials: true }
    ).subscribe({
      next: (recommendations) => {
        console.log(`Recommendations received (offset ${offset}):`, recommendations);
        
        if (includePreviewUrls) {

          this.enhancedRecommendationsSubject.next(recommendations);
          
          const simpleRecommendations = recommendations.map(item => item.recommendation || item);
          this.recommendationsSubject.next(simpleRecommendations);
        } else {
          this.recommendationsSubject.next(recommendations);
          this.enhancedRecommendationsSubject.next([]);
        }
        
        this.loadingSubject.next(false);
      },
      error: (err) => {
        console.error('Error fetching recommendations:', err);
        this.errorSubject.next('Failed to get song recommendations. Please try again.');
        this.loadingSubject.next(false);
      }
    });
    
    return includePreviewUrls ? this.enhancedRecommendations$ : this.recommendations$;
  }
  
  getTopTracksRecommendations(
    timeRange: string,
    includePreviewUrls: boolean = true,
    limit: number = 10,
    offset: number = 0,
    clearCache: boolean = false,
    customPrompt: string | null = null
  ): Observable<any[]> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);
    
    // Build the URL with query parameters
    let url = `${this.apiUrl}/gemini/top-tracks-recommendations?timeRange=${timeRange}&includePreviewUrls=${includePreviewUrls}&limit=${limit}&offset=${offset}&clearCache=${clearCache}`;
    
    // Add custom prompt if available
    if (customPrompt && customPrompt.trim() !== '') {
      url += `&customPrompt=${encodeURIComponent(customPrompt.trim())}`;
    }
    
    this.http.get<any[]>(
      url,
      { withCredentials: true }
    ).subscribe({
      next: (recommendations) => {
        console.log(`Top tracks recommendations received (offset ${offset}):`, recommendations);
        
        if (includePreviewUrls) {
          // If these are enhanced recommendations with preview URLs
          this.enhancedRecommendationsSubject.next(recommendations);
          
          // Also update the simple recommendations subject for backward compatibility
          const simpleRecommendations = recommendations.map(item => item.recommendation || item);
          this.recommendationsSubject.next(simpleRecommendations);
        } else {
          // Simple recommendations without preview URLs
          this.recommendationsSubject.next(recommendations);
          this.enhancedRecommendationsSubject.next([]);
        }
        
        this.loadingSubject.next(false);
      },
      error: (err) => {
        console.error('Error fetching top tracks recommendations:', err);
        this.errorSubject.next('Failed to get song recommendations. Please try again.');
        this.loadingSubject.next(false);
      }
    });
    
    return includePreviewUrls ? this.enhancedRecommendations$ : this.recommendations$;
  }
  
  getTrackPreview(query: string): Observable<RecommendationWithPreview> {
    return this.http.get<RecommendationWithPreview>(
      `${this.apiUrl}/spotify/preview?query=${encodeURIComponent(query)}`,
      { withCredentials: true }
    );
  }
  
  getPreviewsForRecommendations(recommendations: any[]): Observable<RecommendationWithPreview[]> {
    return this.http.post<RecommendationWithPreview[]>(
      `${this.apiUrl}/spotify/preview/batch`, 
      recommendations,
      { withCredentials: true }
    );
  }

  addTrackToPlaylist(playlistId: string, trackUri: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/spotify/playlists/${playlistId}/tracks`,
      { uri: trackUri },
      { withCredentials: true }
    );
  }
}