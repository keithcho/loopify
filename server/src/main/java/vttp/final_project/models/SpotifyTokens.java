package vttp.final_project.models;

import java.time.Instant;

public class SpotifyTokens {
    private String userId;
    private String accessToken;
    private String refreshToken;
    private Instant accessTokenExpiry;
    private String scope;
    private Instant issuedAt;
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public Instant getAccessTokenExpiry() {
        return accessTokenExpiry;
    }
    
    public void setAccessTokenExpiry(Instant accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public Instant getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(this.accessTokenExpiry);
    }
    
    public boolean isAlmostExpired() {
        // Consider token almost expired if less than 5 minutes remaining
        return Instant.now().plusSeconds(300).isAfter(this.accessTokenExpiry);
    }
}
