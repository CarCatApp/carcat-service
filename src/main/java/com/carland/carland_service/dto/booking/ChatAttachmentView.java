package com.carland.carland_service.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ChatAttachmentView {
    String uploadId;
    String kind;
    String url;
    String fileName;
    Long sizeBytes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long durationMs;
    /** audio/mp4 for kind audio. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String mime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String contentType;
    Integer width;
    Integer height;
    Boolean unavailable;
}
