package com.forex.transaction.controller;

import com.forex.transaction.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Live rates from exchangerate-api.com")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * GET /api/v1/rates
     * Returns all USD-based rates. Used by the converter page JS to pre-fill the rate field.
     */
    @GetMapping
    @Operation(summary = "Get all live exchange rates (USD base)")
    public ResponseEntity<Map<String, BigDecimal>> getAllRates() {
        return ResponseEntity.ok(exchangeRateService.getRates());
    }

    /**
     * GET /api/v1/rates/convert?from=USD&to=ZAR
     * Returns the direct cross-rate between two currencies.
     */
    @GetMapping("/convert")
    @Operation(summary = "Get rate between two currencies")
    public ResponseEntity<?> getRate(
            @RequestParam String from,
            @RequestParam String to
    ) {
        BigDecimal rate = exchangeRateService.getRate(from.toUpperCase(), to.toUpperCase());
        if (rate == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unknown currency: " + from + " or " + to));
        }
        return ResponseEntity.ok(Map.of(
                "from", from.toUpperCase(),
                "to",   to.toUpperCase(),
                "rate", rate
        ));
    }
}
