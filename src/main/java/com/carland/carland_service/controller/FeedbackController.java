package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.FeedbackRequest;
import com.carland.carland_service.dto.response.FeedbackResponse;
import com.carland.carland_service.enums.FeedbackTypes;
import com.carland.carland_service.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * tr: Geri bildirim REST controller'ı; opsiyonel dosya ekiyle geri bildirim gönderme ve geri bildirim tiplerini listeleme uçlarını sunar.
 * en: REST controller for feedback; exposes endpoints to submit feedback with an optional file attachment and to list the available feedback types.
 */
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * tr: Multipart istekle geri bildirim kaydeder; "data" bölümünde FeedbackRequest, opsiyonel "file" bölümünde ek dosya alır ve kaydedilen geri bildirimi döner.
     * en: Saves feedback from a multipart request; takes the FeedbackRequest in the "data" part and an optional attachment in the "file" part, returns the saved feedback.
     */
    @PostMapping(value = "/push", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE} , produces = MediaType.APPLICATION_JSON_VALUE)
    public FeedbackResponse pushFeedback(@RequestPart(value = "file", required = false) MultipartFile file,
                                         @RequestPart("data") FeedbackRequest feedbackRequest,
                                         @RequestBody(required = false) byte[] rawBody,
                                         @RequestHeader("phoneNumber") String phoneNumber,
                                         @RequestHeader("X-User-Id") String userIdHeader,
                                         @RequestHeader("Accept-Language") String acceptLanguage) {
        return feedbackService.pushFeedback(file, feedbackRequest, phoneNumber, userIdHeader, acceptLanguage);
    }


    /**
     * tr: FeedbackTypes enum'undaki tüm geri bildirim tiplerinin adlarını liste olarak döner.
     * en: Returns the names of all feedback types defined in the FeedbackTypes enum as a list.
     */
    @GetMapping("/get/types")
    public List<String> getFeedbackTypeList() {
        return Arrays.stream(FeedbackTypes.values())
                .map(Enum::name)
                .toList();
    }

}
