package com.fitcoach.config;

import com.fitcoach.repository.AppUserRepository;
import com.fitcoach.service.SettingService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/** Adds site-wide values (site name, announcement, logged-in user) to every view. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final SettingService settings;
    private final AppUserRepository userRepo;

    public GlobalModelAdvice(SettingService settings, AppUserRepository userRepo) {
        this.settings = settings;
        this.userRepo = userRepo;
    }

    @ModelAttribute
    public void addGlobals(Model model, Principal principal) {
        model.addAttribute("siteName", settings.get(SettingService.SITE_NAME, "FitCoach"));
        model.addAttribute("announcement", settings.get(SettingService.ANNOUNCEMENT, ""));
        if (principal != null) {
            userRepo.findByEmail(principal.getName()).ifPresent(u -> model.addAttribute("currentUser", u));
        }
    }
}
