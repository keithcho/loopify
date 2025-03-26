package vttp.final_project.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FallbackService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackService.class);
    
    /**
     * Provides fallback recommendations when the Gemini API fails
     * Modified to only include song titles
     * 
     * @param playlistTracks List of tracks in the playlist
     * @return List of song recommendations
     */
    public List<Map<String, String>> getFallbackRecommendations(List<String> playlistTracks) {
        List<Map<String, String>> fallbacks = new ArrayList<>();
        
        logger.info("Generating fallback recommendations based on {} playlist tracks", 
                playlistTracks != null ? playlistTracks.size() : 0);
        
        // Check if we have any tracks to analyze
        if (playlistTracks == null || playlistTracks.isEmpty()) {
            // If no tracks, provide diverse popular songs from different genres
            addFallbackRecommendation(fallbacks, "Bohemian Rhapsody", "");
            addFallbackRecommendation(fallbacks, "Hotel California", "");
            addFallbackRecommendation(fallbacks, "Billie Jean", "");
            addFallbackRecommendation(fallbacks, "Imagine", "");
            addFallbackRecommendation(fallbacks, "Smells Like Teen Spirit", "");
            addFallbackRecommendation(fallbacks, "Sweet Child O' Mine", "");
            addFallbackRecommendation(fallbacks, "Like a Rolling Stone", "");
            addFallbackRecommendation(fallbacks, "Respect", "");
            addFallbackRecommendation(fallbacks, "Yesterday", "");
            addFallbackRecommendation(fallbacks, "Purple Haze", "");
            addFallbackRecommendation(fallbacks, "Dancing Queen", "");
            addFallbackRecommendation(fallbacks, "Superstition", "");
            addFallbackRecommendation(fallbacks, "Stairway to Heaven", "");
            addFallbackRecommendation(fallbacks, "I Want to Hold Your Hand", "");
            addFallbackRecommendation(fallbacks, "Johnny B. Goode", "");
        } else {
            // Analyze the playlist to determine a general style/genre
            List<String> rockArtists = List.of("queen", "beatles", "rolling stones", "led zeppelin", 
                    "ac/dc", "guns n' roses", "nirvana", "radiohead", "pink floyd", "u2");
                    
            List<String> popArtists = List.of("michael jackson", "madonna", "prince", "beyoncé", 
                    "taylor swift", "adele", "justin timberlake", "katy perry", "bruno mars", "ariana grande");
                    
            List<String> electronicArtists = List.of("daft punk", "calvin harris", "avicii", 
                    "skrillex", "deadmau5", "david guetta", "the chemical brothers", "tiësto", "diplo", "marshmello");
                    
            List<String> hipHopArtists = List.of("jay-z", "kanye west", "tupac", "drake", "kendrick lamar", 
                    "eminem", "nas", "snoop dogg", "missy elliott", "run-dmc");
                    
            // Count genres in the playlist
            int rockCount = 0, popCount = 0, electronicCount = 0, hipHopCount = 0;
            
            for (String track : playlistTracks) {
                String trackLower = track.toLowerCase();
                
                // Check if any known artists are in the track string
                for (String artist : rockArtists) {
                    if (trackLower.contains(artist)) {
                        rockCount++;
                        break;
                    }
                }
                
                for (String artist : popArtists) {
                    if (trackLower.contains(artist)) {
                        popCount++;
                        break;
                    }
                }
                
                for (String artist : electronicArtists) {
                    if (trackLower.contains(artist)) {
                        electronicCount++;
                        break;
                    }
                }
                
                for (String artist : hipHopArtists) {
                    if (trackLower.contains(artist)) {
                        hipHopCount++;
                        break;
                    }
                }
            }
            
            // Determine dominant genre
            int maxCount = Math.max(Math.max(rockCount, popCount), Math.max(electronicCount, hipHopCount));
            
            if (maxCount == 0) {
                // No clear genre detected, provide diverse recommendations
                addFallbackRecommendation(fallbacks, "Bohemian Rhapsody", "");
                addFallbackRecommendation(fallbacks, "Billie Jean", "");
                addFallbackRecommendation(fallbacks, "One More Time", "");
                addFallbackRecommendation(fallbacks, "99 Problems", "");
                addFallbackRecommendation(fallbacks, "Hotel California", "");
                addFallbackRecommendation(fallbacks, "Shape of You", "");
                addFallbackRecommendation(fallbacks, "Levels", "");
                addFallbackRecommendation(fallbacks, "Alright", "");
                addFallbackRecommendation(fallbacks, "Hey Jude", "");
                addFallbackRecommendation(fallbacks, "Bad Guy", "");
                addFallbackRecommendation(fallbacks, "Dreams", "");
                addFallbackRecommendation(fallbacks, "Uptown Funk", "");
                addFallbackRecommendation(fallbacks, "Hurt", "");
                addFallbackRecommendation(fallbacks, "Hymn for the Weekend", "");
                addFallbackRecommendation(fallbacks, "Blinding Lights", "");
            } else if (maxCount == rockCount) {
                // Rock recommendations
                addFallbackRecommendation(fallbacks, "Bohemian Rhapsody", "");
                addFallbackRecommendation(fallbacks, "Stairway to Heaven", "");
                addFallbackRecommendation(fallbacks, "Sweet Child O' Mine", "");
                addFallbackRecommendation(fallbacks, "Smells Like Teen Spirit", "");
                addFallbackRecommendation(fallbacks, "Back in Black", "");
                addFallbackRecommendation(fallbacks, "Comfortably Numb", "");
                addFallbackRecommendation(fallbacks, "I Can't Get No Satisfaction", "");
                addFallbackRecommendation(fallbacks, "With or Without You", "");
                addFallbackRecommendation(fallbacks, "Creep", "");
                addFallbackRecommendation(fallbacks, "Come As You Are", "");
                addFallbackRecommendation(fallbacks, "Baba O'Riley", "");
                addFallbackRecommendation(fallbacks, "Enter Sandman", "");
                addFallbackRecommendation(fallbacks, "Black", "");
                addFallbackRecommendation(fallbacks, "November Rain", "");
                addFallbackRecommendation(fallbacks, "Paint It Black", "");
            } else if (maxCount == popCount) {
                // Pop recommendations
                addFallbackRecommendation(fallbacks, "Billie Jean", "");
                addFallbackRecommendation(fallbacks, "Like a Prayer", "");
                addFallbackRecommendation(fallbacks, "Purple Rain", "");
                addFallbackRecommendation(fallbacks, "Single Ladies", "");
                addFallbackRecommendation(fallbacks, "Blank Space", "");
                addFallbackRecommendation(fallbacks, "Rolling in the Deep", "");
                addFallbackRecommendation(fallbacks, "SexyBack", "");
                addFallbackRecommendation(fallbacks, "Roar", "");
                addFallbackRecommendation(fallbacks, "Just the Way You Are", "");
                addFallbackRecommendation(fallbacks, "Thank U, Next", "");
                addFallbackRecommendation(fallbacks, "Shape of You", "");
                addFallbackRecommendation(fallbacks, "Toxic", "");
                addFallbackRecommendation(fallbacks, "Uptown Funk", "");
                addFallbackRecommendation(fallbacks, "Bad Guy", "");
                addFallbackRecommendation(fallbacks, "Blinding Lights", "");
            } else if (maxCount == electronicCount) {
                // Electronic recommendations
                addFallbackRecommendation(fallbacks, "One More Time", "");
                addFallbackRecommendation(fallbacks, "Summer", "");
                addFallbackRecommendation(fallbacks, "Wake Me Up", "");
                addFallbackRecommendation(fallbacks, "Bangarang", "");
                addFallbackRecommendation(fallbacks, "Strobe", "");
                addFallbackRecommendation(fallbacks, "Titanium", "");
                addFallbackRecommendation(fallbacks, "Block Rockin' Beats", "");
                addFallbackRecommendation(fallbacks, "Adagio for Strings", "");
                addFallbackRecommendation(fallbacks, "Lean On", "");
                addFallbackRecommendation(fallbacks, "Alone", "");
                addFallbackRecommendation(fallbacks, "Levels", "");
                addFallbackRecommendation(fallbacks, "Don't You Worry Child", "");
                addFallbackRecommendation(fallbacks, "Animals", "");
                addFallbackRecommendation(fallbacks, "Scary Monsters and Nice Sprites", "");
                addFallbackRecommendation(fallbacks, "Clarity", "");
            } else if (maxCount == hipHopCount) {
                // Hip-hop recommendations
                addFallbackRecommendation(fallbacks, "Empire State of Mind", "");
                addFallbackRecommendation(fallbacks, "Stronger", "");
                addFallbackRecommendation(fallbacks, "California Love", "");
                addFallbackRecommendation(fallbacks, "God's Plan", "");
                addFallbackRecommendation(fallbacks, "Alright", "");
                addFallbackRecommendation(fallbacks, "Lose Yourself", "");
                addFallbackRecommendation(fallbacks, "N.Y. State of Mind", "");
                addFallbackRecommendation(fallbacks, "Drop It Like It's Hot", "");
                addFallbackRecommendation(fallbacks, "Get Ur Freak On", "");
                addFallbackRecommendation(fallbacks, "It's Tricky", "");
                addFallbackRecommendation(fallbacks, "Hotline Bling", "");
                addFallbackRecommendation(fallbacks, "Humble", "");
                addFallbackRecommendation(fallbacks, "Juicy", "");
                addFallbackRecommendation(fallbacks, "This Is America", "");
                addFallbackRecommendation(fallbacks, "Jesus Walks", "");
            }
        }
        
        logger.info("Generated {} fallback recommendations", fallbacks.size());
        return fallbacks;
    }

    /**
     * Helper method to add a fallback recommendation
     */
    private void addFallbackRecommendation(List<Map<String, String>> list, String title, String artist) {
        Map<String, String> rec = new LinkedHashMap<>();
        rec.put("song_title", title);
        rec.put("artist", artist);
        list.add(rec);
    }
}