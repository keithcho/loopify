package vttp.final_project.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SpotifyPreviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpotifyPreviewService.class);
    
    @Autowired
    private SpotifyService spotifyService;
    
    private RestTemplate restTemplate = new RestTemplate();
    
    // Regular expression to find preview URLs in Spotify page source
    private static final Pattern PREVIEW_URL_PATTERN = Pattern.compile("https://p\\.scdn\\.co/mp3-preview/[a-zA-Z0-9]+");
    
    // Maximum query length for Spotify search (conservative value)
    private static final int MAX_QUERY_LENGTH = 200;
    
    /**
     * Search for a track on Spotify and get its preview URL
     * 
     * @param request The HTTP request
     * @param query The search query (song title and artist)
     * @return A map containing track information and preview URL
     */
    public Map<String, Object> getTrackPreviewUrl(HttpServletRequest request, String query) {
        logger.info("Searching for track, query length: {}", query != null ? query.length() : 0);
        
        // Sanitize the query to prevent excessively long requests
        String sanitizedQuery = sanitizeSearchQuery(query);
        logger.info("Sanitized query length: {}", sanitizedQuery.length());
        
        // Step 1: Search Spotify API for the track
        ResponseEntity<?> searchResponse = spotifyService.searchTracks(request, sanitizedQuery, 1);
        
        if (!searchResponse.getStatusCode().is2xxSuccessful()) {
            logger.error("Failed to search for track: {}", sanitizedQuery);
            return createErrorResponse("Failed to search for track on Spotify API");
        }
        
        // Process the search results to get the track ID
        Map<String, Object> searchResults = (Map<String, Object>) searchResponse.getBody();
        List<Map<String, Object>> tracks = extractTracksFromSearchResults(searchResults);
        
        if (tracks.isEmpty()) {
            logger.warn("No tracks found for query: {}", sanitizedQuery);
            return createErrorResponse("No tracks found matching the query");
        }
        
        Map<String, Object> track = tracks.get(0);
        String trackId = (String) track.get("id");
        Map<String, Object> externalUrls = (Map<String, Object>) track.get("external_urls");
        String spotifyUrl = (String) externalUrls.get("spotify");
        
        logger.info("Found track: {} with URL: {}", track.get("name"), spotifyUrl);
        
        // Step 2: Scrape the Spotify web page to find preview URLs
        Set<String> previewUrls = scrapePreviewUrls(spotifyUrl);
        
        if (previewUrls.isEmpty()) {
            logger.warn("No preview URLs found for track: {}", track.get("name"));
        } else {
            logger.info("Found {} preview URLs for track: {}", previewUrls.size(), track.get("name"));
        }
        
        // Create the response with track info and preview URLs
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("track", track);
        response.put("previewUrls", new ArrayList<>(previewUrls));
        
        return response;
    }
    
    /**
     * Sanitize and limit the length of the search query
     * 
     * @param query The original search query
     * @return A sanitized and truncated query
     */
    private String sanitizeSearchQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        
        // Extract song title from JSON if present
        if (query.contains("\"song_title\"")) {
            try {
                Pattern titlePattern = Pattern.compile("\"song_title\"\\s*:\\s*\"([^\"]+)\"");
                Matcher titleMatcher = titlePattern.matcher(query);
                
                if (titleMatcher.find()) {
                    String title = titleMatcher.group(1);
                    return sanitizeAndTruncate(title);
                }
            } catch (Exception e) {
                logger.warn("Error parsing JSON-like query: {}", e.getMessage());
            }
        }
        
        return sanitizeAndTruncate(query);
    }
    
    /**
     * Sanitize and truncate a string to the maximum query length
     * 
     * @param input The input string
     * @return A sanitized and truncated string
     */
    private String sanitizeAndTruncate(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove any excessively repeated artist names or sections
        input = input.replaceAll("(\\s*&\\s*[^&]+){5,}", " & others");
        
        // If still too long, truncate to the maximum length
        if (input.length() > MAX_QUERY_LENGTH) {
            logger.warn("Truncating query from {} to {} characters", input.length(), MAX_QUERY_LENGTH);
            return input.substring(0, MAX_QUERY_LENGTH);
        }
        
        return input;
    }
    
    /**
     * Get preview URLs for multiple tracks based on recommendations
     * Modified to handle recommendations with song titles only
     * 
     * @param request The HTTP request
     * @param recommendations List of recommended tracks with song_title
     * @return List of track information with preview URLs
     */
    public List<Map<String, Object>> getPreviewUrlsForRecommendations(
            HttpServletRequest request, List<Map<String, String>> recommendations) {
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map<String, String> recommendation : recommendations) {
            String songTitle = recommendation.get("song_title");
            
            if (songTitle == null || songTitle.isEmpty()) {
                logger.warn("Skipping recommendation with missing song title");
                continue;
            }
            
            // Use song title as the query
            String query = songTitle;
            
            try {
                Map<String, Object> result = getTrackPreviewUrl(request, query);
                if ((boolean) result.get("success")) {
                    // Add the original recommendation data
                    result.put("recommendation", recommendation);
                    
                    // Extract artist from Spotify track for display (if available)
                    if (result.containsKey("track")) {
                        Map<String, Object> track = (Map<String, Object>) result.get("track");
                        if (track.containsKey("artists") && track.get("artists") instanceof List) {
                            List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                            if (!artists.isEmpty()) {
                                String spotifyArtist = (String) artists.get(0).get("name");
                                // Update the recommendation with the artist from Spotify
                                ((Map<String, String>)result.get("recommendation")).put("artist", spotifyArtist);
                            }
                        }
                    }
                    
                    results.add(result);
                } else {
                    logger.warn("Failed to get preview URL for: {}", query);
                }
            } catch (Exception e) {
                logger.error("Error getting preview URL for: {}", query, e);
            }
        }
        
        return results;
    }
    
    /**
     * Scrape the Spotify web page to find preview URLs
     * 
     * @param spotifyUrl The Spotify track URL
     * @return Set of preview URLs found
     */
    private Set<String> scrapePreviewUrls(String spotifyUrl) {
        Set<String> previewUrls = new HashSet<>();
        
        try {
            // Set user agent to mimic a browser
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Get the Spotify web page content
            ResponseEntity<String> response = restTemplate.exchange(
                spotifyUrl, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String html = response.getBody();
                
                // Use regex to find all preview URLs
                Matcher matcher = PREVIEW_URL_PATTERN.matcher(html);
                while (matcher.find()) {
                    previewUrls.add(matcher.group());
                }
                
                // If regex didn't find anything, try Jsoup parsing as backup
                if (previewUrls.isEmpty() && html != null) {
                    Document doc = Jsoup.parse(html);
                    
                    // Look for elements with attributes containing preview URLs
                    Elements elements = doc.select("*[src*=scdn.co], *[href*=scdn.co], *[data-url*=scdn.co]");
                    for (Element element : elements) {
                        for (String attrName : element.attributes().asList().stream()
                                .map(attr -> attr.getKey()).toList()) {
                            String attrValue = element.attr(attrName);
                            if (attrValue.contains("p.scdn.co/mp3-preview/")) {
                                previewUrls.add(attrValue);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scraping preview URLs from: {}", spotifyUrl, e);
        }
        
        return previewUrls;
    }
    
    /**
     * Extract tracks from Spotify search results
     * 
     * @param searchResults The search results from Spotify API
     * @return List of track objects
     */
    private List<Map<String, Object>> extractTracksFromSearchResults(Map<String, Object> searchResults) {
        try {
            Map<String, Object> tracks = (Map<String, Object>) searchResults.get("tracks");
            if (tracks != null) {
                return (List<Map<String, Object>>) tracks.get("items");
            }
        } catch (Exception e) {
            logger.error("Error extracting tracks from search results", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Create an error response
     * 
     * @param message The error message
     * @return Map containing error information
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}