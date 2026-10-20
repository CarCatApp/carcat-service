package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin color sort satırını siler. colors tablosunda active kolonu yok, kayıt DB'den gider.
 * en: Deletes an admin color-sort row. colors has no active column, so the row is removed from the DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminColorDeleteRequest {
    Long colorId;
}
