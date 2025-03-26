package vttp.final_project.repositories;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import vttp.final_project.models.SpotifyTokens;

@Repository
public class SpotifyTokenRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<SpotifyTokens> tokenRowMapper = (ResultSet rs, int rowNum) -> {
        SpotifyTokens tokens = new SpotifyTokens();
        tokens.setUserId(rs.getString("user_id"));
        tokens.setAccessToken(rs.getString("access_token"));
        tokens.setRefreshToken(rs.getString("refresh_token"));
        Timestamp expiryTs = rs.getTimestamp("access_token_expiry");
        tokens.setAccessTokenExpiry(expiryTs != null ? expiryTs.toInstant() : null);
        tokens.setScope(rs.getString("scope"));
        Timestamp issuedTs = rs.getTimestamp("issued_at");
        tokens.setIssuedAt(issuedTs != null ? issuedTs.toInstant() : null);
        return tokens;
    };
    
    public Optional<SpotifyTokens> findByUserId(String userId) {
        String sql = "SELECT * FROM spotify_tokens WHERE user_id = ?";
        List<SpotifyTokens> results = jdbcTemplate.query(sql, tokenRowMapper, userId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public void save(SpotifyTokens tokens) {
        String sql = "INSERT INTO spotify_tokens (user_id, access_token, refresh_token, access_token_expiry, " +
                     "scope, issued_at) VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE access_token = ?, refresh_token = ?, " +
                     "access_token_expiry = ?, scope = ?, issued_at = ?";
        
        jdbcTemplate.update(sql,
                tokens.getUserId(),
                tokens.getAccessToken(),
                tokens.getRefreshToken(),
                Timestamp.from(tokens.getAccessTokenExpiry()),
                tokens.getScope(),
                Timestamp.from(tokens.getIssuedAt()),
                tokens.getAccessToken(),
                tokens.getRefreshToken(),
                Timestamp.from(tokens.getAccessTokenExpiry()),
                tokens.getScope(),
                Timestamp.from(tokens.getIssuedAt())
        );
    }
    
    public void delete(String userId) {
        String sql = "DELETE FROM spotify_tokens WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
    
    // Schema creation method (for initial setup)
    public void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS spotify_tokens (" +
                     "user_id VARCHAR(255) PRIMARY KEY," +
                     "access_token TEXT NOT NULL," +
                     "refresh_token TEXT NOT NULL," +
                     "access_token_expiry TIMESTAMP NOT NULL," +
                     "scope TEXT," +
                     "issued_at TIMESTAMP NOT NULL" +
                     ")";
        jdbcTemplate.execute(sql);
    }
}
