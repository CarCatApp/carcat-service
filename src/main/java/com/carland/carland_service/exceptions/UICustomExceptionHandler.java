package com.carland.carland_service.exceptions;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * tr: UI (Thymeleaf) sayfaları için exception handler; InviteException'ı "invite-error" hata sayfasına yönlendirir.
 * en: Exception handler for UI (Thymeleaf) pages; routes InviteException to the "invite-error" error page.
 */
@ControllerAdvice
public class UICustomExceptionHandler {
    @ExceptionHandler(InviteException.class)
    public String handleInviteException(InviteException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "invite-error";
    }
}
