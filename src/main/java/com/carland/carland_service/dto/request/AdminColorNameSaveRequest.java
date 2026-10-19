package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Değişen renk adları ve yeni satırlar. Değişmeyen satır gelmez.
 * en: Changed color names and new rows. Unchanged rows are omitted.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminColorNameSaveRequest {
    List<AdminColorNameRow> rows;
}
