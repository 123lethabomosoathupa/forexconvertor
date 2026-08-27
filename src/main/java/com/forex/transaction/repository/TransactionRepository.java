package com.forex.transaction.repository;

import com.forex.transaction.document.Transaction;
import com.forex.transaction.document.Transaction.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoRepository<Transaction, String> — String because MongoDB ids are ObjectId strings.
 *
 * @Query uses MongoDB query JSON, not JPQL.
 * ?0, ?1 ... are positional parameter placeholders.
 */
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    // ── User-scoped (most common — always filter by userId first) ──────────────

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(String id, Long userId);

    // ── Filtered history — all params optional (null = ignore) ────────────────
    // MongoDB @Query: fields missing from the filter simply aren't added.
    // We use a custom service method + MongoTemplate for this (see TransactionService).

    // Simple lookups used by the service layer
    List<Transaction> findByUserIdAndStatus(Long userId, Status status);

    @Query("{ 'userId': ?0, 'fromCurrency': ?1, 'toCurrency': ?2 }")
    List<Transaction> findByUserIdAndCurrencyPair(Long userId, String from, String to);

    // ── Stats / aggregation ────────────────────────────────────────────────────

    long countByUserId(Long userId);

    // Sum of source amounts for completed transactions
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, status: 'COMPLETED' } }",
        "{ $group: { _id: null, total: { $sum: '$sourceAmount' } } }"
    })
    Optional<BigDecimalWrapper> sumSourceAmountByUserId(Long userId);

    // Most-used currency pair for a user
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

    // ── Admin ──────────────────────────────────────────────────────────────────

    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ── Projection interfaces (used by aggregation results) ────────────────────

    interface BigDecimalWrapper { BigDecimal getTotal(); }
    interface PairCount         { PairId get_id(); long getCount();
        interface PairId        { String getFrom(); String getTo(); }
    }
    interface InstantWrapper    { Instant getCreatedAt(); }
}
