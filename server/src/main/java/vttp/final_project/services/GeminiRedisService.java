package vttp.final_project.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiRedisService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiRedisService.class);
    
    // Redis key prefixes
    private static final String RECOMMENDATIONS_KEY_PREFIX = "recommendations:";
    private static final String CUSTOM_PROMPT_KEY_PREFIX = "custom_prompt:";
    
    // Hash keys
    private static final String RECOMMENDATIONS_HASH_KEY = "recommendations_data";
    private static final String CUSTOM_PROMPT_HASH_KEY = "custom_prompt_data";
    
    // Default TTL for cache entries (24 hours)
    @Value("${app.cache.recommendations.ttl:86400}")
    private long recommendationsTtl;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Stores recommendations in Redis cache
     * 
     * @param playlistId The playlist ID or cache key
     * @param recommendations The list of recommendations to cache
     */
    public void cacheRecommendations(String playlistId, List<Map<String, String>> recommendations) {
        try {
            String recommendationsJson = objectMapper.writeValueAsString(recommendations);
            String redisKey = RECOMMENDATIONS_KEY_PREFIX + playlistId;
            
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            hashOps.put(redisKey, RECOMMENDATIONS_HASH_KEY, recommendationsJson);
            
            // Set expiry for the Redis key
            redisTemplate.expire(redisKey, recommendationsTtl, TimeUnit.SECONDS);
            
            logger.info("Cached {} recommendations for playlist {}", 
                    recommendations.size(), playlistId);
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing recommendations for caching", e);
        }
    }
    
    /**
     * Retrieves cached recommendations from Redis
     * 
     * @param playlistId The playlist ID or cache key
     * @return The cached recommendations, or null if not found
     */
    public List<Map<String, String>> getCachedRecommendations(String playlistId) {
        String redisKey = RECOMMENDATIONS_KEY_PREFIX + playlistId;
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        
        String recommendationsJson = hashOps.get(redisKey, RECOMMENDATIONS_HASH_KEY);
        
        if (recommendationsJson == null) {
            logger.info("No cached recommendations found for playlist {}", playlistId);
            return null;
        }
        
        try {
            List<Map<String, String>> recommendations = objectMapper.readValue(
                    recommendationsJson,
                    new TypeReference<List<Map<String, String>>>() {}
            );
            
            logger.info("Retrieved {} cached recommendations for playlist {}", 
                    recommendations.size(), playlistId);
            
            return recommendations;
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached recommendations", e);
            return null;
        }
    }
    
    /**
     * Stores a custom prompt for a playlist in Redis
     * 
     * @param playlistId The playlist ID or cache key
     * @param customPrompt The custom prompt to cache
     */
    public void cacheCustomPrompt(String playlistId, String customPrompt) {
        String redisKey = CUSTOM_PROMPT_KEY_PREFIX + playlistId;
        
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        hashOps.put(redisKey, CUSTOM_PROMPT_HASH_KEY, customPrompt);
        
        // Set expiry for the Redis key
        redisTemplate.expire(redisKey, recommendationsTtl, TimeUnit.SECONDS);
        
        logger.info("Cached custom prompt for playlist {}: '{}'", 
                playlistId, customPrompt);
    }
    
    /**
     * Retrieves a cached custom prompt for a playlist
     * 
     * @param playlistId The playlist ID or cache key
     * @return The cached custom prompt, or null if not found
     */
    public String getCachedCustomPrompt(String playlistId) {
        String redisKey = CUSTOM_PROMPT_KEY_PREFIX + playlistId;
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        
        String customPrompt = hashOps.get(redisKey, CUSTOM_PROMPT_HASH_KEY);
        
        if (customPrompt == null) {
            logger.info("No cached custom prompt found for playlist {}", playlistId);
        } else {
            logger.info("Retrieved cached custom prompt for playlist {}: '{}'", 
                    playlistId, customPrompt);
        }
        
        return customPrompt;
    }
    
    /**
     * Removes cached recommendations for a playlist
     * 
     * @param playlistId The playlist ID or cache key
     */
    public void clearCachedRecommendations(String playlistId) {
        String redisKey = RECOMMENDATIONS_KEY_PREFIX + playlistId;
        redisTemplate.delete(redisKey);
        
        logger.info("Cleared cached recommendations for playlist {}", playlistId);
    }
    
    /**
     * Removes cached custom prompt for a playlist
     * 
     * @param playlistId The playlist ID or cache key
     */
    public void clearCachedCustomPrompt(String playlistId) {
        String redisKey = CUSTOM_PROMPT_KEY_PREFIX + playlistId;
        redisTemplate.delete(redisKey);
        
        logger.info("Cleared cached custom prompt for playlist {}", playlistId);
    }
    
    /**
     * Clears all cached data for a specific playlist
     * 
     * @param playlistId The playlist ID or cache key
     */
    public void clearAllCachedData(String playlistId) {
        clearCachedRecommendations(playlistId);
        clearCachedCustomPrompt(playlistId);
    }
    
    /**
     * Clears all cached recommendations and custom prompts
     */
    public void clearAllCaches() {
        // Find all keys with our prefixes
        redisTemplate.delete(redisTemplate.keys(RECOMMENDATIONS_KEY_PREFIX + "*"));
        redisTemplate.delete(redisTemplate.keys(CUSTOM_PROMPT_KEY_PREFIX + "*"));
        
        logger.info("Cleared all recommendation and custom prompt caches");
    }
}