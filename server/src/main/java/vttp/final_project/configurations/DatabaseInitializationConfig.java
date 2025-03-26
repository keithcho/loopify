package vttp.final_project.configurations;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@Configuration
public class DatabaseInitializationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationConfig.class);
    
    @Autowired
    public void initializeDatabase(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            logger.info("Initializing database schema...");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            logger.info("Database schema initialization completed successfully.");
        } catch (Exception e) {
            logger.error("Error initializing database schema", e);
        }
    }
}