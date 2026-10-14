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
        String lang = BookingMineService.langOf(acceptLanguage);
        return BookingPackagePriceInfoResponse.builder()
                .packageId(pkg.getId())
                .price(price)
                .currency(pkg.getCurrency() == null ? "AZN" : pkg.getCurrency())
                .title(text(lang,
                        "Qiymət şərtləri",
                        "Price terms",
                        "Условия цены"))
                .subtitle(text(lang,
                        "Qiymətə yalnız xidmət haqqı daxildir.",
                        "The price includes the service fee only.",
                        "В цену включена только стоимость услуги."))
                .description(text(lang,
                        "Ehtiyat hissələri və sərfiyyat materialları (yağ, filtr və s.) ümumi məbləğə daxil deyil, onlar ayrıca hesablanacaq.",
                        "Spare parts and consumables (oil, filters, etc.) are not included in the total and will be calculated separately.",
                        "Запчасти и расходные материалы (масло, фильтры и т. д.) не входят в общую сумму и будут рассчитаны отдельно."))
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
