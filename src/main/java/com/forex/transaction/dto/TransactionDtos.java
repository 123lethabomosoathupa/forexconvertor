package com.forex.transaction.dto;

import com.forex.transaction.document.Transaction.Status;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {}

    public record ConvertRequest(

        @NotBlank(message = "fromCurrency is required")
        @Size(min = 3, max = 3, message = "Must be a 3-letter ISO code")
        @Pattern(regexp = "[A-Z]{3}", message = "Must be uppercase, e.g. USD")
        String fromCurrency,

        @NotBlank(message = "toCurrency is required")
        @Size(min = 3, max = 3, message = "Must be a 3-letter ISO code")
        @Pattern(regexp = "[A-Z]{3}", message = "Must be uppercase, e.g. ZAR")
        String toCurrency,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.000001", message = "Amount must be positive")
        @Digits(integer = 12, fraction = 6)
        BigDecimal amount,

        @NotNull(message = "exchangeRate is required")
        @DecimalMin(value = "0.000001", message = "Rate must be positive")
        BigDecimal exchangeRate,

        String notes

    ) {
        public void validate() {
            if (fromCurrency != null && fromCurrency.equalsIgnoreCase(toCurrency)) {
                throw new IllegalArgumentException("fromCurrency and toCurrency must differ");
            }
        }
    }

    public record TransactionResponse(
        String id,
        Long userId,
        String userEmail,
        String fromCurrency,
        String toCurrency,
        BigDecimal sourceAmount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        Status status,
        Instant createdAt,
        String ipAddress,
        String notes
    ) {}

    public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
    ) {}

    public record TransactionFilter(
        String from,
        String to,
        Status status,
        Instant after,
        Instant before,
        int page,
        int size
    ) {
        public TransactionFilter {
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 20;
        }
    }

    public record UserStatsResponse(
        Long userId,
        String userEmail,
        long totalTransactions,
        BigDecimal totalSourceVolume,
        String topPair,
        Instant lastTransactionAt
    ) {}

    public record ErrorResponse(
        int status,
        String error,
        String message,
        String path
    ) {}
}
