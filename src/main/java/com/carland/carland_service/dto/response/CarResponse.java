package com.carland.carland_service.dto.response;


import com.carland.carland_service.util.PlateNumberSerializer;
import com.carland.carland_service.util.PlateNumbers;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * tr: CarController araç sorgularında dönen yanıt DTO'su. Aracın kimlik, teknik özellik, kilometre ve toplam masraf bilgilerini içerir.
 * en: Response DTO returned by CarController car queries. Contains the car's identity, technical specs, mileage and all-time cost info.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponse {
    private Long carId;
    private Long customerId;
    private String vin;
    @JsonSerialize(using = PlateNumberSerializer.class)
    private String plateNumber;
    private String brand;
    private String model;
    private Integer modelYear;
    private String color;
    private String engineType;
    private Integer engineVolume;
    private String transmissionType;
    private Long mileage;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private String bodyType;
    private String message;
    private List<String> vinProvidedFields;
    private List<String> iddaProvidedFields;
    private Boolean fromIdda;
    private Boolean engineTypeLocked;
    private Long colorId;
    /** tr: IDDA'nın Azerice renk metni. Araçta saklanır. colorId doluysa null. / en: IDDA color text in Azerbaijani, stored on the car. Null when colorId is set. */
    private String iddaColor;
    private Integer iddaStatusCode;
    private String iddaStatusName;
    private String iddaStatusMessage;
    /**
     * tr: Yalnız {@code /check/plate}. IDDA'nın döndürdüğü RequestIdentifier. Code 0, Code 4 ve DB tekrarı dahil.
     * en: Only {@code /check/plate}. The RequestIdentifier IDDA returned, including Code 0, Code 4 and a database replay.
     */
    @JsonProperty("RequestIdentifier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String requestIdentifier;
    /**
     * tr: Yalnız {@code /check/plate}. Code 4 və ya {@code ServiceRestrictedByPerson}. Mesaj metnine bakılmaz.
     * en: Only {@code /check/plate}. Code 4 or {@code ServiceRestrictedByPerson}. The message text is ignored.
     */
    @JsonProperty("isRestrict")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isRestrict;
    /** Partner ids for badge logo fetch ({@code GET /photo/for/partner/badge-logo/get/{partnerId}}). */
    private List<String> servicedPartnerIds;
    private String resource;
    private BigDecimal allTimeCost;
    private Long engineTypeId;
    /** OTP-style: generate / prompt-field edit locked until this instant. */
    private LocalDateTime lockedUntil;
    /** Seconds left until unlock; 0 = not locked. Flutter countdown. */
    private Long remainingSeconds;

    /**
     * tr: JSON plakayı görüntü formatında verir. Kayıt bitişik kalabilir ({@code 00XX000} → {@code 00 XX 000}).
     * en: JSON returns the display plate. The stored value may stay concatenated ({@code 00XX000} → {@code 00 XX 000}).
     */
    public String getPlateNumber() {
        return PlateNumbers.format(plateNumber);
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = PlateNumbers.format(plateNumber);
    }
}
