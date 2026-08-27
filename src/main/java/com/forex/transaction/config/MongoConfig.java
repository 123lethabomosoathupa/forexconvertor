package com.forex.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

/**
 * MongoDB configuration.
 *
 * Key points:
 *  - Spring Data MongoDB handles BigDecimal → Decimal128 automatically.
 *  - MongoTransactionManager requires MongoDB 4.0+ with a replica set
 *    (or a single-node replica set for local dev).
 *    Comment it out if you're running a plain standalone Mongo locally.
 *  - Indexes declared with @Indexed / @CompoundIndex on the document
 *    are created on startup when auto-index-creation=true in properties.
 */
@Configuration
public class MongoConfig {

    /**
     * Enables @Transactional on service methods.
     * Requires replica set. For local dev without replica set, remove this bean
     * and remove @Transactional from TransactionService methods.
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }

    @Bean
    public MongoCustomConversions customConversions() {
        // BigDecimal ↔ Decimal128 is handled by Spring Data MongoDB natively.
        // Add custom converters here if needed.
        return new MongoCustomConversions(List.of());
    }
}
