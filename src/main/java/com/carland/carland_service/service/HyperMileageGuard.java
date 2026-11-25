package com.carland.carland_service.service;

import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;

/**
 * tr: Hyper aynı kaydı tekrar gönderince daha düşük kilometreyi yazmasın.
 *     Kayıtlı değer boşsa gelen kabul edilir. Gelen daha yüksekse uygulanır.
 * en: A repeated Hyper snapshot must not write a lower mileage over the stored one.
 *     A blank stored value accepts the incoming mileage. A higher incoming value is applied.
 */
public final class HyperMileageGuard {

    private HyperMileageGuard() {
    }

    public static Integer keepIfNotLower(Integer stored, Integer incoming) {
        if (stored == null) {
            return incoming;
        }
        if (incoming == null || incoming < stored) {
            return stored;
        }
        return incoming;
    }

    /**
     * tr: Başlık kilometresini yüksek tuttuysak, Hyper'ın yeni satırlarındaki düşük veya boş doneKm'yi de o değere çeker.
     * en: After the visit header keeps the higher mileage, pulls a lower or blank line doneKm up to that value.
     */
    public static void stampLinesKeptAbove(Visit visit, Integer incomingMileage) {
        Integer kept = visit == null ? null : visit.getLastServiceMileage();
        if (visit == null || visit.getServices() == null || kept == null
                || incomingMileage == null || kept <= incomingMileage) {
            return;
        }
        for (VisitServiceLine line : visit.getServices()) {
            if (line.getDoneKm() == null || line.getDoneKm() <= incomingMileage) {
                line.setDoneKm(kept);
            }
        }
    }

    /**
     * tr: Karttaki son km ziyaretten yüksekse ziyaret başlığını ve satır kilometresini karta çeker.
     * en: When the card's last km is higher than the visit, raises the visit header and line mileage to the card.
     */
    public static boolean raiseVisitMileage(Visit visit, VisitServiceLine line, Integer keptKm) {
        if (visit == null || keptKm == null) {
            return false;
        }
        boolean changed = false;
        if (visit.getLastServiceMileage() == null || visit.getLastServiceMileage() < keptKm) {
            visit.setLastServiceMileage(keptKm);
            changed = true;
        }
        if (line != null && line.getDoneKm() != null && line.getDoneKm() < keptKm) {
            line.setDoneKm(keptKm);
            changed = true;
        }
        return changed;
    }
}
