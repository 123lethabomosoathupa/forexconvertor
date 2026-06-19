package com.forex.forexapp.service;

import com.forex.forexapp.model.AppUser;
import com.forex.forexapp.model.RateAlert;
import com.forex.forexapp.repository.AppUserRepository;
import com.forex.forexapp.repository.RateAlertRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AlertService {

    private final RateAlertRepository alertRepository;
    private final AppUserRepository   userRepository;

    public AlertService(RateAlertRepository alertRepository,
                        AppUserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository  = userRepository;
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void createAlert(String from, String to,
                            double targetRate, String direction) {
        RateAlert alert = new RateAlert(
            currentUser(), from.toUpperCase(), to.toUpperCase(),
            targetRate, direction.toUpperCase());
        alertRepository.save(alert);
    }

    // Get a single alert by ID — verifies it belongs to current user
    public RateAlert getAlertById(Long id) {
        RateAlert alert = alertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        if (!alert.getUser().getId().equals(currentUser().getId())) {
            throw new RuntimeException("Not authorised to access this alert");
        }
        return alert;
    }

    // Update an existing alert
    public void updateAlert(Long id, String from, String to,
                            double targetRate, String direction) {
        RateAlert alert = getAlertById(id);
        alert.setFromCurrency(from.toUpperCase());
        alert.setToCurrency(to.toUpperCase());
        alert.setTargetRate(targetRate);
        alert.setDirection(direction.toUpperCase());
        alert.setTriggered(false); // reset trigger when edited
        alertRepository.save(alert);
    }

    public List<RateAlert> checkAndGetTriggeredAlerts(double currentRate,
                                                       String from, String to) {
        List<RateAlert> pending = alertRepository
            .findByFromCurrencyAndToCurrencyAndTriggeredFalse(
                from.toUpperCase(), to.toUpperCase());

        List<RateAlert> triggered = pending.stream()
            .filter(a -> {
                if ("ABOVE".equals(a.getDirection())) return currentRate >= a.getTargetRate();
                if ("BELOW".equals(a.getDirection())) return currentRate <= a.getTargetRate();
                return false;
            })
            .toList();

        triggered.forEach(a -> a.setTriggered(true));
        alertRepository.saveAll(triggered);
        return triggered;
    }

    public List<RateAlert> getActiveAlerts() {
        return alertRepository.findByUserAndTriggeredFalse(currentUser());
    }

    public List<RateAlert> getAllAlerts() {
        return alertRepository.findByUser(currentUser());
    }

    public void deleteAlert(Long id) {
        // Verify ownership before deleting
        getAlertById(id);
        alertRepository.deleteById(id);
    }
}