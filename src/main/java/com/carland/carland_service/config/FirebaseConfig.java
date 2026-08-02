package com.carland.carland_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

/**
 * tr: Firebase Admin SDK'yı uygulama açılışında /app/firebase.json servis hesabı dosyasıyla başlatan
 *     konfigürasyondur; push notification gönderimi için gereklidir. (Eski adı: FirebaseTokenService)
 * en: Configuration that initializes the Firebase Admin SDK at application startup using the
 *     /app/firebase.json service account file; required for push notification delivery.
 *     (Former name: FirebaseTokenService)
 */
@Configuration
public class FirebaseConfig {

    /**
     * tr: Uygulama ayağa kalkarken Firebase'i bir kez initialize eder; zaten başlatılmışsa tekrar başlatmaz.
     * en: Initializes Firebase once during application startup; skips when already initialized.
     */
    @PostConstruct
    public void initialize() throws IOException {

        FileSystemResource resource = new FileSystemResource("/app/firebase.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            System.out.println("Firebase initialized successfully!");
        }
    }
}
