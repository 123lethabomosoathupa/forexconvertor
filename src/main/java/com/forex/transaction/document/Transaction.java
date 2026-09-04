package com.forex.transaction.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "transactions")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_date",     def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_currency_pair", def = "{'fromCurrency': 1, 'toCurrency': 1}"),
    @CompoundIndex(name = "idx_status_date",   def = "{'status': 1, 'createdAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String id;

    // User identity — stored from session (web) or JWT (API)
    @Indexed
    private Long userId;
    private String userEmail;

    private String fromCurrency;
    private String toCurrency;
    private BigDecimal sourceAmount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;

    @Builder.Default
    private Status status = Status.COMPLETED;

    @Indexed
    @Builder.Default
    private Instant createdAt = Instant.now();

    private String ipAddress;
    private String notes;

    public enum Status { COMPLETED, FAILED, PENDING }
}
