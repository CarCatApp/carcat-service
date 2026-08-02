package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.FeedbackRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * tr: E-posta gönderim işlemlerinin (geri bildirim maili) servis sözleşmesidir.
 * en: Service contract for e-mail sending operations (feedback mail).
 */
public interface MailService {
    /**
     * tr: Geri bildirim içeriğini (varsa dosya ekiyle) hedef adrese e-posta olarak gönderir.
     * en: Sends the feedback content (with a file attachment if provided) to the target address by e-mail.
     */
    void sendFeedbackMail(FeedbackRequest feedbackRequest, MultipartFile file, String string, String customerPhone);



}
