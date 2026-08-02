package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.FeedbackRequest;
import com.carland.carland_service.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * tr: MailService'in Brevo (eski adıyla Sendinblue) SMTP API üzerinden çalışan implementasyonudur;
 *     geri bildirim maillerini HTML gövde ve opsiyonel Base64 ekiyle gönderir.
 * en: Implementation of MailService backed by the Brevo (formerly Sendinblue) SMTP API;
 *     sends feedback e-mails with an HTML body and an optional Base64-encoded attachment.
 */
@Service
@RequiredArgsConstructor
public class BrevoMailServiceImpl implements MailService {

    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    /**
     * tr: Geri bildirim mailini Brevo API'ye POST ederek gönderir; dosya varsa Base64'e çevirip
     *     ek olarak iliştirir. Brevo 4xx/5xx dönerse veya başka bir hata oluşursa açıklayıcı
     *     mesajla RuntimeException fırlatır.
     * en: Sends the feedback mail by POSTing to the Brevo API; when a file is provided it is
     *     Base64-encoded and attached. Throws a RuntimeException with a descriptive message on
     *     Brevo 4xx/5xx responses or any other failure.
     */
    @Override
    public void sendFeedbackMail(FeedbackRequest request, MultipartFile file, String ticketId, String customerPhone) {

        try {

            Map<String, Object> body = new HashMap<>();

            body.put("sender", Map.of("name", "Carland Feedback", "email", "noreply@digital-innovation.agency"));

            body.put("to", List.of(Map.of("email", "app.carcat.feedback@gmail.com")));

            body.put("subject", "[FEEDBACK][" + request.getType() + "] Ticket #" + ticketId);

            body.put("htmlContent", buildBody(request, ticketId, customerPhone));

            if (file != null && !file.isEmpty()) {
                String encoded = Base64.getEncoder().encodeToString(file.getBytes());

                body.put("attachment", List.of(Map.of("name", file.getOriginalFilename(), "content", encoded)));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.add("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_URL, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Brevo error: " + response.getBody());
            }

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Brevo 4xx error: " + e.getResponseBodyAsString(), e);

        } catch (HttpServerErrorException e) {
            throw new RuntimeException("Brevo 5xx error: " + e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            throw new RuntimeException("Mail göndərilərkən xəta baş verdi", e);
        }
    }

    private String buildBody(FeedbackRequest request,
                             String ticketId,
                             String customerPhone) {

        return """
            <p><b>New Feedback</b></p>
            <p>
            <b>Ticket ID:</b> %s<br/>
            <b>Customer phone:</b> %s<br/>
            <b>Type:</b> %s<br/>
            <b>Subject:</b> %s<br/>
            <b>Rating:</b> %s
            </p>
            <p><b>Description:</b><br/>%s</p>
        """.formatted(
                ticketId,
                customerPhone,
                request.getType(),
                request.getSubject(),
                request.getRating(),
                request.getDescription()
        );
    }
}

