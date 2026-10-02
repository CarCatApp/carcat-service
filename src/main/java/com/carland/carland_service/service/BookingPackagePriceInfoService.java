package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingPackagePriceInfoResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * tr: Paket fiyatı bilgi kutusu. Metin Accept-Language dilinde döner.
 * en: Package-price info sheet. Copy follows Accept-Language.
 */
@Service
@RequiredArgsConstructor
public class BookingPackagePriceInfoService {

    private final BranchCarePackageRepository carePackageRepository;

    @Transactional(readOnly = true)
    public BookingPackagePriceInfoResponse info(Long packageId, String acceptLanguage) {
        BranchCarePackage pkg = carePackageRepository.findById(packageId == null ? -1L : packageId)
                .orElseThrow(() -> new ResourceNotFoundException("care package not found"));
        Branch branch = pkg.getBranch();
        if (!Boolean.TRUE.equals(pkg.getActive())
                || branch == null
                || !Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("care package not found");
        }
        int price = pkg.getPrice() == null ? 0 : pkg.getPrice();
        String fee = price + " ₼";
        String lang = BookingMineService.langOf(acceptLanguage);
        return BookingPackagePriceInfoResponse.builder()
                .packageId(pkg.getId())
                .price(price)
                .currency(pkg.getCurrency() == null ? "AZN" : pkg.getCurrency())
                .title(text(lang,
                        "Paket qiyməti haqqında",
                        "About the package price",
                        "О цене пакета"))
                .subtitle(text(lang,
                        fee + " yalnız xidmət haqqıdır.",
                        fee + " is the service fee only.",
                        fee + " — это только стоимость услуги."))
                .description(text(lang,
                        "Ehtiyat hissələri, mayelər və digər tələb olunan materiallar qiymətə daxil deyil və ayrıca ödənilə bilər",
                        "Parts, fluids and other required materials are not included and may be charged separately",
                        "Запчасти, жидкости и другие необходимые материалы не включены и могут оплачиваться отдельно"))
                .build();
    }

    private static String text(String lang, String az, String en, String ru) {
        if ("en".equals(lang)) {
            return en;
        }
        if ("ru".equals(lang)) {
            return ru;
        }
        return az;
    }
}
