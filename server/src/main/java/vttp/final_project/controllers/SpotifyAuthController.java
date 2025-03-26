package vttp.final_project.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vttp.final_project.models.SpotifyTokens;
import vttp.final_project.repositories.SpotifyTokenRepository;
import vttp.final_project.services.SpotifyAuthService;

@Controller
@RequestMapping("/api/auth/spotify")
@CrossOrigin(origins = "${app.frontend.url}", allowCredentials = "true")
public class SpotifyAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthController.class);
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private SpotifyAuthService spotifyAuthService;
    
    @Autowired
    private SpotifyTokenRepository tokenRepository;
    
    @GetMapping
    public RedirectView startAuth(HttpServletRequest request) {
        logger.info("Starting Spotify authentication flow");
        
        // Generate code verifier and challenge
        String codeVerifier = spotifyAuthService.generateCodeVerifier();
        String codeChallenge = spotifyAuthService.generateCodeChallenge(codeVerifier);
        
        // Generate state to prevent CSRF
        String state = UUID.randomUUID().toString();
        
        logger.info("Generated state: {}", state);
        
        // Store code verifier and state in session
        HttpSession session = request.getSession(true);
        session.setAttribute("spotify_code_verifier", codeVerifier);
        session.setAttribute("spotify_auth_state", state);
        
        logger.info("Stored verifier and state in session: {}", session.getId());
        
        // Redirect to Spotify authorization URL
        String authUrl = spotifyAuthService.getAuthorizationUrl(codeChallenge, state);
        logger.info("Redirecting to Spotify auth URL");
        return new RedirectView(authUrl);
    }
    
    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        logger.info("Callback received from Spotify. Code present: {}, State: {}, Error: {}", 
                code != null, state, error);
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.error("Session is null during callback");
            return new RedirectView(frontendUrl + "/login?error=session_expired");
        }
        
        logger.info("Session ID: {}", session.getId());
        
        // Verify state to prevent CSRF
        String storedState = (String) session.getAttribute("spotify_auth_state");
        logger.info("Stored state: {}, Received state: {}", storedState, state);
        
        if (storedState == null || !storedState.equals(state)) {
            logger.error("State mismatch. Stored: {}, Received: {}", storedState, state);
            return new RedirectView(frontendUrl + "/login?error=state_mismatch");
        }
        
        // Check for error parameter
        if (error != null) {
            logger.error("Error from Spotify: {}", error);
            return new RedirectView(frontendUrl + "/login?error=" + error);
        }
        
        // Get code verifier from session
        String codeVerifier = (String) session.getAttribute("spotify_code_verifier");
        if (codeVerifier == null) {
            logger.error("Code verifier missing from session");
            return new RedirectView(frontendUrl + "/login?error=missing_verifier");
        }
        
        try {
            logger.info("Attempting to exchange code for tokens");
            // Exchange code for tokens
            SpotifyTokens tokens = spotifyAuthService.exchangeCodeForTokens(code, codeVerifier);
            
            String userId = session.getId();
            logger.info("Got tokens for user ID: {}", userId);
            
            // Save tokens to database
            spotifyAuthService.saveUserTokens(userId, tokens);
            logger.info("Saved tokens to database");
            
            // Clean up session attributes we don't need anymore
            session.removeAttribute("spotify_code_verifier");
            session.removeAttribute("spotify_auth_state");
            
            // Set a session attribute to indicate user is authenticated
            session.setAttribute("authenticated", true);
            session.setAttribute("userId", userId);
            logger.info("Set authentication attributes in session");
            
            // Redirect to frontend
            logger.info("Redirecting to dashboard");
            return new RedirectView(frontendUrl + "/dashboard");
            
        } catch (Exception e) {
            logger.error("Failed to exchange code for tokens", e);
            return new RedirectView(frontendUrl + "/login?error=token_exchange_failed");
        }
    }
    
    @GetMapping("/logout")
    public RedirectView logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false, defaultValue = "/login") String redirect) {
        
        logger.info("Processing logout request");
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Get user ID before invalidating session
            String userId = (String) session.getAttribute("userId");
            logger.info("Logging out user ID: {}", userId);
            
            if (userId != null) {
                // Delete the user's tokens from the database
                logger.info("Deleting tokens for user ID: {}", userId);
                tokenRepository.delete(userId);
            }
            
            // Invalidate session
            logger.info("Invalidating session: {}", session.getId());
            session.invalidate();
        }
        
        // Always clear the session cookie, even if session was null
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    logger.info("Clearing JSESSIONID cookie");
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    cookie.setSecure(true);
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                }
            }
        }
        
        // Add a response header to prevent caching
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        // Ensure the redirect starts with a slash and prepend the frontend URL
        if (!redirect.startsWith("/")) {
            redirect = "/" + redirect;
        }
        
        logger.info("Redirecting to: {}{}", frontendUrl, redirect);
        return new RedirectView(frontendUrl + redirect + "?logout=success");
    }
}