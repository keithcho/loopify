package vttp.final_project.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import vttp.final_project.services.ApiMetricsService;
import vttp.final_project.services.SpotifyService;


@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = "${app.frontend.url}", allowCredentials = "true")
public class SpotifyApiController {

    @Autowired
    SpotifyService spotifyService;

    @Autowired
    ApiMetricsService apiMetricsService;
    
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        // Track API usage metric
        apiMetricsService.incrementApiCounter("/api/spotify/profile");
        
        return spotifyService.getUserProfile(request);
    }

    @GetMapping("/playlists")
    public ResponseEntity<?> getUserPlaylists(HttpServletRequest request,
        @RequestParam(defaultValue = "5") int limit,
        @RequestParam(defaultValue = "0") int offset) {
        
        // Track API usage metric
        apiMetricsService.incrementApiCounter("/api/spotify/playlists");
        
        return spotifyService.getUserPlaylists(request, limit, offset);
    }

    @GetMapping("/playlists/{id}")
    public ResponseEntity<?> getPlaylistById(
        HttpServletRequest request,
        @PathVariable("id") String playlistId) {
        
        // Track API usage metric
        apiMetricsService.incrementApiCounter("/api/spotify/playlists/{id}");
        
        return spotifyService.getPlaylistById(request, playlistId);
    }

    @GetMapping("/tracks")
    public ResponseEntity<?> getSeveralTracks(HttpServletRequest request,
        @RequestParam String ids) {
        
        apiMetricsService.incrementApiCounter("/api/spotify/tracks");
        return spotifyService.getSeveralTracks(request, ids);
    }

    @PostMapping("/playlists/{id}/tracks")
    public ResponseEntity<?> addTrackToPlaylist(
        HttpServletRequest request,
        @PathVariable("id") String playlistId,
        @RequestBody Map<String, String> requestBody) {
        
        apiMetricsService.incrementApiCounter("/api/spotify/playlists/{id}/tracks");
        
        String trackUri = requestBody.get("uri");
        if (trackUri == null || trackUri.isEmpty()) {
            return ResponseEntity.badRequest().body("Track URI is required");
        }
        
        return spotifyService.addTrackToPlaylist(request, playlistId, trackUri);
    }

    @GetMapping("/top-tracks")
    public ResponseEntity<?> getUserTopTracks(
        HttpServletRequest request,
        @RequestParam(defaultValue = "medium_term") String timeRange,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset) {
        
        apiMetricsService.incrementApiCounter("/api/spotify/top-tracks");

        return spotifyService.getUserTopTracks(request, timeRange, limit, offset);
    }
}
