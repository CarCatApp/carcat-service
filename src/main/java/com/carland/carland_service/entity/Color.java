package com.carland.carland_service.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


/**
 * tr: "colors" tablosunu modelleyen entity; araç rengi sözlük kaydını temsil eder.
 * en: Entity modeling the "colors" table; represents a car color lookup record.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "colors")
public class Color {

    @Id
    Long colorId;
    /** English display name. */
    String color;
    @JsonIgnore
    String az;
    @JsonIgnore
    String ru;
    String hex;

    /**
     * tr: Accept-Language'e göre gösterilecek isim (az/ru kolon, yoksa İngilizce color).
     * en: Display name for Accept-Language (az/ru columns, otherwise English color).
     */
    public String nameForLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return firstNonBlank(az, color);
        }
        String key = lang.toLowerCase();
        if (key.startsWith("en")) {
            return firstNonBlank(color, az);
        }
        if (key.startsWith("ru")) {
            return firstNonBlank(ru, color);
        }
        return firstNonBlank(az, color);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
