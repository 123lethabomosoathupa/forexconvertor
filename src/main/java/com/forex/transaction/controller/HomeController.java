package com.forex.transaction.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth) {
        // If already logged in → dashboard, otherwise → login
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
}
