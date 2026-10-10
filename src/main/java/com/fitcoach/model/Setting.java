package com.fitcoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Setting {

    @Id
    @Column(name = "setting_key")
    private String key;

    @Column(name = "setting_value", length = 500)
    private String value;

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
