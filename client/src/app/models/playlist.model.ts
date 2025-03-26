// Represents image metadata
export interface SpotifyImage {
  url: string;
  height?: number;
  width?: number;
}

// External URLs
export interface ExternalUrls {
  spotify: string;
}

// Simplified owner information
export interface PlaylistOwner {
  id: string;
  displayName: string;
  external_urls: ExternalUrls;
}

// Artist information
export interface Artist {
  id: string;
  name: string;
  external_urls: ExternalUrls;
}

// Album information
export interface Album {
  id: string;
  name: string;
  images: SpotifyImage[];
  artists: Artist[];
  release_date?: string;
}

// Track information
export interface Track {
  id: string;
  name: string;
  duration_ms: number;
  preview_url: string | null;
  explicit: boolean;
  popularity: number;
  artists: Artist[];
  album: Album;
  external_urls: ExternalUrls;
}

// Paginated track collection
export interface TrackCollection {
  href: string;
  total: number;
  limit: number;
  offset: number;
  next: string | null;
  previous: string | null;
  items: PlaylistTrack[];
}

// Playlist track with added_at information
export interface PlaylistTrack {
  added_at: string;
  track: Track;
}

// The main Playlist model
export interface Playlist {
  id: string;
  name: string;
  description: string;
  images: SpotifyImage[];
  owner: PlaylistOwner;
  tracks: TrackCollection;
  collaborative: boolean;
  public: boolean;
  external_urls: ExternalUrls;
  uri: string;
}

// Paginated playlists collection - for playlist listing endpoints
export interface PlaylistCollection {
  href: string;
  total: number;
  limit: number;
  offset: number;
  next: string | null;
  previous: string | null;
  items: Playlist[];
}