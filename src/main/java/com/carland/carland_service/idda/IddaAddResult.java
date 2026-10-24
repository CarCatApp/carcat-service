package com.carland.carland_service.idda;

import com.carland.carland_service.entity.Car;

/**
 * tr: IDDA add sonucu. created true ise bakım şablonu bu çağrıda kuruldu ve Hyper senkronu planlanır.
 * en: IDDA add result. created true means the maintenance template was built in this call and Hyper sync is scheduled.
 */
public record IddaAddResult(Car car, boolean created) {
}
