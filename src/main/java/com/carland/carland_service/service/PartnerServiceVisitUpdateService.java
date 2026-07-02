package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.response.v2.PartnerUpdateServiceVisitResult;

public interface PartnerServiceVisitUpdateService {
    PartnerUpdateServiceVisitResult update(HyperVehicleByVinResponse request);
}
