package vttp.final_project.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Bootstrapper implements CommandLineRunner{

    Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Override
    public void run(String... args) {
    }
}
