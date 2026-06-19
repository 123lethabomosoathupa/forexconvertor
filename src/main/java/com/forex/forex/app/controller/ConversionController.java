package com.forex.forexapp.controller;

import com.forex.forexapp.model.ConversionLog;
import com.forex.forexapp.model.RateAlert;
import com.forex.forexapp.service.AlertService;
import com.forex.forexapp.service.ConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class ConversionController {

    private final ConversionService conversionService;
    private final AlertService      alertService;

    private static final List<String> CURRENCIES = List.of(
        "USD","EUR","GBP","ZAR","JPY","AUD","CAD",
        "CHF","CNY","INR","NGN","KES","GHS","EGP"
    );

    public ConversionController(ConversionService conversionService,
                                AlertService alertService) {
        this.conversionService = conversionService;
        this.alertService      = alertService;
    }

    @GetMapping("/")
    public String index() { return "redirect:/converter"; }

    @GetMapping("/converter")
    public String converterPage(Model model) {
        model.addAttribute("currencies", CURRENCIES);
        return "converter";
    }

    @PostMapping("/converter")
    public String doConvert(@RequestParam String from,
                            @RequestParam String to,
                            @RequestParam double amount,
                            Model model) {
        model.addAttribute("currencies",   CURRENCIES);
        model.addAttribute("selectedFrom", from.toUpperCase());
        model.addAttribute("selectedTo",   to.toUpperCase());
        model.addAttribute("amount",       amount);

        try {
            ConversionLog result = conversionService.convert(
                from.toUpperCase(), to.toUpperCase(), amount);
            model.addAttribute("result",       result);
            model.addAttribute("showSparkline", true);

            List<RateAlert> triggered = alertService.checkAndGetTriggeredAlerts(
                result.getRate(), result.getFromCurrency(), result.getToCurrency());
            if (!triggered.isEmpty()) {
                model.addAttribute("triggeredAlerts", triggered);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Conversion failed: " + e.getMessage());
        }
        return "converter";
    }

    @GetMapping("/history")
    public String historyPage(Model model) {
        model.addAttribute("logs",       conversionService.getRecentHistory());
        model.addAttribute("currencies", CURRENCIES);
        return "history";
    }

    @GetMapping("/history/pair")
    public String historyByPair(@RequestParam String from,
                                @RequestParam String to,
                                Model model) {
        model.addAttribute("logs",         conversionService.getHistoryByPair(
                                               from.toUpperCase(), to.toUpperCase()));
        model.addAttribute("currencies",   CURRENCIES);
        model.addAttribute("selectedFrom", from.toUpperCase());
        model.addAttribute("selectedTo",   to.toUpperCase());
        model.addAttribute("showSparkline", true);
        return "history";
    }
}