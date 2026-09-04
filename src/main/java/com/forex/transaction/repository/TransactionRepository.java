package com.forex.transaction.repository;

import com.forex.transaction.document.Transaction;
import com.forex.transaction.document.Transaction.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(String id, Long userId);

    long countByUserId(Long userId);

    // Top currency pair per user
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0 } }",
        "{ $group: { _id: { from: '$fromCurrency', to: '$toCurrency' }, count: { $sum: 1 } } }",
        "{ $sort: { count: -1 } }",
        "{ $limit: 1 }"
    })
    Optional<PairCount> findTopPairByUserId(Long userId);

    // Last transaction timestamp
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0 } }",
        "{ $sort: { createdAt: -1 } }",
        "{ $limit: 1 }",
        "{ $project: { _id: 0, createdAt: 1 } }"
    })
    Optional<InstantWrapper> findLastTransactionAt(Long userId);

    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Projection interfaces
    interface PairCount {
        PairId get_id();
        long getCount();
        interface PairId { String getFrom(); String getTo(); }
    }

    interface InstantWrapper { Instant getCreatedAt(); }
}
