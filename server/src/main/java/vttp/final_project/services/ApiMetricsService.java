package vttp.final_project.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Service
public class ApiMetricsService implements HttpSessionListener {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> apiCounters = new ConcurrentHashMap<>();
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final Timer geminiResponseTimer;
    
    public ApiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register the gauge for active sessions
        Gauge.builder("app.sessions.active", activeSessions, AtomicInteger::get)
            .description("Number of active user sessions")
            .register(meterRegistry);
            
        // Create timer for Gemini API response time
        this.geminiResponseTimer = Timer.builder("app.gemini.response.time")
            .description("Time taken for Gemini API to respond with recommendations")
            .register(meterRegistry);
    }
    
    /**
     * Increment counter for API endpoint access
     * @param endpoint The API endpoint being accessed
     */
    public void incrementApiCounter(String endpoint) {
        apiCounters.computeIfAbsent(endpoint, key -> 
            Counter.builder("app.api.requests")
                .description("Number of API requests")
                .tag("endpoint", key)
                .register(meterRegistry)
        ).increment();
    }
    
    /**
     * Record the time taken for a Gemini API call
     * @param timeMs Time in milliseconds
     */
    public void recordGeminiResponseTime(long timeMs) {
        geminiResponseTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Timer for measuring Gemini API call duration
     * @return Timer object that can be used to record time
     */
    public Timer getGeminiResponseTimer() {
        return geminiResponseTimer;
    }
    
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        activeSessions.incrementAndGet();
    }
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        activeSessions.decrementAndGet();
    }
    
    /**
     * Get the current number of active sessions
     * @return The count of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.get();
    }
}