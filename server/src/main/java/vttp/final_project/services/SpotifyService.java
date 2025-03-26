package vttp.final_project.services;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vttp.final_project.models.SpotifyDTO;
import vttp.final_project.models.SpotifyDTO.PlaylistsDTO;

@Service
public class SpotifyService {

    @Autowired
    private SpotifyAuthService spotifyAuthService;

    RestTemplate restTemplate = new RestTemplate();

    Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
    
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/me",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching user profile: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getUserPlaylists(HttpServletRequest request, int limit, int offset) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            String url = String.format("https://api.spotify.com/v1/me/playlists?limit=%d&offset=%d", limit, offset);
            
            ResponseEntity<PlaylistsDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PlaylistsDTO.class
            );

            PlaylistsDTO playlists = response.getBody();

            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            logger.error("Error getting user playlists: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get user playlists: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getPlaylistById(HttpServletRequest request, String playlistId) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            String url = String.format("https://api.spotify.com/v1/playlists/%s", playlistId);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
    
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting playlist by ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get playlist: " + e.getMessage());
        }
    }

    public ResponseEntity<?> searchTracks(HttpServletRequest request, String query, int limit) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            String encodedQuery = UriComponentsBuilder.fromPath("")
                .queryParam("q", query)
                .build()
                .getQuery()
                .substring(2); // Remove "q=" prefix
            
            String url = String.format(
                "https://api.spotify.com/v1/search?q=%s&type=track&limit=%d",
                encodedQuery,
                limit
            );
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error searching for tracks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to search for tracks: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getSeveralTracks(HttpServletRequest request, String ids) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            String url = String.format("https://api.spotify.com/v1/tracks?ids=%s", ids);
            
            ResponseEntity<SpotifyDTO.TracksResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SpotifyDTO.TracksResponseDTO.class
            );
    
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting multiple tracks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get tracks: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getUserTopTracks(HttpServletRequest request,
                                    String timeRange,
                                    int limit,
                                    int offset) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            // Build the URL with query parameters
            String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/me/top/tracks")
                .queryParam("time_range", timeRange)
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .build()
                .toUriString();
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting user top tracks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get top tracks: " + e.getMessage());
        }
    }

    public ResponseEntity<?> addTrackToPlaylist(HttpServletRequest request, String playlistId, String trackUri) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        Optional<String> accessTokenOpt = spotifyAuthService.getValidAccessToken(userId);
        
        if (accessTokenOpt.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessTokenOpt.get());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Create request body
        String requestBody = String.format("{\"uris\":[\"%s\"]}", trackUri);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            // Spotify API endpoint to add tracks to a playlist
            String url = String.format("https://api.spotify.com/v1/playlists/%s/tracks", playlistId);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
    
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error adding track to playlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add track to playlist: " + e.getMessage());
        }
    }
}
