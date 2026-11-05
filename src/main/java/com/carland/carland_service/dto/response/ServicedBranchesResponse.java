package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: Aracın hizmet aldığı şubeler, rozet sırasıyla.
 * en: Branches where the car was serviced, in badge order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicedBranchesResponse {
    @Builder.Default
    private List<ServicedBranchResponse> branches = new ArrayList<>();
}
