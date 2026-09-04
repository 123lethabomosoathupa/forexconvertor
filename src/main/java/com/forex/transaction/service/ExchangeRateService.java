package com.forex.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches live exchange rates from open.er-api.com (free, no key required).
 * Caches results for 1 hour to avoid hammering the API on every page load.
 *
 * Falls back to empty map if the API is unreachable — the converter still works
 * but the user must enter the rate manually.
 */
@Slf4j
@Service
public class ExchangeRateService {

    private static final String API_URL =
            "https://open.er-api.com/v6/latest/USD";

    private static final long CACHE_TTL_SECONDS = 3600; // 1 hour

    private final RestTemplate restTemplate = new RestTemplate();

    // Simple in-memory cache
    private Map<String, BigDecimal> cachedRates = new HashMap<>();
    private Instant cacheExpiry = Instant.EPOCH;

    /**
     * Returns USD-based rates for all currencies.
     * Map key = currency code (e.g. "ZAR"), value = units per 1 USD.
     */
    public Map<String, BigDecimal> getRates() {
        if (Instant.now().isBefore(cacheExpiry) && !cachedRates.isEmpty()) {
            return Collections.unmodifiableMap(cachedRates);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);

            if (response != null && "success".equals(response.get("result"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rawRates = (Map<String, Object>) response.get("rates");

                Map<String, BigDecimal> parsed = new HashMap<>();
                rawRates.forEach((k, v) ->
                        parsed.put(k, new BigDecimal(v.toString()))
                );

                cachedRates = parsed;
                cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
                log.info("Exchange rates refreshed — {} currencies loaded", parsed.size());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rates: {}", e.getMessage());
        }

        return Collections.unmodifiableMap(cachedRates);
    }

    /**
     * Returns the rate to convert from → to via USD as the base.
     * Returns null if either currency is unknown.
     */
    public BigDecimal getRate(String from, String to) {
        Map<String, BigDecimal> rates = getRates();
        if (rates.isEmpty()) return null;

        BigDecimal fromRate = rates.get(from);
        BigDecimal toRate   = rates.get(to);

        if (fromRate == null || toRate == null) return null;

        // rate = toRate / fromRate  (both relative to USD)
        return toRate.divide(fromRate, 8, java.math.RoundingMode.HALF_UP);
    }
}
