package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.PartnerId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * tr: Araç kartındaki rozet listeleri. Partner id'leri kalır; rozet fotoğrafı şube id'si ile çekilir.
 *     Hyper ziyaretinde {@code branch_id} boşsa geçici şube 10 yazılır.
 * en: Badge id lists on the car card. Partner ids stay; the badge photo is fetched by branch id.
 *     A Hyper visit with an empty {@code branch_id} uses temporary branch 10.
 */
public final class ServicedBadgeIds {

    /** GEÇİCİ. Partner 1 ve boş branch_id. */
    public static final long HYPER_TEMPORARY_BRANCH_ID = 10L;

    private ServicedBadgeIds() {
    }

    /**
     * @return true when {@code car} was changed and must be saved
     */
    public static boolean applyIfChanged(Car car, List<Visit> visits) {
        List<String> partners = partnerIds(visits);
        List<String> branches = branchIds(visits);
        boolean partnersSame = same(car.getServicedPartnerIds(), partners);
        boolean branchesSame = car.getServicedBranchIds() != null && same(car.getServicedBranchIds(), branches);
        if (partnersSame && branchesSame) {
            return false;
        }
        car.setServicedPartnerIds(partners);
        car.setServicedBranchIds(branches);
        return true;
    }

    public static List<String> partnerIds(List<Visit> visits) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (visits == null) {
            return List.of();
        }
        for (Visit visit : visits) {
            if (visit != null && visit.getServiceCenterId() != null) {
                ordered.add(String.valueOf(visit.getServiceCenterId()));
            }
        }
        return new ArrayList<>(ordered);
    }

    public static List<String> branchIds(List<Visit> visits) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (visits == null) {
            return List.of();
        }
        for (Visit visit : visits) {
            Long branchId = branchId(visit);
            if (branchId != null) {
                ordered.add(String.valueOf(branchId));
            }
        }
        return new ArrayList<>(ordered);
    }

    private static Long branchId(Visit visit) {
        if (visit == null) {
            return null;
        }
        if (visit.getBranchId() != null) {
            return visit.getBranchId();
        }
        if (PartnerId.HYPER.getId().equals(visit.getServiceCenterId())) {
            return HYPER_TEMPORARY_BRANCH_ID;
        }
        return null;
    }

    private static boolean same(List<String> current, List<String> updated) {
        List<String> left = current == null ? List.of() : current;
        return left.equals(updated);
    }
}
