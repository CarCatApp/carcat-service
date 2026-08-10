package com.carland.carland_service.test_sima_idda.dto.response;

import com.carland.carland_service.test_sima_idda.dto.idda.IddaCarItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * IDDA car list + VIN match preview against customer's local cars (no DB write).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IddaCarListResponse {
    private String fin;
    private List<IddaCarItem> iddaCars;
    private List<String> localVins;
    private List<String> matchedVins;
    private String note;
}
