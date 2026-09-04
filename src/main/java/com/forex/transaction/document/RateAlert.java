package com.forex.transaction.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A user-defined alert: notify when a currency pair hits a target rate.
 * direction: "ABOVE" — trigger when rate exceeds target
 *            "BELOW" — trigger when rate falls below target
 */
@Document(collection = "rate_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateAlert {

    @Id
    private String id;

    @Indexed
    private String userId;      // Mongo ObjectId of the owning User

    private String userEmail;

    private String fromCurrency;
    private String toCurrency;

    private double targetRate;

    private String direction;   // "ABOVE" or "BELOW"

    @Builder.Default
    private boolean triggered = false;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant triggeredAt;
}
