package com.himanshu.portfolio_risk_analytics.config;

import com.mongodb.ConnectionString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Workaround for a Spring Boot 4 bug where spring.data.mongodb.uri does not
 * bind correctly when its value comes from a placeholder (${MONGO_DB_URI}),
 * causing MongoProperties.uri to stay null and silently falling back to
 * mongodb://localhost/test.
 *
 * This bean explicitly applies the resolved connection string, bypassing
 * the broken property-binding path entirely.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientSettingsBuilderCustomizer(
            @Value("${MONGO_DB_URI}") String mongoUri) {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        return settingsBuilder -> settingsBuilder.applyConnectionString(connectionString);
    }
}
