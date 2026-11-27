package com.carland.carland_service.dto.booking;

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
public class ChatSendRequest {
    String clientMessageId;
    String kind;
    String text;
    /** Client hint only; server clock wins. */
    String createdAt;
    /** For kind image|file|audio — from prior upload. */
    String uploadId;
    String fileName;
    Long sizeBytes;
    /** Client hint for kind audio. Server keeps ffprobe when they differ. */
    Long durationMs;
    Integer width;
    Integer height;
}
