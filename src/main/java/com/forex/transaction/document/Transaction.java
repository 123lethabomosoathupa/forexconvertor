package com.forex.transaction.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MongoDB document representing one currency conversion event.
 *
 * Collection: "transactions"
 *
 * Key design decisions:
 *  - id is a MongoDB ObjectId (String) — no BIGSERIAL, no sequences
 *  - userId + userEmail are stored directly from the JWT (no JOIN to auth DB)
 *  - Compound indexes mirror the most common query patterns
 *  - BigDecimal is stored as Decimal128 by Spring Data MongoDB automatically
 */
@Document(collection = "transactions")
@CompoundIndexes({
    // Most common: a user's history sorted newest first
    @CompoundIndex(name = "idx_user_date",     def = "{'userId': 1, 'createdAt': -1}"),
    // Reporting: group or filter by currency pair
    @CompoundIndex(name = "idx_currency_pair", def = "{'fromCurrency': 1, 'toCurrency': 1}"),
    // Admin: filter by status + date
    @CompoundIndex(name = "idx_status_date",   def = "{'status': 1, 'createdAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String id;                  // MongoDB ObjectId — e.g. "665f3a2b1c4e7d0012ab34cd"

    // ── User identity (from JWT claims) ───────────────────────────────────────

    @Indexed
    private Long userId;                // numeric id from Auth Service JWT

    private String userEmail;           // stored for audit readability without a JOIN

    // ── Conversion details ─────────────────────────────────────────────────────

    private String fromCurrency;        // ISO 4217 — e.g. "USD"
    private String toCurrency;          // ISO 4217 — e.g. "ZAR"
    private BigDecimal sourceAmount;    // what the user submitted
    private BigDecimal convertedAmount; // sourceAmount * exchangeRate
    private BigDecimal exchangeRate;    // rate at the moment of conversion — immutable after save

    // ── Status ─────────────────────────────────────────────────────────────────

    @Builder.Default
    private Status status = Status.COMPLETED;

    // ── Audit ──────────────────────────────────────────────────────────────────

    @Indexed
    @Builder.Default
    private Instant createdAt = Instant.now();

    private String ipAddress;           // for fraud detection — supports IPv6
    private String notes;               // optional free-text (e.g. "Client invoice #42")

    // ── Enum ───────────────────────────────────────────────────────────────────

    public enum Status { COMPLETED, FAILED, PENDING }
}
