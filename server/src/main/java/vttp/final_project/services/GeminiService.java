package vttp.final_project.services;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

@Service
public class GeminiService {

    @Value("${gemini.api-key}")
    private String geminiApiKey;
    
    @Autowired
    private FallbackService fallbackService;

    @Autowired
    private GeminiRedisService redisService;

    @Autowired
    private ApiMetricsService apiMetricsService;
    
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second delay between retries

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    
    private RestTemplate restTemplate = new RestTemplate();
    
    public ResponseEntity<?> getSongRecommendationsForPlaylist(Object playlistData) {
        return getSongRecommendationsForPlaylist(playlistData, 10, 0, null);
    }

    public ResponseEntity<?> getSongRecommendationsForPlaylist(Object playlistData, int limit, int offset) {
        return getSongRecommendationsForPlaylist(playlistData, limit, offset, null);
    }
    
    public ResponseEntity<?> getSongRecommendationsForPlaylist(Object playlistData, int limit, int offset, String customPrompt) {
        try {
            // Extract track titles from the playlist
            List<String> trackTitles = extractTrackTitles(playlistData);
            
            if (trackTitles.isEmpty()) {
                return ResponseEntity.badRequest().body("No tracks found in the playlist");
            }
            
            // Generate a playlist ID to use as cache key
            Map<String, Object> playlistMap = (Map<String, Object>) playlistData;
            String playlistId = (String) playlistMap.get("id");
            
            // Check if the custom prompt has changed - use Redis instead of in-memory map
            String cachedPrompt = redisService.getCachedCustomPrompt(playlistId);
            boolean promptChanged = (customPrompt != null && !customPrompt.equals(cachedPrompt)) || 
                                   (customPrompt == null && cachedPrompt != null);
            
            // If the prompt has changed, clear the cache for this playlist
            if (promptChanged) {
                logger.info("Custom prompt changed from '{}' to '{}', clearing cache for playlist {}",
                           cachedPrompt, customPrompt, playlistId);
                redisService.clearCachedRecommendations(playlistId);
                
                // Update the cached prompt in Redis
                if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                    redisService.cacheCustomPrompt(playlistId, customPrompt);
                } else {
                    redisService.clearCachedCustomPrompt(playlistId);
                }
            }
            
            // Get recommendations based on these tracks
            List<Map<String, String>> recommendations = getGeminiSongRecommendations(
                trackTitles, playlistId, limit, offset, customPrompt);
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            logger.error("Error getting song recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting song recommendations: " + e.getMessage());
        }
    }
    
    private List<String> extractTrackTitles(Object playlistData) {
        List<String> trackTitles = new ArrayList<>();
        
        try {
            // Cast to Map to navigate the JSON structure
            Map<String, Object> playlistMap = (Map<String, Object>) playlistData;
            
            // Access the tracks object
            Map<String, Object> tracksMap = (Map<String, Object>) playlistMap.get("tracks");
            
            // Get the items array which contains the tracks
            List<Map<String, Object>> items = (List<Map<String, Object>>) tracksMap.get("items");
            
            // Extract track names from each item
            for (Map<String, Object> item : items) {
                Map<String, Object> track = (Map<String, Object>) item.get("track");
                if (track != null) {
                    String trackName = (String) track.get("name");
                    
                    // Get artist name
                    List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                    if (artists != null && !artists.isEmpty()) {
                        String artistName = (String) artists.get(0).get("name");
                        trackTitles.add(trackName + " by " + artistName);
                    } else {
                        trackTitles.add(trackName);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting track titles: {}", e.getMessage(), e);
        }
        
        return trackTitles;
    }

    /**
     * Get song recommendations from Gemini API with metrics tracking
     * 
     * @param trackTitles List of track titles to base recommendations on
     * @param playlistId Playlist ID or cache key for storing recommendations
     * @param limit Maximum number of recommendations to return
     * @param offset Starting index for pagination
     * @param customPrompt Optional custom prompt to influence recommendations
     * @return List of song recommendations
     */
    @Timed(value = "app.gemini.recommendations.time", description = "Time taken to get song recommendations")
    public List<Map<String, String>> getGeminiSongRecommendations(
            List<String> trackTitles, String playlistId, int limit, int offset, String customPrompt) {
        
        // Start a timer for tracking the entire method execution
        Timer.Sample methodTimer = Timer.start();
        
        try {
            // Check if we have already generated recommendations for this playlist
            List<Map<String, String>> cachedRecommendations = redisService.getCachedRecommendations(playlistId);
            
            // If we have cached recommendations and the offset is within range, use those
            if (cachedRecommendations != null && offset < cachedRecommendations.size()) {
                logger.info("Using cached recommendations for playlist {} (offset {})", playlistId, offset);
                
                int endIndex = Math.min(offset + limit, cachedRecommendations.size());
                return cachedRecommendations.subList(offset, endIndex);
            }
            
            // If this is a new request or we need more recommendations than we have cached
            int recommendationsToGenerate = 30;
            if (cachedRecommendations != null) {
                // Generate more recommendations to add to cache
                logger.info("Generating more recommendations to add to cache for playlist {}", playlistId);
            }
            
            String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
            String fullUrl = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("key", geminiApiKey)
                    .build()
                    .toUriString();

            // Limit to at most 20 tracks to keep the prompt size reasonable
            List<String> limitedTracks = trackTitles.stream().limit(20).collect(Collectors.toList());
            
            // Create the playlist tracks list as a string
            String tracksString = limitedTracks.stream()
                    .map(title -> "- " + title)
                    .collect(Collectors.joining("\n"));
            
            // Build the prompt differently based on whether this is an initial or follow-up request
            String prompt;
            
            if (cachedRecommendations == null || cachedRecommendations.isEmpty()) {
                prompt = "Based on the following playlist tracks:\n\n" + 
                        tracksString + 
                        "\n\nRecommend " + recommendationsToGenerate + " new songs that would fit well with this playlist. ";
                
                // Add custom prompt instruction if provided
                if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                    prompt += "I want you to " + customPrompt.trim() + ". ";
                }
                
                prompt += """
                        Only provide the song titles without artists. The recommendations should feel cohesive with the existing playlist.
                        DO NOT recommend songs that are already in the input list.
                        
                        Your response should be a JSON array with objects containing only 'song_title' field. For example:
                        [
                            { "song_title": "Example Song" },
                            { "song_title": "Another Song" }
                        ]""";
            } else {
                // Follow-up request - ask for more recommendations, avoiding previous ones
                String previousRecommendations = cachedRecommendations.stream()
                    .map(rec -> rec.get("song_title"))
                    .collect(Collectors.joining("\n- ", "- ", ""));
                
                prompt = "Based on the following playlist tracks:\n\n" + 
                        tracksString + 
                        "\n\nI already recommended these songs:\n\n" +
                        previousRecommendations +
                        "\n\nRecommend " + recommendationsToGenerate + " MORE new songs that would fit well with this playlist. ";
                
                // Add custom prompt instruction if provided
                if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                    prompt += "I want you to " + customPrompt.trim() + ". ";
                }
                
                prompt += """
                        Only provide the song titles without artists. The recommendations should feel cohesive with the existing playlist.
                        DO NOT recommend songs that are already in the input list or in the previously recommended songs list.
                        
                        Your response should be a JSON array with objects containing only 'song_title' field. For example:
                        [
                            { "song_title": "Example Song" },
                            { "song_title": "Another Song" }
                        ]""";
            }
            
            logger.info("Requesting {} more song recommendations with prompt length: {}", 
                    recommendationsToGenerate, prompt.length());
            
            if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                logger.info("Including custom prompt: '{}'", customPrompt);
            }

            // Create the request payload using Jakarta JSON
            JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
            
            // Build "contents" array
            JsonArrayBuilder contentsBuilder = Json.createArrayBuilder();
            JsonObjectBuilder contentItemBuilder = Json.createObjectBuilder();
            
            // Build "parts" array
            JsonArrayBuilder partsBuilder = Json.createArrayBuilder();
            JsonObjectBuilder partBuilder = Json.createObjectBuilder();
            partBuilder.add("text", prompt);
            partsBuilder.add(partBuilder);
            
            contentItemBuilder.add("parts", partsBuilder);
            contentsBuilder.add(contentItemBuilder);
            payloadBuilder.add("contents", contentsBuilder);
            
            // Build "generationConfig" object
            JsonObjectBuilder generationConfigBuilder = Json.createObjectBuilder();
            generationConfigBuilder.add("response_mime_type", "application/json");
            
            // Build "response_schema" object
            JsonObjectBuilder responseSchemaBuilder = Json.createObjectBuilder();
            responseSchemaBuilder.add("type", "ARRAY");
            
            // Build "items" object
            JsonObjectBuilder itemsBuilder = Json.createObjectBuilder();
            itemsBuilder.add("type", "OBJECT");
            
            // Build "properties" object
            JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
            
            // Build "song_title" object (no artist property now)
            JsonObjectBuilder songTitleBuilder = Json.createObjectBuilder();
            songTitleBuilder.add("type", "STRING");
            
            propertiesBuilder.add("song_title", songTitleBuilder);
            itemsBuilder.add("properties", propertiesBuilder);
            responseSchemaBuilder.add("items", itemsBuilder);
            
            generationConfigBuilder.add("response_schema", responseSchemaBuilder);
            
            // Set a lower temperature for more predictable responses
            generationConfigBuilder.add("temperature", 0.2);
            
            payloadBuilder.add("generationConfig", generationConfigBuilder);
            
            // Build the final payload
            JsonObject payload = payloadBuilder.build();
            String jsonPayload = payload.toString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the HTTP entity
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
            
            // Implement retry logic
            int retryCount = 0;
            boolean success = false;
            List<Map<String, String>> newRecommendations = new ArrayList<>();
            ResponseEntity<String> response = null;

            while (!success && retryCount < MAX_RETRIES) {
                try {
                    // Log the attempt
                    logger.info("Attempt #{} to get recommendations from Gemini API", retryCount + 1);
                    
                    // Track the start time for the Gemini API call specifically
                    long geminiCallStartTime = System.currentTimeMillis();
                    
                    // Make the POST request
                    response = restTemplate.exchange(
                        fullUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                    );

                    // Record the Gemini API call duration using our metrics service
                    long geminiCallDuration = System.currentTimeMillis() - geminiCallStartTime;
                    apiMetricsService.recordGeminiResponseTime(geminiCallDuration);
                    logger.info("Gemini API call took {} ms", geminiCallDuration);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        String responseBody = response.getBody();
                        logger.info("Received successful response from Gemini API with {} characters", 
                            responseBody != null ? responseBody.length() : 0);
                        
                        try {
                            // Try to parse the recommendations
                            newRecommendations = parseSongRecommendations(responseBody);
                            
                            // If we got some recommendations, consider it a success
                            if (!newRecommendations.isEmpty()) {
                                logger.info("Successfully parsed {} recommendations", newRecommendations.size());
                                success = true;
                            } else {
                                // Empty recommendations, retry with a different approach
                                logger.warn("Received empty recommendations list from Gemini API, retrying with modified prompt");
                                retryCount++;
                                
                                // Modify the prompt slightly for the retry
                                String retryPrompt = prompt + "\n\nPlease only provide song titles. Your response should be in valid JSON format.";
                                
                                // Rebuild the request payload with the updated prompt
                                payloadBuilder = Json.createObjectBuilder();
                                contentsBuilder = Json.createArrayBuilder();
                                contentItemBuilder = Json.createObjectBuilder();
                                partsBuilder = Json.createArrayBuilder();
                                partBuilder = Json.createObjectBuilder();
                                
                                partBuilder.add("text", retryPrompt);
                                partsBuilder.add(partBuilder);
                                contentItemBuilder.add("parts", partsBuilder);
                                contentsBuilder.add(contentItemBuilder);
                                payloadBuilder.add("contents", contentsBuilder);
                                payloadBuilder.add("generationConfig", generationConfigBuilder);
                                
                                payload = payloadBuilder.build();
                                jsonPayload = payload.toString();
                                
                                requestEntity = new HttpEntity<>(jsonPayload, headers);
                            }
                        } catch (Exception e) {
                            logger.error("Error parsing response: {}", e.getMessage());
                            retryCount++;
                            
                            // Try to extract recommendations using a more lenient approach on the next attempt
                            if (retryCount < MAX_RETRIES) {
                                logger.info("Retrying with a simplified prompt format");
                                
                                // Simplify the prompt for the next attempt
                                String retryPrompt = "Based on the playlist tracks, recommend " + recommendationsToGenerate + 
                                        " songs. Just give me the song titles only, one per line.";
                                
                                // Add custom prompt if available
                                if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                                    retryPrompt += " " + customPrompt.trim() + ".";
                                }
                                
                                // Rebuild the request payload with the simplified prompt
                                payloadBuilder = Json.createObjectBuilder();
                                contentsBuilder = Json.createArrayBuilder();
                                contentItemBuilder = Json.createObjectBuilder();
                                partsBuilder = Json.createArrayBuilder();
                                partBuilder = Json.createObjectBuilder();
                                
                                partBuilder.add("text", retryPrompt);
                                partsBuilder.add(partBuilder);
                                contentItemBuilder.add("parts", partsBuilder);
                                contentsBuilder.add(contentItemBuilder);
                                payloadBuilder.add("contents", contentsBuilder);
                                
                                // Adjust generation config for more reliable output
                                JsonObjectBuilder simpleConfigBuilder = Json.createObjectBuilder();
                                simpleConfigBuilder.add("temperature", 0.1); // Lower temperature for more predictable output
                                
                                payloadBuilder.add("generationConfig", simpleConfigBuilder);
                                
                                payload = payloadBuilder.build();
                                jsonPayload = payload.toString();
                                
                                requestEntity = new HttpEntity<>(jsonPayload, headers);
                            }
                        }
                    } else {
                        logger.error("Request failed with status code: {}", response.getStatusCode());
                        logger.error("Response body: {}", response.getBody());
                        retryCount++;
                    }
                } catch (Exception e) {
                    logger.error("Error communicating with Gemini API: {}", e.getMessage());
                    retryCount++;
                }
                
                // Add a delay between retries
                if (!success && retryCount < MAX_RETRIES) {
                    try {
                        logger.info("Waiting {} ms before retry #{}", RETRY_DELAY_MS, retryCount + 1);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // If all retries failed or we got no recommendations, provide fallback recommendations
            if (!success || newRecommendations.isEmpty()) {
                logger.warn("All {} attempts to get recommendations from Gemini API failed, using fallback recommendations", MAX_RETRIES);
                newRecommendations = fallbackService.getFallbackRecommendations(trackTitles);
            }
            
            // Update the cache
            if (cachedRecommendations == null) {
                // First time, just store the new recommendations
                redisService.cacheRecommendations(playlistId, newRecommendations);
            } else {
                // Merge with existing recommendations
                List<Map<String, String>> allRecommendations = new ArrayList<>(cachedRecommendations);
                
                // Filter out duplicates
                for (Map<String, String> newRec : newRecommendations) {
                    boolean isDuplicate = allRecommendations.stream().anyMatch(existing -> 
                        existing.get("song_title").equalsIgnoreCase(newRec.get("song_title"))
                    );
                    
                    if (!isDuplicate) {
                        allRecommendations.add(newRec);
                    }
                }
                
                // Update the cache with the merged list
                redisService.cacheRecommendations(playlistId, allRecommendations);
            }
            
            // Get the cached recommendations
            List<Map<String, String>> allCachedRecommendations = redisService.getCachedRecommendations(playlistId);
            
            // Return only the requested subset based on offset and limit
            int endIndex = Math.min(offset + limit, allCachedRecommendations.size());
            if (offset >= allCachedRecommendations.size()) {
                // Return an empty list if offset is beyond our available recommendations
                return new ArrayList<>();
            }

            return allCachedRecommendations.subList(offset, endIndex);
        } finally {
            // Record the total time taken for the method execution
            methodTimer.stop(apiMetricsService.getGeminiResponseTimer());
        }
    }


    /**
     * Parses song recommendations from Gemini API response.
     * Modified to only extract song titles, not artists.
     * 
     * @param jsonData The JSON response from Gemini API
     * @return List of song recommendations
     */
    private List<Map<String, String>> parseSongRecommendations(String jsonData) {
        List<Map<String, String>> recommendations = new ArrayList<>();
        
        // Log the raw response data (truncate if very long)
        if (jsonData != null) {
            int maxLogLength = 500; // Truncate long responses for logging
            String logData = jsonData.length() > maxLogLength ? 
                jsonData.substring(0, maxLogLength) + "... (truncated, full length: " + jsonData.length() + ")" : jsonData;
            logger.info("Raw Gemini response: {}", logData);
        } else {
            logger.warn("Received null JSON data from Gemini API");
            return recommendations;
        }
        
        try {
            // First, try parsing the outer JSON structure
            logger.info("Attempting to parse outer JSON structure");
            try (JsonReader jsonReader = Json.createReader(new StringReader(jsonData))) {
                JsonObject jsonObject = jsonReader.readObject();
                logger.info("Successfully parsed outer JSON object");
                
                // Extract the text content that should contain our recommendations
                JsonArray candidatesArray = jsonObject.getJsonArray("candidates");
                if (candidatesArray == null || candidatesArray.isEmpty()) {
                    logger.error("No candidates found in Gemini response");
                    return recommendations;
                }
                
                logger.info("Found {} candidates in Gemini response", candidatesArray.size());
                
                JsonObject firstCandidate = candidatesArray.getJsonObject(0);
                JsonObject content = firstCandidate.getJsonObject("content");
                JsonArray parts = content.getJsonArray("parts");
                
                if (parts == null || parts.isEmpty()) {
                    logger.error("No parts found in Gemini response content");
                    return recommendations;
                }
                
                JsonObject firstPart = parts.getJsonObject(0);
                
                // Get the text which should contain our JSON array of recommendations
                String text = firstPart.getString("text");
                
                // Log the inner text content (truncate if very long)
                if (text != null) {
                    int maxTextLogLength = 1000;
                    String logText = text.length() > maxTextLogLength ? 
                        text.substring(0, maxTextLogLength) + "... (truncated, full length: " + text.length() + ")" : text;
                    logger.info("Inner text content: {}", logText);
                    
                    // Check if it looks like a valid JSON array
                    boolean startsWithBracket = text.trim().startsWith("[");
                    boolean endsWithBracket = text.trim().endsWith("]");
                    logger.info("Inner text format check - starts with '[': {}, ends with ']': {}", 
                        startsWithBracket, endsWithBracket);
                    
                    // Check for markdown code blocks
                    if (text.contains("```")) {
                        logger.info("Text contains markdown code blocks that need to be removed");
                        // Remove markdown code blocks
                        text = text.replaceAll("```json", "")
                                .replaceAll("```", "")
                                .trim();
                    }
                }
                
                // Try to find and extract just the JSON array
                if (text != null) {
                    int startIndex = text.indexOf('[');
                    int endIndex = text.lastIndexOf(']');
                    
                    if (startIndex >= 0 && endIndex > startIndex) {
                        // Extract just the JSON array part
                        text = text.substring(startIndex, endIndex + 1);
                        logger.info("Extracted JSON array from text, length: {}", text.length());
                    }
                }
                
                // Now try to parse the inner JSON array
                try {
                    logger.info("Attempting to parse inner JSON content as array");
                    try (JsonReader nestedReader = Json.createReader(new StringReader(text))) {
                        JsonArray songsArray = nestedReader.readArray();
                        logger.info("Successfully parsed inner JSON array with {} elements", songsArray.size());
                        
                        for (JsonValue value : songsArray) {
                            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                                JsonObject songObject = value.asJsonObject();
                                Map<String, String> songInfo = new LinkedHashMap<>();
                                
                                // Safely extract song title with fallback
                                try {
                                    songInfo.put("song_title", songObject.getString("song_title", "Unknown Title"));
                                } catch (Exception e) {
                                    logger.warn("Error extracting song_title: {}", e.getMessage());
                                    songInfo.put("song_title", "Unknown Title");
                                }
                                
                                // We're not extracting artist anymore, but adding a placeholder
                                // This is to maintain compatibility with existing code until it's updated
                                songInfo.put("artist", ""); 
                                
                                recommendations.add(songInfo);
                                logger.debug("Added recommendation: {}", songInfo.get("song_title"));
                            } else {
                                logger.warn("Unexpected value type in songs array: {}", value.getValueType());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error parsing inner JSON array: {}", e.getMessage());
                    
                    // Fallback: Try to parse with regex if JSON parsing fails
                    logger.info("Attempting fallback parsing with regex");
                    recommendations = parseRecommendationsWithRegex(text);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Gemini response: {}", e.getMessage());
            logger.error("Exception type: {}", e.getClass().getName());
            
            // Detailed logging for JSON parsing errors
            if (e.getMessage() != null && e.getMessage().contains("Unexpected char")) {
                logger.error("JSON parsing error details: {}", e.getMessage());
                
                // Try to locate the problem area
                if (jsonData != null) {
                    try {
                        String[] errorParts = e.getMessage().split("at \\(line no=|, column no=|, offset=|\\)");
                        if (errorParts.length >= 4) {
                            int offset = Integer.parseInt(errorParts[3].trim());
                            int contextSize = 50; // Characters before and after the problem
                            int start = Math.max(0, offset - contextSize);
                            int end = Math.min(jsonData.length(), offset + contextSize);
                            
                            String context = jsonData.substring(start, end).replace("\n", "\\n");
                            logger.error("Context around problematic character (offset {}): \"{}\"", offset, context);
                            
                            // Mark the exact position with a pointer
                            StringBuilder pointer = new StringBuilder();
                            for (int i = 0; i < (offset - start); i++) {
                                pointer.append(" ");
                            }
                            pointer.append("^");
                            logger.error("Problem position: {}", pointer.toString());
                        }
                    } catch (Exception ex) {
                        logger.error("Error while trying to locate problem area: {}", ex.getMessage());
                    }
                }
            }
            
            // Try fallback parsing with regex
            logger.info("Attempting fallback parsing with regex after outer JSON parse failure");
            recommendations = parseRecommendationsWithRegex(jsonData);
        }
        
        logger.info("Finished parsing, extracted {} recommendations", recommendations.size());
        return recommendations;
    }
    
    /**
     * Fallback method to extract recommendations using regex when JSON parsing fails
     * Modified to focus on extracting just song titles
     * 
     * @param text The text to parse
     * @return List of extracted recommendations
     */
    private List<Map<String, String>> parseRecommendationsWithRegex(String text) {
        List<Map<String, String>> recommendations = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            logger.warn("Empty text provided for regex parsing");
            return recommendations;
        }
        
        try {
            logger.info("Parsing recommendations with regex from {} characters of text", text.length());
            
            // Pattern 1: Look for JSON-like objects with song_title
            Pattern pattern = Pattern.compile("\"song_title\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                Map<String, String> songInfo = new LinkedHashMap<>();
                songInfo.put("song_title", matcher.group(1));
                songInfo.put("artist", ""); // Empty placeholder
                recommendations.add(songInfo);
            }
            
            // If we found recommendations with the first pattern, return them
            if (!recommendations.isEmpty()) {
                logger.info("Found {} recommendations using JSON-like pattern", recommendations.size());
                return recommendations;
            }
            
            // Pattern 2: Look for lines with numbered items
            String[] lines = text.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("{") || line.startsWith("[") || 
                    line.startsWith("}") || line.startsWith("]")) {
                    continue;
                }
                
                // Remove bullet points or numbers at the start
                line = line.replaceAll("^\\d+\\.\\s*", "").replaceAll("^-\\s*", "").trim();
                
                // If line has any content after cleaning, add it as a song title
                if (!line.isEmpty()) {
                    Map<String, String> songInfo = new LinkedHashMap<>();
                    songInfo.put("song_title", line);
                    songInfo.put("artist", ""); // Empty placeholder
                    recommendations.add(songInfo);
                }
            }
            
            logger.info("Found {} recommendations using line-by-line parsing", recommendations.size());
            
        } catch (Exception e) {
            logger.error("Error during regex parsing: {}", e.getMessage());
        }
        
        return recommendations;
    }

    /**
     * Clears cached recommendations for a specific playlist
     * 
     * @param playlistId The ID of the playlist to clear recommendations for
     */
    public void clearRecommendationsCache(String playlistId) {
        if (playlistId != null) {
            redisService.clearCachedRecommendations(playlistId);
        }
    }

    public void clearAllRecommendationsCache() {
        redisService.clearAllCaches();
    }
    
    /**
     * Get song recommendations based on a user's top tracks
     * 
     * @param topTracksData The user's top tracks data
     * @param cacheKey A unique key for caching recommendations
     * @param limit The maximum number of recommendations to return
     * @param offset The offset for pagination
     * @param customPrompt Optional custom prompt for recommendations
     * @return ResponseEntity containing the recommendations
     */
    public ResponseEntity<?> getSongRecommendationsForTopTracks(
        Object topTracksData, String cacheKey, int limit, int offset, String customPrompt) {
    try {
        // Extract track titles from the top tracks
        List<String> trackTitles = extractTrackTitlesFromTopTracks(topTracksData);
        
        if (trackTitles.isEmpty()) {
            return ResponseEntity.badRequest().body("No top tracks found for the user");
        }
        
        logger.info("Extracted {} track titles from top tracks", trackTitles.size());
        
        // Check if the custom prompt has changed
        String cachedPrompt = redisService.getCachedCustomPrompt(cacheKey);
        boolean promptChanged = (customPrompt != null && !customPrompt.equals(cachedPrompt)) || 
                            (customPrompt == null && cachedPrompt != null);
        
        // If the prompt has changed, clear the cache for this key
        if (promptChanged) {
            redisService.clearCachedRecommendations(cacheKey);
            
            // Update the cached prompt
            if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                redisService.cacheCustomPrompt(cacheKey, customPrompt);
            } else {
                redisService.clearCachedCustomPrompt(cacheKey);
            }
        }
        
        // Get recommendations based on these tracks
        List<Map<String, String>> recommendations = getGeminiSongRecommendations(
            trackTitles, cacheKey, limit, offset, customPrompt);
        
        return ResponseEntity.ok(recommendations);
    } catch (Exception e) {
        logger.error("Error getting song recommendations for top tracks: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting song recommendations: " + e.getMessage());
    }
    }

    /**
    * Extract track titles from top tracks response
    * 
    * @param topTracksData The top tracks data from Spotify API
    * @return List of track titles with artists
    */
    private List<String> extractTrackTitlesFromTopTracks(Object topTracksData) {
    List<String> trackTitles = new ArrayList<>();

    try {
        // Cast to Map to navigate the JSON structure
        Map<String, Object> topTracksMap = (Map<String, Object>) topTracksData;
        
        // Get the items array which contains the tracks
        List<Map<String, Object>> items = (List<Map<String, Object>>) topTracksMap.get("items");
        
        // Extract track names and artists from each item
        for (Map<String, Object> track : items) {
            if (track != null) {
                String trackName = (String) track.get("name");
                
                // Get artist name
                List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                if (artists != null && !artists.isEmpty()) {
                    String artistName = (String) artists.get(0).get("name");
                    trackTitles.add(trackName + " by " + artistName);
                } else {
                    trackTitles.add(trackName);
                }
            }
        }
    } catch (Exception e) {
        logger.error("Error extracting track titles from top tracks: {}", e.getMessage(), e);
    }

    return trackTitles;
    }
}