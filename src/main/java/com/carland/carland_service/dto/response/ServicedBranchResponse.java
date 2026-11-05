package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Rozet ekranındaki tek şube. {@code lastServiceAt} gösterim metnidir.
 * en: One branch on the badge screen. {@code lastServiceAt} is display text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicedBranchResponse {
    private Long branchId;
    private String branchName;
    private Double lat;
    private Double lng;
    private Integer completedServiceCount;
    private String lastServiceAt;
}
