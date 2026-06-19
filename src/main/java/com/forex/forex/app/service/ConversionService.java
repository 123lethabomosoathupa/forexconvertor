package com.forex.forexapp.service;

import com.forex.forexapp.model.AppUser;
import com.forex.forexapp.model.ConversionLog;
import com.forex.forexapp.repository.AppUserRepository;
import com.forex.forexapp.repository.ConversionLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class ConversionService {

    private final RestTemplate            restTemplate;
    private final ConversionLogRepository repository;
    private final AppUserRepository       userRepository;
    private final AlertService            alertService;

    @Value("${exchangerate.api.key}")
    private String apiKey;

    @Value("${exchangerate.api.url}")
    private String apiUrl;

    public ConversionService(RestTemplate restTemplate,
                             ConversionLogRepository repository,
                             AppUserRepository userRepository,
                             AlertService alertService) {
        this.restTemplate   = restTemplate;
        this.repository     = repository;
        this.userRepository = userRepository;
        this.alertService   = alertService;
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public ConversionLog convert(String from, String to, double amount) {
        String url = apiUrl + "/" + apiKey + "/pair/" + from + "/" + to;
        Map response = restTemplate.getForObject(url, Map.class);

        if (response == null || !"success".equals(response.get("result"))) {
            throw new RuntimeException("Failed to fetch exchange rate.");
        }

        double rate            = ((Number) response.get("conversion_rate")).doubleValue();
        double convertedAmount = Math.round(amount * rate * 100.0) / 100.0;

        ConversionLog log = new ConversionLog(
            currentUser(), from, to, amount, rate, convertedAmount);
        ConversionLog saved = repository.save(log);

        alertService.checkAndGetTriggeredAlerts(rate, from, to);
        return saved;
    }

    public List<ConversionLog> getRecentHistory() {
        return repository.findTop10ByUserOrderByCreatedAtDesc(currentUser());
    }

    public List<Double> getRatesForSparkline(String from, String to) {
        return repository
            .findTop30ByUserAndFromCurrencyAndToCurrencyOrderByCreatedAtAsc(
                currentUser(), from, to)
            .stream().map(ConversionLog::getRate).toList();
    }

    public List<ConversionLog> getHistoryByPair(String from, String to) {
        return repository.findByUserAndFromCurrencyAndToCurrencyOrderByCreatedAtDesc(
            currentUser(), from, to);
    }
}