package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingBranchSocialLinkView {
    /** "instagram: hesap" gibi tek satır. */
    String socialMediaLink;
}
