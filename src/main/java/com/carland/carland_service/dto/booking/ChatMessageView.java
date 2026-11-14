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
public class ChatMessageView {
    String clientMessageId;
    String serverId;
    String authorId;
    /** customer | branch | system — partner-ui uses this; mobile may ignore. */
    String fromRole;
    String createdAt;
    String kind;
    String text;
    String delivery;
    Boolean rebook;
    Object attachment;
}
