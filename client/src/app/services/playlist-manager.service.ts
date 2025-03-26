import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, tap, finalize } from 'rxjs/operators';
import { Playlist, PlaylistCollection } from '../models/playlist.model';
import { SpotifyService } from './spotify.service';

@Injectable({
  providedIn: 'root'
})
export class PlaylistManagerService {
  private allPlaylistsSubject = new BehaviorSubject<Playlist[]>([]);
  private loadingAllPlaylistsSubject = new BehaviorSubject<boolean>(false);
  private allPlaylistsLoadedSubject = new BehaviorSubject<boolean>(false);
  private totalPlaylistsSubject = new BehaviorSubject<number>(0);
  
  allPlaylists$ = this.allPlaylistsSubject.asObservable();
  loadingAllPlaylists$ = this.loadingAllPlaylistsSubject.asObservable();
  allPlaylistsLoaded$ = this.allPlaylistsLoadedSubject.asObservable();
  totalPlaylists$ = this.totalPlaylistsSubject.asObservable();
  
  constructor(private spotifyService: SpotifyService) {}
  
  /**
   * Fetch all playlists for the current user
   * @returns An Observable that completes when all playlists have been fetched
   */
  fetchAllPlaylists(): Observable<Playlist[]> {
    // Return cached playlists if they're already loaded
    if (this.allPlaylistsLoadedSubject.value) {
      return of(this.allPlaylistsSubject.value);
    }
    
    // Avoid multiple simultaneous fetch operations
    if (this.loadingAllPlaylistsSubject.value) {
      return this.allPlaylists$;
    }
    
    this.loadingAllPlaylistsSubject.next(true);
    
    // First, fetch initial playlists to get the total count
    return new Observable<Playlist[]>(observer => {
      this.spotifyService.getPlaylistsObservable(1, 0)
        .pipe(
          tap(initialPlaylistData => {
            // Store the total number of playlists
            const totalPlaylists = initialPlaylistData.total;
            this.totalPlaylistsSubject.next(totalPlaylists);
            
            // If there are no playlists, complete early
            if (totalPlaylists === 0) {
              this.allPlaylistsLoadedSubject.next(true);
              this.loadingAllPlaylistsSubject.next(false);
              observer.next([]);
              observer.complete();
              return;
            }
            
            // Calculate how many API calls needed (max 50 per call)
            const callsNeeded = Math.ceil(totalPlaylists / 50);
            const calls = [];
            
            for (let i = 0; i < callsNeeded; i++) {
              calls.push(
                this.spotifyService.getPlaylistsObservable(50, i * 50).pipe(
                  catchError(err => {
                    console.error(`Error fetching playlist batch ${i}:`, err);
                    return of(null); // Return empty result for failed batch
                  })
                )
              );
            }
            
            // Execute all API calls in parallel
            forkJoin(calls)
              .pipe(
                finalize(() => {
                  this.loadingAllPlaylistsSubject.next(false);
                  this.allPlaylistsLoadedSubject.next(true);
                })
              )
              .subscribe({
                next: results => {
                  // Combine all playlists from successful calls
                  const allPlaylists: Playlist[] = [];
                  const seenIds = new Set<string>();
                  
                  results.forEach(playlistsBatch => {
                    if (playlistsBatch && playlistsBatch.items) {
                      playlistsBatch.items.forEach(playlist => {
                        if (!seenIds.has(playlist.id)) {
                          allPlaylists.push(playlist);
                          seenIds.add(playlist.id);
                        }
                      });
                    }
                  });
                  
                  // Sort playlists alphabetically for better search experience
                  allPlaylists.sort((a, b) => a.name.localeCompare(b.name));
                  
                  // Update the subjects
                  this.allPlaylistsSubject.next(allPlaylists);
                  
                  // Complete the observable
                  observer.next(allPlaylists);
                  observer.complete();
                },
                error: err => {
                  console.error('Error fetching all playlists:', err);
                  this.loadingAllPlaylistsSubject.next(false);
                  observer.error(err);
                }
              });
          }),
          catchError(err => {
            console.error('Error fetching initial playlist data:', err);
            this.loadingAllPlaylistsSubject.next(false);
            observer.error(err);
            return of([]);
          })
        )
        .subscribe();
    });
  }
  
  /**
   * Search across all playlists
   * @param query The search query
   * @returns Filtered list of playlists
   */
  searchPlaylists(query: string): Playlist[] {
    if (!query || !this.allPlaylistsSubject.value.length) {
      return this.allPlaylistsSubject.value;
    }
    
    const normalizedQuery = query.toLowerCase().trim();
    
    return this.allPlaylistsSubject.value.filter(playlist => 
      playlist.name.toLowerCase().includes(normalizedQuery) || 
      (playlist.description && playlist.description.toLowerCase().includes(normalizedQuery))
    );
  }
  
  /**
   * Reset the playlist cache
   */
  clearCache(): void {
    this.allPlaylistsSubject.next([]);
    this.allPlaylistsLoadedSubject.next(false);
    this.totalPlaylistsSubject.next(0);
  }
  
  /**
   * Get the current playlist cache
   */
  getCachedPlaylists(): Playlist[] {
    return this.allPlaylistsSubject.value;
  }
  
  /**
   * Check if all playlists have been loaded
   */
  areAllPlaylistsLoaded(): boolean {
    return this.allPlaylistsLoadedSubject.value;
  }
}