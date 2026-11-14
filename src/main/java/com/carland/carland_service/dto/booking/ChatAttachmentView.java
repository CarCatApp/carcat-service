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
public class ChatAttachmentView {
    String uploadId;
    String kind;
    String url;
    String fileName;
    Long sizeBytes;
    Integer width;
    Integer height;
    Boolean unavailable;
}
