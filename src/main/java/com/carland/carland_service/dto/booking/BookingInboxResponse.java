package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInboxResponse {
    int page;
    int pageSize;
    long total;
    int totalPages;
    long pending;
    long confirmed;
    long completed;
    long rejected;
    List<BookingView> items;
}
