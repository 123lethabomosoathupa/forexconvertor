package com.forex.transaction.controller;

import com.forex.transaction.document.RateAlert;
import com.forex.transaction.document.User;
import com.forex.transaction.repository.RateAlertRepository;
import com.forex.transaction.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final RateAlertRepository alertRepository;
    private final UserRepository      userRepository;

    @GetMapping
    public String alertsPage(Authentication auth, Model model) {
        String userId = resolveMongoUserId(auth);
        List<RateAlert> alerts = alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
        model.addAttribute("alerts", alerts);
        return "alerts";
    }

    @PostMapping
    public String createAlert(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam double targetRate,
            @RequestParam String direction,
            Authentication auth
    ) {
        String username = resolveUsername(auth);
        String userId   = resolveMongoUserId(auth);

        RateAlert alert = RateAlert.builder()
                .userId(userId)
                .userEmail(username)
                .fromCurrency(fromCurrency.toUpperCase())
                .toCurrency(toCurrency.toUpperCase())
                .targetRate(targetRate)
                .direction(direction)
                .build();

        alertRepository.save(alert);
        return "redirect:/alerts";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveUsername(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return auth.getName();
    }

    /**
     * Look up the actual Mongo _id for the logged-in user so alerts are
     * linked to the same document as the User collection.
     */
    private String resolveMongoUserId(Authentication auth) {
        String username = resolveUsername(auth);
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(username); // fallback: use username as id
    }
}
