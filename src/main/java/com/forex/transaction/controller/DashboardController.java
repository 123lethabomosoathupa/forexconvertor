package com.forex.transaction.controller;

import com.forex.transaction.dto.TransactionDtos.*;
import com.forex.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        UserStatsResponse stats = transactionService.getStats(auth);
        PagedResponse<TransactionResponse> recent = transactionService.getHistory(
                new TransactionFilter(null, null, null, null, null, 0, 5), auth);
        model.addAttribute("stats",  stats);
        model.addAttribute("recent", recent.content());
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
        ConvertRequest req = new ConvertRequest(fromCurrency, toCurrency, amount, exchangeRate, notes);
        TransactionResponse result = transactionService.convert(req, auth, request);
        model.addAttribute("result", result);
        return "converter";
    }

    @GetMapping("/history")
    public String history(Authentication auth, Model model,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<TransactionResponse> history = transactionService.getHistory(
                new TransactionFilter(null, null, null, null, null, page, size), auth);
        model.addAttribute("history", history);
        return "history";
    }
}
