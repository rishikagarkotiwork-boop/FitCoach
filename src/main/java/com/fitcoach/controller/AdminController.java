package com.fitcoach.controller;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.Content;
import com.fitcoach.model.ContentStatus;
import com.fitcoach.model.Role;
import com.fitcoach.repository.AppUserRepository;
import com.fitcoach.repository.ContentRepository;
import com.fitcoach.repository.FeedbackRepository;
import com.fitcoach.service.CleanupService;
import com.fitcoach.service.SettingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AppUserRepository userRepo;
    private final ContentRepository contentRepo;
    private final FeedbackRepository feedbackRepo;
    private final SettingService settings;
    private final CleanupService cleanup;
    private final PasswordEncoder encoder;

    public AdminController(AppUserRepository userRepo, ContentRepository contentRepo, FeedbackRepository feedbackRepo,
                           SettingService settings, CleanupService cleanup, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.contentRepo = contentRepo;
        this.feedbackRepo = feedbackRepo;
        this.settings = settings;
        this.cleanup = cleanup;
        this.encoder = encoder;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<ContentStatus> statuses = Arrays.asList(ContentStatus.values());
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("roles", Role.values());
        model.addAttribute("contentItems", contentRepo.findAll());
        model.addAttribute("feedback", feedbackRepo.findTop10ByOrderByCreatedAtDesc());
        model.addAttribute("allowRegistration", settings.get(SettingService.ALLOW_REGISTRATION, "true"));
        model.addAttribute("maxPlans", settings.get(SettingService.MAX_PLANS_PER_COACH, "20"));
        model.addAttribute("statLabels", statuses.stream().map(Enum::name).toList());
        model.addAttribute("statData", statuses.stream().map(contentRepo::countByStatus).toList());
        return "admin/dashboard";
    }

    // ---- user management ----

    @PostMapping("/users")
    public String createUser(@RequestParam String name, @RequestParam String email, @RequestParam Role role,
                             @RequestParam String password, RedirectAttributes ra) {
        email = email.trim().toLowerCase();
        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            ra.addFlashAttribute("error", "Name, email and a password of at least 6 characters are required.");
        } else if (userRepo.existsByEmail(email)) {
            ra.addFlashAttribute("error", "A user with that email already exists.");
        } else {
            userRepo.save(new AppUser(name.trim(), email, encoder.encode(password), role));
            ra.addFlashAttribute("msg", "User '" + name.trim() + "' created successfully.");
        }
        return "redirect:/admin#users";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id, @RequestParam String name, @RequestParam String email,
                             @RequestParam Role role, @RequestParam boolean enabled,
                             Authentication auth, RedirectAttributes ra) {
        AppUser user = userRepo.findById(id).orElse(null);
        email = email.trim().toLowerCase();
        if (user == null) {
            ra.addFlashAttribute("error", "User not found.");
        } else if (name.isBlank() || email.isBlank()) {
            ra.addFlashAttribute("error", "Name and email cannot be empty.");
        } else if (!email.equals(user.getEmail()) && userRepo.existsByEmail(email)) {
            ra.addFlashAttribute("error", "That email is already used by another account.");
        } else if (user.getEmail().equals(auth.getName()) && (role != Role.ADMIN || !enabled)) {
            ra.addFlashAttribute("error", "You cannot demote or disable your own account.");
        } else {
            boolean selfEmailChange = user.getEmail().equals(auth.getName()) && !email.equals(user.getEmail());
            user.setName(name.trim());
            user.setEmail(email);
            user.setRole(role);
            user.setEnabled(enabled);
            userRepo.save(user);
            ra.addFlashAttribute("msg", selfEmailChange
                    ? "User updated. Your email changed - please sign in again."
                    : "User '" + user.getName() + "' updated successfully.");
        }
        return "redirect:/admin#users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        AppUser user = userRepo.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User not found.");
        } else if (user.getEmail().equals(auth.getName())) {
            ra.addFlashAttribute("error", "You cannot delete your own account.");
        } else {
            cleanup.deleteUser(user);
            ra.addFlashAttribute("msg", "User '" + user.getName() + "' deleted successfully.");
        }
        return "redirect:/admin#users";
    }

    // ---- content moderation ----

    @PostMapping("/content/{id}/{decision}")
    public String moderate(@PathVariable Long id, @PathVariable String decision, RedirectAttributes ra) {
        Content content = contentRepo.findById(id).orElse(null);
        if (content == null || !(decision.equals("approve") || decision.equals("reject"))) {
            ra.addFlashAttribute("error", "Content not found.");
        } else {
            content.setStatus(decision.equals("approve") ? ContentStatus.APPROVED : ContentStatus.REJECTED);
            contentRepo.save(content);
            ra.addFlashAttribute("msg", "Content '" + content.getTitle() + "' " + content.getStatus() + ".");
        }
        return "redirect:/admin#content";
    }

    // ---- system settings ----

    @PostMapping("/settings")
    public String saveSettings(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        String maxPlans = params.getOrDefault(SettingService.MAX_PLANS_PER_COACH, "20").trim();
        String siteName = params.getOrDefault(SettingService.SITE_NAME, "").trim();
        if (siteName.isEmpty() || !maxPlans.matches("\\d{1,4}")) {
            ra.addFlashAttribute("error", "Site name is required and max plans must be a number.");
            return "redirect:/admin#settings";
        }
        settings.set(SettingService.SITE_NAME, siteName);
        settings.set(SettingService.ALLOW_REGISTRATION,
                String.valueOf(Boolean.parseBoolean(params.getOrDefault(SettingService.ALLOW_REGISTRATION, "true"))));
        settings.set(SettingService.MAX_PLANS_PER_COACH, maxPlans);
        settings.set(SettingService.ANNOUNCEMENT, params.getOrDefault(SettingService.ANNOUNCEMENT, "").trim());
        ra.addFlashAttribute("msg", "System settings updated successfully.");
        return "redirect:/admin#settings";
    }
}
