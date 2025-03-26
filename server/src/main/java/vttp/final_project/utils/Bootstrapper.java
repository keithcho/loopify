package vttp.final_project.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import vttp.final_project.services.GeminiService;

@Component
public class Bootstrapper implements CommandLineRunner{

    @Autowired
    private GeminiService geminiService;

    Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Override
    public void run(String... args) {
        // geminiService.getGeminiSongRecommendations();
    }
}
