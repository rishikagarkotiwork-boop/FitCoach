package com.fitcoach.service;

import com.fitcoach.model.Setting;
import com.fitcoach.repository.SettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingService {

    public static final String SITE_NAME = "siteName";
    public static final String ALLOW_REGISTRATION = "allowRegistration";
    public static final String MAX_PLANS_PER_COACH = "maxPlansPerCoach";
    public static final String ANNOUNCEMENT = "announcement";

    private final SettingRepository repo;

    public SettingService(SettingRepository repo) {
        this.repo = repo;
    }

    public String get(String key, String defaultValue) {
        return repo.findById(key).map(Setting::getValue).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        repo.save(new Setting(key, value));
    }
}
