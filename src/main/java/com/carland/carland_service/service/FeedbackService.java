package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.FeedbackRequest;
import com.carland.carland_service.dto.response.FeedbackResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * tr: Kullanıcı geri bildirimlerinin (opsiyonel dosya ekiyle) alınıp iletilmesi için servis sözleşmesidir.
 * en: Service contract for receiving and forwarding user feedback (with an optional file attachment).
 */
public interface FeedbackService {

    /**
     * tr: Kullanıcının geri bildirimini (varsa ekli dosyayla) alır, ilgili adrese mail olarak iletir ve sonucu döner.
     * en: Receives the user's feedback (with an attached file if provided), forwards it by e-mail, and returns the result.
     */
    FeedbackResponse pushFeedback(MultipartFile file, FeedbackRequest feedbackRequest, String phoneNumber, String userIdHeader, String acceptLanguage);







}
