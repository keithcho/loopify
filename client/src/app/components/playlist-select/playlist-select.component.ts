import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Playlist, PlaylistCollection } from '../../models/playlist.model';
import { Router } from '@angular/router';
import { PlaylistStore } from '../../store/playlist.store';
import { Observable, map, take } from 'rxjs';

@Component({
  selector: 'app-playlist-select',
  standalone: false,
  templateUrl: './playlist-select.component.html',
  styleUrl: './playlist-select.component.css',
  providers: [PlaylistStore] // Provide the store at the component level
})
export class PlaylistSelectComponent implements OnInit, OnDestroy {
  // Inject dependencies
  private router = inject(Router);
  private playlistStore = inject(PlaylistStore);
  
  // Use the view model from the store
  readonly vm$ = this.playlistStore.vm$;
  private searchQueryValue = '';
  
  // Create an observable for the page info text
  readonly pageInfo$ = this.getPageInfoObservable();
  
  // Use Observable for current searchQuery instead of direct getter
  get searchQuery(): string {
    return this.searchQueryValue;
  }
  
  set searchQuery(value: string) {
    this.searchQueryValue = value;
    this.playlistStore.setSearchQuery(value);
    this.playlistStore.search(value);
  }
  
  ngOnInit(): void {
    // Fetch initial set of playlists when component is initialized
    this.playlistStore.fetchPlaylists({ limit: 25, offset: 0 });
    
    // Sync our local property with the store
    this.playlistStore.searchQuery$.subscribe(query => {
      this.searchQueryValue = query;
    });
  }
  
  ngOnDestroy(): void {
    // No explicit subscription cleanup needed as ComponentStore handles this internally
  }
  
  onSearchChange(): void {
    // The search is already handled through the setter of searchQuery property
    if (!this.searchQuery) {
      this.clearSearch();
    }
  }
  
  clearSearch(): void {
    this.playlistStore.clearSearch();
    // Re-fetch the playlists with standard pagination
    this.playlistStore.pagination$.pipe(
      take(1)
    ).subscribe(pagination => {
      this.playlistStore.fetchPlaylists({
        limit: pagination.limit,
        offset: 0
      });
    });
  }

  fetchPlaylists(limit: number = 25, offset: number = 0): void {
    this.playlistStore.fetchPlaylists({ limit, offset });
  }
  
  selectPlaylist(playlist: Playlist): void {
    // Navigate to the song player component with the playlist ID and name
    this.router.navigate(['/player', playlist.id, playlist.name]);
  }
  
  nextPage(): void {
    this.playlistStore.nextPage();
  }
  
  previousPage(): void {
    this.playlistStore.previousPage();
  }
  
  // Return an observable instead of a string
  private getPageInfoObservable(): Observable<string> {
    return this.vm$.pipe(
      map(vm => {
        // For search results, show the actual totals
        if (vm.searchActive) {
          return `Showing all ${vm.filteredPlaylists.length} matching playlists`;
        } else {
          // For regular pagination, show API totals
          const startItem = vm.pagination.offset + 1;
          const endItem = Math.min(
            vm.pagination.offset + vm.playlists.length,
            vm.pagination.total
          );
          
          return `Showing ${startItem}-${endItem} of ${vm.pagination.total}`;
        }
      })
    );
  }
  
  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}