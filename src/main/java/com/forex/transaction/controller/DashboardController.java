package com.forex.transaction.controller;

import com.forex.transaction.dto.TransactionDtos.*;
import com.forex.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        try {
            UserStatsResponse stats = transactionService.getStats(auth);
            model.addAttribute("stats", stats);
        } catch (Exception e) {
            log.warn("Could not load stats: {}", e.getMessage());
            // Provide empty stats so the page still renders
            model.addAttribute("stats", new UserStatsResponse(
                    0L, "unknown", 0L, BigDecimal.ZERO, "N/A", null));
        }

        try {
            PagedResponse<TransactionResponse> recent = transactionService.getHistory(
                    new TransactionFilter(null, null, null, null, null, 0, 5), auth);
            model.addAttribute("recent", recent.content());
        } catch (Exception e) {
            log.warn("Could not load recent transactions: {}", e.getMessage());
            model.addAttribute("recent", List.of());
        }

        return "dashboard";
    }

    @GetMapping("/converter")
    public String converterPage() {
        return "converter";
    }

    @PostMapping("/converter")
    public String convert(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam BigDecimal amount,
            @RequestParam BigDecimal exchangeRate,
            @RequestParam(required = false) String notes,
            Authentication auth,
            HttpServletRequest request,
            Model model
    ) {
        try {
            ConvertRequest req = new ConvertRequest(
                    fromCurrency, toCurrency, amount, exchangeRate, notes);
            TransactionResponse result = transactionService.convert(req, auth, request);
            model.addAttribute("result", result);
        } catch (Exception e) {
            log.error("Conversion failed: {}", e.getMessage(), e);
            model.addAttribute("errorMsg", "Conversion failed: " + e.getMessage());
        }
        return "converter";
    }

    @GetMapping("/history")
    public String history(
            Authentication auth,
            Model model,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            PagedResponse<TransactionResponse> history = transactionService.getHistory(
                    new TransactionFilter(null, null, null, null, null, page, size), auth);
            model.addAttribute("history", history);
        } catch (Exception e) {
            log.warn("Could not load history: {}", e.getMessage());
            model.addAttribute("history", new PagedResponse<TransactionResponse>(
                    List.of(), 0, size, 0L, 0, true, true));
        }
        return "history";
    }
}
