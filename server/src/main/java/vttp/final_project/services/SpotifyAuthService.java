package vttp.final_project.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import vttp.final_project.models.SpotifyTokens;
import vttp.final_project.repositories.SpotifyTokenRepository;

@Service
public class SpotifyAuthService {
    
    @Value("${spotify.client.id}")
    private String clientId;
    
    @Value("${spotify.client.secret}")
    private String clientSecret;
    
    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    @Autowired
    private SpotifyTokenRepository tokenRepository;

    @Autowired
    private TokenEncryptionService encryptionService;

    RestTemplate restTemplate = new RestTemplate();
    
    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);

    public String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[64];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }
    
    public String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes);
            byte[] digest = messageDigest.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate code challenge", e);
        }
    }
    
    public String getAuthorizationUrl(String codeChallenge, String state) {
        return "https://accounts.spotify.com/authorize" +
               "?client_id=" + clientId +
               "&response_type=code" +
               "&redirect_uri=" + redirectUri +
               "&code_challenge_method=S256" +
               "&code_challenge=" + codeChallenge +
               "&state=" + state +
               "&scope=user-read-private%20user-read-email%20playlist-read-private%20playlist-read-collaborative%20playlist-modify-public%20playlist-modify-private%20user-top-read";
    }
    
    public SpotifyTokens exchangeCodeForTokens(String code, String codeVerifier) {
        logger.info("Beginning token exchange with code: {}", code.substring(0, 5) + "...");
        logger.info("Using redirect URI: {}", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        logger.info("Prepared token request with client ID: {}", clientId.substring(0, 5) + "...");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            logger.info("Sending token request to Spotify API...");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token",
                    request,
                    Map.class
            );
            
            logger.info("Received response from Spotify API: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenResponse = response.getBody();
                logger.info("Successfully received tokens from Spotify");
                
                SpotifyTokens tokens = new SpotifyTokens();
                tokens.setAccessToken(encryptionService.encrypt((String) tokenResponse.get("access_token")));
                tokens.setRefreshToken(encryptionService.encrypt((String) tokenResponse.get("refresh_token")));
                
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");
                tokens.setAccessTokenExpiry(Instant.now().plusSeconds(expiresIn));
                
                tokens.setScope((String) tokenResponse.get("scope"));
                tokens.setIssuedAt(Instant.now());
                
                return tokens;
            } else {
                logger.error("Failed to exchange token. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to exchange code for tokens. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Exception during token exchange", e);
            throw new RuntimeException("Failed to exchange code for tokens", e);
        }
    }
    
    public SpotifyTokens refreshAccessToken(String userId, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", encryptionService.decrypt(refreshToken));
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token",
                request,
                Map.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> tokenResponse = response.getBody();
            
            SpotifyTokens tokens = new SpotifyTokens();
            tokens.setUserId(userId);
            tokens.setAccessToken(encryptionService.encrypt((String) tokenResponse.get("access_token")));
            
            // The response might not include a new refresh token, so use the existing one if not present
            if (tokenResponse.containsKey("refresh_token")) {
                tokens.setRefreshToken(encryptionService.encrypt((String) tokenResponse.get("refresh_token")));
            } else {
                tokens.setRefreshToken(refreshToken);
            }
            
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");
            tokens.setAccessTokenExpiry(Instant.now().plusSeconds(expiresIn));
            
            tokens.setScope((String) tokenResponse.get("scope"));
            tokens.setIssuedAt(Instant.now());
            
            return tokens;
        } else {
            throw new RuntimeException("Failed to refresh access token");
        }
    }
    
    public Optional<String> getValidAccessToken(String userId) {
        Optional<SpotifyTokens> tokensOpt = tokenRepository.findByUserId(userId);
        
        if (tokensOpt.isEmpty()) {
            return Optional.empty();
        }
        
        SpotifyTokens tokens = tokensOpt.get();
        
        // If token is expired or about to expire, refresh it
        if (tokens.isAlmostExpired()) {
            try {
                SpotifyTokens refreshedTokens = refreshAccessToken(userId, tokens.getRefreshToken());
                refreshedTokens.setUserId(userId);
                tokenRepository.save(refreshedTokens);
                return Optional.of(encryptionService.decrypt(refreshedTokens.getAccessToken()));
            } catch (Exception e) {
                // If refresh fails, token might be invalid - remove it
                tokenRepository.delete(userId);
                return Optional.empty();
            }
        }
        
        // Return the existing valid token
        return Optional.of(encryptionService.decrypt(tokens.getAccessToken()));
    }
    
    public void saveUserTokens(String userId, SpotifyTokens tokens) {
        tokens.setUserId(userId);
        tokenRepository.save(tokens);
    }
}
