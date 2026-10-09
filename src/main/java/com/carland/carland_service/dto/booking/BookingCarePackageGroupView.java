package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Paket içindeki bir davranış grubu (Dəyişdirmə, Əlavə etmə, Yoxlanış). Boş grup dönmez.
 * en: One behavior group inside a care package (Replace, Top up, Check). Empty groups are omitted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCarePackageGroupView {
    Long id;
    String name;
    List<BookingCarePackageServiceView> services;
}
