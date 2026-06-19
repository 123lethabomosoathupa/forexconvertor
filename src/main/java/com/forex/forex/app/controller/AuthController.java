package com.forex.forexapp.controller;

import com.forex.forexapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String logout,
                            @RequestParam(required = false) String error,
                            Model model) {
        if (logout != null) model.addAttribute("logoutMsg", "Logged out successfully.");
        if (error  != null) model.addAttribute("errorMsg",  "Invalid username or password.");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String username,
                                 @RequestParam String password,
                                 Model model) {
        try {
            userService.register(username, password);
            return "redirect:/login?registered";
        } catch (RuntimeException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "register";
        }
    }
}