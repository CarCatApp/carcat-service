package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.PartnerId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * tr: Araç kartındaki rozet listeleri. Partner id'leri kalır; rozet fotoğrafı şube id'si ile çekilir.
 *     Hyper ziyaretinde {@code branch_id} boşsa geçici şube 10 yazılır.
 * en: Badge id lists on the car card. Partner ids stay; the badge photo is fetched by branch id.
 *     A Hyper visit with an empty {@code branch_id} uses temporary branch 10.
 */
public final class ServicedBadgeIds {

    /** GEÇİCİ. Partner 1 ve boş branch_id. Hyper şube id yollayana kadar. */
    public static final long HYPER_TEMPORARY_BRANCH_ID = 10L;

    /** GEÇİCİ. dealer "Xəqani Express" → Hyper Servis 28 May. */
    public static final long HYPER_XAQANI_BRANCH_ID = 13L;

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
            Long branchId = resolvedBranchId(visit);
            if (branchId != null) {
                ordered.add(String.valueOf(branchId));
            }
        }
        return new ArrayList<>(ordered);
    }

    /**
     * tr: Rozet şubesi. {@code branch_id} doluysa o.
     *     Hyper ve boşsa dealer: Xəqani Express → 13, Babək Ekspress → 10, diğer Hyper yazısı 10.
     *     Başka partnerde boşsa yok.
     * en: Badge branch. The stored {@code branch_id} when set.
     *     Hyper with an empty one uses dealer: Xəqani Express → 13, Babək Ekspress → 10, any other Hyper text stays 10.
     *     Another partner with an empty one has none.
     */
    public static Long resolvedBranchId(Visit visit) {
        if (visit == null) {
            return null;
        }
        if (visit.getBranchId() != null) {
            return visit.getBranchId();
        }
        if (!PartnerId.HYPER.getId().equals(visit.getServiceCenterId())) {
            return null;
        }
        Long fromDealer = temporaryDealerBranchId(visit.getDealer());
        return fromDealer != null ? fromDealer : HYPER_TEMPORARY_BRANCH_ID;
    }

    /**
     * tr: GEÇİCİ. Hyper branch id yollayana kadar yalnız bu iki dealer adı.
     *     Başka yazı, boş ve eski test metni null döner.
     * en: TEMPORARY. Only these two dealer names until Hyper sends a branch id.
     *     Any other text, a blank and old test text return null.
     */
    public static Long temporaryDealerBranchId(String dealer) {
        String key = foldDealer(dealer);
        if ("xeqani express".equals(key) || "xaqani express".equals(key)) {
            return HYPER_XAQANI_BRANCH_ID;
        }
        if ("babek ekspress".equals(key)) {
            return HYPER_TEMPORARY_BRANCH_ID;
        }
        return null;
    }

    private static String foldDealer(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT)
                .replace('ə', 'e')
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ğ', 'g')
                .replace('ç', 'c');
        return lower.replaceAll("\\s+", " ");
    }

    private static boolean same(List<String> current, List<String> updated) {
        List<String> left = current == null ? List.of() : current;
        return left.equals(updated);
    }
}
