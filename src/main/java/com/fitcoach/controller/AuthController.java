package com.fitcoach.controller;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.Role;
import com.fitcoach.repository.AppUserRepository;
import com.fitcoach.service.SettingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AppUserRepository userRepo;
    private final PasswordEncoder encoder;
    private final SettingService settings;

    public AuthController(AppUserRepository userRepo, PasswordEncoder encoder, SettingService settings) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.settings = settings;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("open", registrationOpen());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                           @RequestParam(required = false) String goal, RedirectAttributes ra) {
        if (!registrationOpen()) {
            ra.addFlashAttribute("error", "Registration is currently closed.");
            return "redirect:/register";
        }
        email = email.trim().toLowerCase();
        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            ra.addFlashAttribute("error", "Name, email and a password of at least 6 characters are required.");
            return "redirect:/register";
        }
        if (userRepo.existsByEmail(email)) {
            ra.addFlashAttribute("error", "An account with that email already exists.");
            return "redirect:/register";
        }
        AppUser user = new AppUser(name.trim(), email, encoder.encode(password), Role.USER);
        user.setGoal(goal);
        userRepo.save(user);
        ra.addFlashAttribute("msg", "Account created. Please sign in.");
        return "redirect:/login";
    }

    /** Sends the logged-in person to the dashboard for their role. */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean coach = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        return admin ? "redirect:/admin" : coach ? "redirect:/coach" : "redirect:/user";
    }

    private boolean registrationOpen() {
        return Boolean.parseBoolean(settings.get(SettingService.ALLOW_REGISTRATION, "true"));
    }
}
