package com.himanshu.portfolio_risk_analytics.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * Fully replaces Spring Boot 4's Mongo auto-configuration.
 *
 * Boot 4 has a binding bug where spring.data.mongodb.uri does not bind
 * correctly when sourced from a placeholder (${MONGO_DB_URI}), leaving
 * MongoProperties.uri null. Boot then builds its OWN default
 * MongoClientSettings (hosts=[localhost:27017]) regardless of any
 * MongoClientSettingsBuilderCustomizer bean added on top -- which is why
 * a customizer-only fix throws "Can not set both hosts and srvHost"
 * (Boot's default sets hosts, our customizer then tries to set srvHost
 * from the real SRV URI on the same builder).
 *
 * The robust fix is to define our own MongoClient, MongoDatabaseFactory,
 * and MongoTemplate beans directly. Boot's auto-configured equivalents
 * are all @ConditionalOnMissingBean, so ours take over entirely and
 * Boot's broken default path never runs.
 */
@Configuration
public class MongoConfig {

    @Value("${MONGO_DB_URI}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        String database = connectionString.getDatabase();
        if (database == null || database.isBlank()) {
            database = "trader_db";
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
