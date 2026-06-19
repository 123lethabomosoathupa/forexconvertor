package com.forex.forexapp.controller;

import com.forex.forexapp.model.RateAlert;
import com.forex.forexapp.service.AlertService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    private static final List<String> CURRENCIES = List.of(
        "USD","EUR","GBP","ZAR","JPY","AUD","CAD",
        "CHF","CNY","INR","NGN","KES","GHS","EGP"
    );

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // GET /alerts — show all alerts + create form
    @GetMapping
    public String alertsPage(Model model) {
        model.addAttribute("alerts",     alertService.getAllAlerts());
        model.addAttribute("currencies", CURRENCIES);
        return "alerts";
    }

    // POST /alerts — create new alert
    @PostMapping
    public String createAlert(@RequestParam String from,
                              @RequestParam String to,
                              @RequestParam double targetRate,
                              @RequestParam String direction,
                              Model model) {
        try {
            alertService.createAlert(from, to, targetRate, direction);
        } catch (Exception e) {
            model.addAttribute("error",      e.getMessage());
            model.addAttribute("alerts",     alertService.getAllAlerts());
            model.addAttribute("currencies", CURRENCIES);
            return "alerts";
        }
        return "redirect:/alerts";
    }

    // GET /alerts/{id}/edit — show edit form pre-filled with alert data
    @GetMapping("/{id}/edit")
    public String editAlertPage(@PathVariable Long id, Model model) {
        try {
            RateAlert alert = alertService.getAlertById(id);
            model.addAttribute("editAlert",  alert);
            model.addAttribute("alerts",     alertService.getAllAlerts());
            model.addAttribute("currencies", CURRENCIES);
        } catch (Exception e) {
            model.addAttribute("error",      e.getMessage());
            model.addAttribute("alerts",     alertService.getAllAlerts());
            model.addAttribute("currencies", CURRENCIES);
        }
        return "alerts";
    }

    // POST /alerts/{id}/edit — save edited alert
    @PostMapping("/{id}/edit")
    public String updateAlert(@PathVariable Long id,
                              @RequestParam String from,
                              @RequestParam String to,
                              @RequestParam double targetRate,
                              @RequestParam String direction,
                              Model model) {
        try {
            alertService.updateAlert(id, from, to, targetRate, direction);
        } catch (Exception e) {
            model.addAttribute("error",      e.getMessage());
            model.addAttribute("alerts",     alertService.getAllAlerts());
            model.addAttribute("currencies", CURRENCIES);
            return "alerts";
        }
        return "redirect:/alerts";
    }

    // POST /alerts/{id}/delete — delete alert
    @PostMapping("/{id}/delete")
    public String deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return "redirect:/alerts";
    }
}