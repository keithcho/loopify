package vttp.final_project.models;

import java.util.List;

public class SpotifyDTO {

    // User Profile DTO
    public static class UserProfileDTO {
        private String id;
        private String displayName;
        private String email;
        private String country;
        private List<ImageDTO> images;
        private ExternalUrlsDTO externalUrls;
        private FollowersDTO followers;
        private String uri;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public List<ImageDTO> getImages() { return images; }
        public void setImages(List<ImageDTO> images) { this.images = images; }
        
        public ExternalUrlsDTO getExternalUrls() { return externalUrls; }
        public void setExternalUrls(ExternalUrlsDTO externalUrls) { this.externalUrls = externalUrls; }
        
        public FollowersDTO getFollowers() { return followers; }
        public void setFollowers(FollowersDTO followers) { this.followers = followers; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
    }

    // Playlist DTO
    public static class PlaylistDTO {
        private String id;
        private String name;
        private String description;
        private List<ImageDTO> images;
        private UserProfileDTO owner;
        private boolean isPublic;
        private boolean isCollaborative;
        private TracksDTO tracks;
        private String uri;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<ImageDTO> getImages() { return images; }
        public void setImages(List<ImageDTO> images) { this.images = images; }
        
        public UserProfileDTO getOwner() { return owner; }
        public void setOwner(UserProfileDTO owner) { this.owner = owner; }
        
        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
        
        public boolean isCollaborative() { return isCollaborative; }
        public void setCollaborative(boolean isCollaborative) { this.isCollaborative = isCollaborative; }
        
        public TracksDTO getTracks() { return tracks; }
        public void setTracks(TracksDTO tracks) { this.tracks = tracks; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
    }

    // Playlists DTO (Pagination wrapper)
    public static class PlaylistsDTO {
        private List<PlaylistDTO> items;
        private int total;
        private int limit;
        private int offset;
        private String href;
        private String next;
        private String previous;

        // Getters and setters
        public List<PlaylistDTO> getItems() { return items; }
        public void setItems(List<PlaylistDTO> items) { this.items = items; }
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        
        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }
    }

    // Track DTO
    public static class TrackDTO {
        private String id;
        private String name;
        private List<ArtistDTO> artists;
        private AlbumDTO album;
        private int durationMs;
        private int popularity;
        private String uri;
        private ExternalUrlsDTO externalUrls;
        private boolean isExplicit;
        private String previewUrl;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<ArtistDTO> getArtists() { return artists; }
        public void setArtists(List<ArtistDTO> artists) { this.artists = artists; }
        
        public AlbumDTO getAlbum() { return album; }
        public void setAlbum(AlbumDTO album) { this.album = album; }
        
        public int getDurationMs() { return durationMs; }
        public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
        
        public int getPopularity() { return popularity; }
        public void setPopularity(int popularity) { this.popularity = popularity; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public ExternalUrlsDTO getExternalUrls() { return externalUrls; }
        public void setExternalUrls(ExternalUrlsDTO externalUrls) { this.externalUrls = externalUrls; }
        
        public boolean isExplicit() { return isExplicit; }
        public void setExplicit(boolean isExplicit) { this.isExplicit = isExplicit; }

        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    }

    // Tracks response DTO
    public static class TracksResponseDTO {
        private List<TrackDTO> tracks;

        // Getters and setters
        public List<TrackDTO> getTracks() { return tracks; }
        public void setTracks(List<TrackDTO> tracks) { this.tracks = tracks; }
    }

    // Artist DTO
    public static class ArtistDTO {
        private String id;
        private String name;
        private List<ImageDTO> images;
        private List<String> genres;
        private int popularity;
        private String uri;
        private ExternalUrlsDTO externalUrls;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<ImageDTO> getImages() { return images; }
        public void setImages(List<ImageDTO> images) { this.images = images; }
        
        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }
        
        public int getPopularity() { return popularity; }
        public void setPopularity(int popularity) { this.popularity = popularity; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public ExternalUrlsDTO getExternalUrls() { return externalUrls; }
        public void setExternalUrls(ExternalUrlsDTO externalUrls) { this.externalUrls = externalUrls; }
    }

    // Album DTO
    public static class AlbumDTO {
        private String id;
        private String name;
        private List<ImageDTO> images;
        private List<ArtistDTO> artists;
        private String albumType;
        private String releaseDate;
        private String releaseDatePrecision;
        private int totalTracks;
        private String uri;
        private ExternalUrlsDTO externalUrls;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<ImageDTO> getImages() { return images; }
        public void setImages(List<ImageDTO> images) { this.images = images; }
        
        public List<ArtistDTO> getArtists() { return artists; }
        public void setArtists(List<ArtistDTO> artists) { this.artists = artists; }
        
        public String getAlbumType() { return albumType; }
        public void setAlbumType(String albumType) { this.albumType = albumType; }
        
        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
        
        public String getReleaseDatePrecision() { return releaseDatePrecision; }
        public void setReleaseDatePrecision(String releaseDatePrecision) { this.releaseDatePrecision = releaseDatePrecision; }
        
        public int getTotalTracks() { return totalTracks; }
        public void setTotalTracks(int totalTracks) { this.totalTracks = totalTracks; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public ExternalUrlsDTO getExternalUrls() { return externalUrls; }
        public void setExternalUrls(ExternalUrlsDTO externalUrls) { this.externalUrls = externalUrls; }
    }

    // Tracks Pagination Wrapper
    public static class TracksDTO {
        private List<TrackDTO> items;
        private int total;
        private int limit;
        private int offset;
        private String href;
        private String next;
        private String previous;

        // Getters and setters
        public List<TrackDTO> getItems() { return items; }
        public void setItems(List<TrackDTO> items) { this.items = items; }
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        
        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }
    }

    // Image DTO
    public static class ImageDTO {
        private String url;
        private int height;
        private int width;

        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
    }

    // External URLs DTO
    public static class ExternalUrlsDTO {
        private String spotify;

        // Getters and setters
        public String getSpotify() { return spotify; }
        public void setSpotify(String spotify) { this.spotify = spotify; }
    }

    // Followers DTO
    public static class FollowersDTO {
        private String href;
        private int total;

        // Getters and setters
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
    }
}