package com.forex.forexapp.controller;

import com.forex.forexapp.model.AppUser;
import com.forex.forexapp.model.ConversionLog;
import com.forex.forexapp.repository.AppUserRepository;
import com.forex.forexapp.repository.ConversionLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final ConversionLogRepository repository;
    private final AppUserRepository        userRepository;

    public DashboardController(ConversionLogRepository repository,
                                AppUserRepository userRepository) {
        this.repository     = repository;
        this.userRepository = userRepository;
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AppUser user = currentUser();

        // --- Summary stats ---
        long   totalConversions = repository.countByUser(user);
        Double highestRate      = repository.findMaxRateByUser(user);
        Double lowestRate       = repository.findMinRateByUser(user);

        // --- Most used pair ---
        List<Object[]> topPairs = repository.findTopPairsByUser(user);
        String mostUsedPair = topPairs.isEmpty() ? "N/A"
            : topPairs.get(0)[0] + "/" + topPairs.get(0)[1];

        // --- Top pairs bar data (top 5) ---
        // Each entry: [label, count, barWidth%]
        long maxCount = topPairs.isEmpty() ? 1
            : ((Number) topPairs.get(0)[2]).longValue();

        List<Map<String, Object>> topPairsData = topPairs.stream()
            .limit(5)
            .map(row -> {
                long count    = ((Number) row[2]).longValue();
                int  barWidth = (int) (((double) count / maxCount) * 100);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("pair",     row[0] + "/" + row[1]);
                m.put("count",    count);
                m.put("barWidth", barWidth);
                return m;
            })
            .collect(Collectors.toList());

        // --- 30-day volatility per pair (std deviation of rates) ---
        LocalDateTime since30 = LocalDateTime.now().minusDays(30);
        List<ConversionLog> recent = repository.findByUserSince(user, since30);

        // Group by pair, compute std deviation
        Map<String, List<Double>> ratesByPair = new LinkedHashMap<>();
        for (ConversionLog log : recent) {
            String key = log.getFromCurrency() + "/" + log.getToCurrency();
            ratesByPair.computeIfAbsent(key, k -> new ArrayList<>()).add(log.getRate());
        }

        // Compute volatility score (std dev) per pair, sort descending
        List<Map<String, Object>> volatilityData = ratesByPair.entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .map(e -> {
                List<Double> rates = e.getValue();
                double mean  = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double variance = rates.stream()
                    .mapToDouble(r -> Math.pow(r - mean, 2))
                    .average().orElse(0);
                double stdDev = Math.sqrt(variance);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("pair",   e.getKey());
                m.put("stdDev", Math.round(stdDev * 10000.0) / 10000.0);
                return m;
            })
            .sorted((a, b) -> Double.compare(
                (double) b.get("stdDev"), (double) a.get("stdDev")))
            .limit(5)
            .collect(Collectors.toList());

        // Normalise volatility bar widths
        double maxStdDev = volatilityData.isEmpty() ? 1
            : (double) volatilityData.get(0).get("stdDev");
        volatilityData.forEach(v -> {
            int barWidth = maxStdDev == 0 ? 0
                : (int) (((double) v.get("stdDev") / maxStdDev) * 100);
            v.put("barWidth", barWidth);
        });

        // --- Rate trend: last 30 data points across all pairs for the chart ---
        List<ConversionLog> trendLogs = repository
            .findTop30ByUserAndFromCurrencyAndToCurrencyOrderByCreatedAtAsc(
                user,
                topPairs.isEmpty() ? "USD" : (String) topPairs.get(0)[0],
                topPairs.isEmpty() ? "ZAR" : (String) topPairs.get(0)[1]);

        List<String> trendLabels = trendLogs.stream()
            .map(ConversionLog::getFormattedTime).toList();
        List<Double> trendRates  = trendLogs.stream()
            .map(ConversionLog::getRate).toList();

        model.addAttribute("totalConversions", totalConversions);
        model.addAttribute("mostUsedPair",     mostUsedPair);
        model.addAttribute("highestRate",      highestRate  != null ? highestRate  : 0.0);
        model.addAttribute("lowestRate",       lowestRate   != null ? lowestRate   : 0.0);
        model.addAttribute("topPairsData",     topPairsData);
        model.addAttribute("volatilityData",   volatilityData);
        model.addAttribute("trendLabels",      trendLabels);
        model.addAttribute("trendRates",       trendRates);
        model.addAttribute("trendPair",        mostUsedPair);

        return "dashboard";
    }
}