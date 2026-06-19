package com.forex.forexapp.controller;

import com.forex.forexapp.model.ConversionLog;
import com.forex.forexapp.repository.ConversionLogRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class ChartController {

    private final ConversionLogRepository repository;

    private static final List<String> CURRENCIES = List.of(
        "USD","EUR","GBP","ZAR","JPY","AUD","CAD",
        "CHF","CNY","INR","NGN","KES","GHS","EGP"
    );

    public ChartController(ConversionLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/charts")
    public String chartsPage(Model model) {
        model.addAttribute("currencies", CURRENCIES);
        return "charts";
    }

    @GetMapping("/charts/data")
    @ResponseBody
    public Map<String, Object> chartData(@RequestParam String from,
                                          @RequestParam String to) {
        List<ConversionLog> logs = repository
            .findTop50ByFromCurrencyAndToCurrencyOrderByCreatedAtAsc(
                from.toUpperCase(), to.toUpperCase());

        return Map.of(
            "pair",   from.toUpperCase() + "/" + to.toUpperCase(),
            "labels", logs.stream().map(ConversionLog::getFormattedTime).toList(),
            "rates",  logs.stream().map(ConversionLog::getRate).toList()
        );
    }
}