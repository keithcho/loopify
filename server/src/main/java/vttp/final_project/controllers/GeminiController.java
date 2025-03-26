package vttp.final_project.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vttp.final_project.services.ApiMetricsService;
import vttp.final_project.services.GeminiService;
import vttp.final_project.services.SpotifyPreviewService;
import vttp.final_project.services.SpotifyService;

@RestController
@RequestMapping("/api/gemini")
@CrossOrigin(origins = "${app.frontend.url}", allowCredentials = "true")
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    @Autowired
    private GeminiService geminiService;
    
    @Autowired
    private SpotifyService spotifyService;
    
    @Autowired
    private SpotifyPreviewService previewService;
    
    @Autowired
    private ApiMetricsService apiMetricsService;
    
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(
            HttpServletRequest request,
            @RequestParam String playlistId,
            @RequestParam(defaultValue = "false") boolean includePreviewUrls,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "false") boolean clearCache,
            @RequestParam(required = false) String customPrompt) {
        
        // Track API usage metric
        apiMetricsService.incrementApiCounter("/api/gemini/recommendations");
        
        logger.info("Getting recommendations for playlist: {}, limit: {}, offset: {}, clearCache: {}, customPrompt: {}", 
                playlistId, limit, offset, clearCache, customPrompt != null ? "'" + customPrompt + "'" : "null");
        
        // Clear the cache if requested
        if (clearCache && offset == 0) {
            geminiService.clearRecommendationsCache(playlistId);
        }
        
        // First, get the playlist tracks from Spotify
        ResponseEntity<?> playlistResponse = spotifyService.getPlaylistById(request, playlistId);
        
        if (!playlistResponse.getStatusCode().is2xxSuccessful()) {
            return playlistResponse;
        }
        
        // Pass the playlist data to GeminiService to get recommendations with limit and offset
        ResponseEntity<?> recommendationsResponse = geminiService.getSongRecommendationsForPlaylist(
                playlistResponse.getBody(), limit, offset, customPrompt);
        
        if (!recommendationsResponse.getStatusCode().is2xxSuccessful() || !includePreviewUrls) {
            return recommendationsResponse;
        }
        
        // Get preview URLs for the recommendations
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> recommendations = (List<Map<String, String>>) recommendationsResponse.getBody();
            
            List<Map<String, Object>> enhancedRecommendations = 
                    previewService.getPreviewUrlsForRecommendations(request, recommendations);
            
            return ResponseEntity.ok(enhancedRecommendations);
        } catch (Exception e) {
            logger.error("Error enhancing recommendations with preview URLs", e);
            return ResponseEntity.ok(recommendationsResponse.getBody());
        }
    }
    
    @GetMapping("/top-tracks-recommendations")
    public ResponseEntity<?> getTopTracksRecommendations(
            HttpServletRequest request,
            @RequestParam String timeRange,
            @RequestParam(defaultValue = "false") boolean includePreviewUrls,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "false") boolean clearCache,
            @RequestParam(required = false) String customPrompt) {
        
        // Track API usage metric
        apiMetricsService.incrementApiCounter("/api/gemini/top-tracks-recommendations");
        
        logger.info("Getting recommendations based on top tracks: {}, limit: {}, offset: {}, clearCache: {}, customPrompt: {}", 
                timeRange, limit, offset, clearCache, customPrompt != null ? "'" + customPrompt + "'" : "null");
        
        // Generate a unique cache key based on user ID + time range
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String userId = (String) session.getAttribute("userId");
        String cacheKey = userId + "_top_tracks_" + timeRange;
        
        // Clear the cache if requested
        if (clearCache && offset == 0) {
            geminiService.clearRecommendationsCache(cacheKey);
        }
        
        // First, get the user's top tracks from Spotify
        ResponseEntity<?> topTracksResponse = spotifyService.getUserTopTracks(request, timeRange, 20, 0);
        
        if (!topTracksResponse.getStatusCode().is2xxSuccessful()) {
            return topTracksResponse; // Return the error response if top tracks fetch failed
        }
        
        // Pass the top tracks data to GeminiService to get recommendations with limit and offset
        // Use the cacheKey instead of playlist ID
        ResponseEntity<?> recommendationsResponse = geminiService.getSongRecommendationsForTopTracks(
                topTracksResponse.getBody(), cacheKey, limit, offset, customPrompt);
        
        if (!recommendationsResponse.getStatusCode().is2xxSuccessful() || !includePreviewUrls) {
            return recommendationsResponse;
        }
        
        // Get preview URLs for the recommendations
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> recommendations = (List<Map<String, String>>) recommendationsResponse.getBody();
            
            List<Map<String, Object>> enhancedRecommendations = 
                    previewService.getPreviewUrlsForRecommendations(request, recommendations);
            
            return ResponseEntity.ok(enhancedRecommendations);
        } catch (Exception e) {
            logger.error("Error enhancing top tracks recommendations with preview URLs", e);
            return ResponseEntity.ok(recommendationsResponse.getBody());
        }
    }
}