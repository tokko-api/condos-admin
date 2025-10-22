package com.condos.tenant.config;

import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import jakarta.annotation.PostConstruct;

@Configuration
public class MongoIndexes {

    private final MongoTemplate mongo;

    public MongoIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @PostConstruct
    void ensureIndexes() {
        // Índice único en slug SOLO para registros con active=true
        var options = new IndexOptions()
                .name("uq_slug_active_only")
                .unique(true)
                .partialFilterExpression(new Document("active", true));

        mongo.getCollection("tenants")
                .createIndex(new Document("slug", 1), options);
    }
}