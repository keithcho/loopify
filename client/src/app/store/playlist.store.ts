import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, EMPTY, combineLatest, of } from 'rxjs';
import { 
  switchMap, 
  catchError, 
  withLatestFrom, 
  map, 
  tap, 
  debounceTime, 
  distinctUntilChanged, 
  take
} from 'rxjs/operators';
import { Playlist, PlaylistCollection } from '../models/playlist.model';
import { SpotifyService } from '../services/spotify.service';

export interface PlaylistState {
  playlists: Playlist[];
  filteredPlaylists: Playlist[];
  allPlaylists: Playlist[];
  loading: boolean;
  error: string | null;
  searchQuery: string;
  searchActive: boolean;
  pagination: {
    offset: number;
    limit: number;
    total: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
  allPlaylistsFetched: boolean;
  fetchingAllPlaylists: boolean;
}

@Injectable()
export class PlaylistStore extends ComponentStore<PlaylistState> {
  constructor(private spotifyService: SpotifyService) {
    super({
      playlists: [],
      filteredPlaylists: [],
      allPlaylists: [],
      loading: false,
      error: null,
      searchQuery: '',
      searchActive: false,
      pagination: {
        offset: 0,
        limit: 25,
        total: 0,
        hasNext: false,
        hasPrevious: false
      },
      allPlaylistsFetched: false,
      fetchingAllPlaylists: false
    });
  }

  // SELECTORS
  readonly playlists$ = this.select(state => state.playlists);
  readonly filteredPlaylists$ = this.select(state => state.filteredPlaylists);
  readonly loading$ = this.select(state => state.loading);
  readonly error$ = this.select(state => state.error);
  readonly searchQuery$ = this.select(state => state.searchQuery);
  readonly searchActive$ = this.select(state => state.searchActive);
  readonly pagination$ = this.select(state => state.pagination);
  readonly allPlaylistsFetched$ = this.select(state => state.allPlaylistsFetched);
  readonly fetchingAllPlaylists$ = this.select(state => state.fetchingAllPlaylists);
  
  readonly displayedPlaylists$ = this.select(
    this.searchActive$,
    this.playlists$,
    this.filteredPlaylists$,
    (searchActive, playlists, filteredPlaylists) => 
      searchActive ? filteredPlaylists : playlists
  );
  
  // Public methods to access state (instead of using protected get() method)
  getPaginationState(): Observable<PlaylistState['pagination']> {
    return this.pagination$;
  }

  getSearchQueryValue(): Observable<string> {
    return this.searchQuery$;
  }

  getSearchActiveStatus(): Observable<boolean> {
    return this.searchActive$;
  }

  getFilteredPlaylistsCount(): Observable<number> {
    return this.filteredPlaylists$.pipe(map(playlists => playlists.length));
  }

  readonly vm$ = this.select(
    this.playlists$,
    this.filteredPlaylists$,
    this.loading$,
    this.error$,
    this.searchQuery$,
    this.searchActive$,
    this.pagination$,
    this.allPlaylistsFetched$,
    this.fetchingAllPlaylists$,
    this.displayedPlaylists$,
    (
      playlists, 
      filteredPlaylists, 
      loading, 
      error, 
      searchQuery, 
      searchActive, 
      pagination,
      allPlaylistsFetched,
      fetchingAllPlaylists,
      displayedPlaylists
    ) => ({
      playlists,
      filteredPlaylists,
      loading,
      error,
      searchQuery,
      searchActive,
      pagination,
      allPlaylistsFetched,
      fetchingAllPlaylists,
      displayedPlaylists,
      // Helper properties for the view
      playlistCollection: {
        items: playlists,
        total: pagination.total,
        limit: pagination.limit,
        offset: pagination.offset,
        next: pagination.hasNext ? 'next' : null,
        previous: pagination.hasPrevious ? 'previous' : null
      } as PlaylistCollection
    })
  );

  // UPDATERS
  readonly setPlaylists = this.updater((state, playlists: Playlist[]) => ({
    ...state,
    playlists,
    loading: false
  }));

  readonly setFilteredPlaylists = this.updater((state, filteredPlaylists: Playlist[]) => ({
    ...state,
    filteredPlaylists,
    loading: false
  }));

  readonly setAllPlaylists = this.updater((state, allPlaylists: Playlist[]) => ({
    ...state,
    allPlaylists,
    allPlaylistsFetched: true,
    fetchingAllPlaylists: false
  }));

  readonly setLoading = this.updater((state, loading: boolean) => ({
    ...state,
    loading
  }));

  readonly setFetchingAllPlaylists = this.updater((state, fetchingAllPlaylists: boolean) => ({
    ...state,
    fetchingAllPlaylists
  }));

  readonly setError = this.updater((state, error: string | null) => ({
    ...state,
    error,
    loading: false
  }));

  readonly setSearchQuery = this.updater((state, searchQuery: string) => ({
    ...state,
    searchQuery
  }));

  readonly setSearchActive = this.updater((state, searchActive: boolean) => ({
    ...state,
    searchActive
  }));

  readonly updatePagination = this.updater((state, pagination: Partial<PlaylistState['pagination']>) => ({
    ...state,
    pagination: {
      ...state.pagination,
      ...pagination
    }
  }));

  readonly clearSearch = this.updater((state) => ({
    ...state,
    searchQuery: '',
    searchActive: false,
    filteredPlaylists: []
  }));

  // EFFECTS
  readonly fetchPlaylists = this.effect((params$: Observable<{limit?: number, offset?: number}>) => {
    return params$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(({ limit = 25, offset = 0 }) => 
        this.spotifyService.getPlaylistsObservable(limit, offset).pipe(
          tapResponse(
            (playlistCollection: PlaylistCollection) => {
              this.setPlaylists(playlistCollection.items);
              this.updatePagination({
                limit: playlistCollection.limit,
                offset: playlistCollection.offset,
                total: playlistCollection.total,
                hasNext: !!playlistCollection.next,
                hasPrevious: !!playlistCollection.previous
              });
              this.setLoading(false);
            },
            (error) => {
              console.error('Error fetching playlists:', error);
              this.setError('Failed to load Spotify playlists. Please try again.');
            }
          )
        )
      )
    );
  });

  readonly fetchAllPlaylists = this.effect((trigger$: Observable<void>) => {
    return trigger$.pipe(
      withLatestFrom(this.allPlaylistsFetched$, this.fetchingAllPlaylists$),
      switchMap(([_, allPlaylistsFetched, fetchingAllPlaylists]): Observable<unknown> => {
        // Return empty if already fetched or currently fetching
        if (allPlaylistsFetched || fetchingAllPlaylists) {
          return EMPTY;
        }

        this.setFetchingAllPlaylists(true);
        
        // First, get total count with a small request
        return this.spotifyService.getPlaylistsObservable(1, 0).pipe(
          switchMap((initialResponse) => {
            const totalPlaylists = initialResponse.total;
            const batchSize = 50; // Max per Spotify API
            const batchCount = Math.ceil(totalPlaylists / batchSize);
            const batchRequests: Observable<PlaylistCollection>[] = [];
            
            // Create a batch request for each offset
            for (let i = 0; i < batchCount; i++) {
              batchRequests.push(
                this.spotifyService.getPlaylistsObservable(batchSize, i * batchSize)
              );
            }
            
            // Combine all batch requests
            return combineLatest(batchRequests).pipe(
              map((responses) => {
                // Merge all playlists from all responses, remove duplicates
                const allPlaylists: Playlist[] = [];
                const seenIds = new Set<string>();
                
                responses.forEach(response => {
                  if (response && response.items) {
                    response.items.forEach(playlist => {
                      if (!seenIds.has(playlist.id)) {
                        allPlaylists.push(playlist);
                        seenIds.add(playlist.id);
                      }
                    });
                  }
                });
                
                // Sort alphabetically for better search experience
                return allPlaylists.sort((a, b) => a.name.localeCompare(b.name));
              }),
              tapResponse(
                (allPlaylists) => {
                  this.setAllPlaylists(allPlaylists);
                },
                (error) => {
                  console.error('Error fetching all playlists:', error);
                  this.setFetchingAllPlaylists(false);
                }
              )
            );
          }),
          catchError(() => {
            this.setFetchingAllPlaylists(false);
            return EMPTY;
          })
        );
      })
    );
  });

  readonly search = this.effect((searchQuery$: Observable<string>) => {
    return searchQuery$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      withLatestFrom(this.allPlaylistsFetched$),
      tap(([query, allPlaylistsFetched]) => {
        if (!query) {
          this.clearSearch();
          return;
        }
        
        this.setSearchQuery(query);
        this.setSearchActive(true);
        
        // If we don't have all playlists, fetch them
        if (!allPlaylistsFetched) {
          this.fetchAllPlaylists();
        }
      }),
      switchMap(([query, allPlaylistsFetched]): Observable<unknown> => {
        if (!query) {
          return EMPTY;
        }
        
        return this.select(state => state.allPlaylists).pipe(
          take(1),  // Just take the current value of allPlaylists
          map(allPlaylists => {
            if (allPlaylists.length === 0 && !allPlaylistsFetched) {
              // If still fetching all playlists, don't try to filter yet
              return [];
            }
            
            const normalizedQuery = query.toLowerCase().trim();
            
            // Filter all playlists based on the query
            return allPlaylists.filter(playlist => 
              playlist.name.toLowerCase().includes(normalizedQuery) || 
              (playlist.description && playlist.description.toLowerCase().includes(normalizedQuery))
            );
          }),
          tap(filteredPlaylists => {
            this.setFilteredPlaylists(filteredPlaylists);
          })
        );
      })
    );
  });

  readonly nextPage = this.effect((trigger$: Observable<void>) => {
    return trigger$.pipe(
      withLatestFrom(this.pagination$, this.searchActive$, this.filteredPlaylists$),
      switchMap(([_, pagination, searchActive, filteredPlaylists]): Observable<unknown> => {
        if (searchActive) {
          // Handle pagination locally for search results
          const nextOffset = pagination.offset + pagination.limit;
          const nextItems = filteredPlaylists.slice(nextOffset, nextOffset + pagination.limit);
          
          if (nextItems.length > 0) {
            this.updatePagination({
              offset: nextOffset,
              hasPrevious: true,
              hasNext: nextOffset + pagination.limit < filteredPlaylists.length
            });
            this.setPlaylists(nextItems);
          }
          return EMPTY;
        } else if (pagination.hasNext) {
          // Instead of returning the effect, we create a new Observable
          // that will emit a value that will trigger the fetchPlaylists effect
          const newOffset = pagination.offset + pagination.limit;
          // This creates a new Observable that emits once and completes
          return of({ limit: pagination.limit, offset: newOffset })
            .pipe(
              // Then we use the tap operator to trigger the fetchPlaylists effect
              tap(params => this.fetchPlaylists(params))
            );
        }
        return EMPTY;
      })
    );
  });

  readonly previousPage = this.effect((trigger$: Observable<void>) => {
    return trigger$.pipe(
      withLatestFrom(this.pagination$, this.searchActive$, this.filteredPlaylists$),
      switchMap(([_, pagination, searchActive, filteredPlaylists]): Observable<unknown> => {
        if (searchActive) {
          // Handle pagination locally for search results
          const prevOffset = Math.max(0, pagination.offset - pagination.limit);
          const prevItems = filteredPlaylists.slice(prevOffset, prevOffset + pagination.limit);
          
          if (prevItems.length > 0) {
            this.updatePagination({
              offset: prevOffset,
              hasPrevious: prevOffset > 0,
              hasNext: true
            });
            this.setPlaylists(prevItems);
          }
          return EMPTY;
        } else if (pagination.hasPrevious) {
          // Regular pagination from API - same fix as in nextPage
          const newOffset = Math.max(0, pagination.offset - pagination.limit);
          return of({ limit: pagination.limit, offset: newOffset })
            .pipe(
              tap(params => this.fetchPlaylists(params))
            );
        }
        return EMPTY;
      })
    );
  });
}