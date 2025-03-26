package vttp.final_project.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import vttp.final_project.services.SpotifyPreviewService;

@RestController
@RequestMapping("/api/spotify/preview")
@CrossOrigin(origins = "${app.frontend.url}", allowCredentials = "true")
public class SpotifyPreviewController {

    @Autowired
    private SpotifyPreviewService previewService;
    
    @GetMapping
    public ResponseEntity<?> getTrackPreview(
            HttpServletRequest request,
            @RequestParam String query) {
        
        Map<String, Object> result = previewService.getTrackPreviewUrl(request, query);
        
        if ((boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @PostMapping("/batch")
    public ResponseEntity<?> getPreviewsForRecommendations(
            HttpServletRequest request,
            @RequestBody List<Map<String, String>> recommendations) {
        
        List<Map<String, Object>> results = 
                previewService.getPreviewUrlsForRecommendations(request, recommendations);
        
        return ResponseEntity.ok(results);
    }
}