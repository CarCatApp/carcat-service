package com.carland.carland_service.test_sima_idda;

import java.util.Map;

/**
 * CRCT-259: SIMA numeric codes → user AZ/EN/RU. Flutter shows {@code message}, not raw SIMA text.
 * Copy: {@code SIMA_KYC_Xeta_last (Aziz).xlsx}; 710 CTA is support (PO), not try-later.
 */
public final class SimaKycErrorCatalog {

    private record Tr(String az, String en, String ru) {
    }

    private static final Map<Integer, Tr> BY_CODE = Map.ofEntries(
            e(70000, "Sistemdə müvəqqəti nasazlıq yarandı. Zəhmət olmasa bir az sonra yenidən cəhd edin.",
                    "A temporary system issue occurred. Please try again in a moment.",
                    "Произошла временная неполадка в системе. Пожалуйста, повторите попытку через некоторое время."),
            e(70001, "Xidmətə qoşulmada problem yarandı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "There was a problem connecting to the service. Please contact support.",
                    "Возникла проблема при подключении к сервису. Пожалуйста, обратитесь в службу поддержки."),
            e(70002, "Daxil etdiyiniz məlumatlarda yanlışlıq var. Zəhmət olmasa yenidən yoxlayıb daxil edin.",
                    "There is an error in the information you entered. Please check and enter it again.",
                    "В введённых вами данных есть ошибка. Пожалуйста, проверьте и введите заново."),
            e(710, "Bu xidmət hazırda əlçatan deyil. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "This service is currently unavailable. Please contact support.",
                    "Данный сервис в настоящее время недоступен. Пожалуйста, обратитесь в службу поддержки."),
            e(750, "Yoxlanış zamanı xəta baş verdi. Zəhmət olmasa yenidən cəhd edin.",
                    "An error occurred during verification. Please try again.",
                    "Произошла ошибка во время проверки. Пожалуйста, повторите попытку."),
            e(7080, "Şəkil yüklənmədi. Zəhmət olmasa kameranın işlədiyinə əmin olub yenidən cəhd edin.",
                    "The image failed to upload. Please make sure your camera is working and try again.",
                    "Изображение не загрузилось. Пожалуйста, убедитесь, что камера работает, и повторите попытку."),
            e(7081, "Şəkil formatında problem var. Zəhmət olmasa yenidən çəkib göndərin.",
                    "There is a problem with the image format. Please retake and resend it.",
                    "Возникла проблема с форматом изображения. Пожалуйста, сделайте снимок заново и отправьте его."),
            e(7082, "Şəkil formatı dəstəklənmir. Zəhmət olmasa yenidən cəhd edin.",
                    "The image format is not supported. Please try again.",
                    "Формат изображения не поддерживается. Пожалуйста, повторите попытку."),
            e(751, "Canlılıq yoxlanışı uğursuz oldu. Zəhmət olmasa yenidən cəhd edin.",
                    "The liveness check was unsuccessful. Please try again.",
                    "Проверка на «живость» не пройдена. Пожалуйста, повторите попытку."),
            e(752, "Şəxsiyyətinizi təsdiqləyə bilmədik. Zəhmət olmasa yenidən cəhd edin.",
                    "We were unable to verify your identity. Please try again.",
                    "Нам не удалось подтвердить вашу личность. Пожалуйста, повторите попытку."),
            e(7530, "Kadr çərçivəsində birdən artıq insan üzü aşkarlandı. Zəhmət olmasa yenidən cəhd edin.",
                    "More than one face was detected in the frame. Please try again.",
                    "В кадре обнаружено более одного лица. Пожалуйста, повторите попытку."),
            e(716, "Əməliyyat tamamlanmadı. Zəhmət olmasa bir az sonra yenidən cəhd edin.",
                    "The operation could not be completed. Please try again in a moment.",
                    "Операция не была завершена. Пожалуйста, повторите попытку через некоторое время."),
            e(713, "Yalnız “Sənəd nömrəsi” və ya “Doğum tarixi” xanalarından biri doldurulmalıdır.",
                    "Only one of “Document Number” or “Date of Birth” should be filled.",
                    "Необходимо заполнить только одно из полей: «Номер документа» или «Дата рождения»."),
            e(714, "“Sənəd nömrəsi” və ya “Doğum tarixi” xanalarından ən azı biri doldurulmalıdır.",
                    "At least one of “Document Number” or “Date of Birth” must be filled.",
                    "Необходимо заполнить хотя бы одно из полей: «Номер документа» или «Дата рождения»."),
            e(722, "Daxil etdiyiniz FİN kod və ya sənəd nömrəsi ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code or document number you entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду или номеру документа. Пожалуйста, проверьте данные."),
            e(721, "Sənədinizlə bağlı foto məlumatı tapılmadı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "No photo information was found for your document. Please contact support.",
                    "Не найдена фотография, связанная с вашим документом. Пожалуйста, обратитесь в службу поддержки."),
            e(7072, "Daxil etdiyiniz FİN kod və ya sənəd nömrəsi ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code or document number entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду или номеру документа. Пожалуйста, проверьте данные."),
            e(7071, "Sənədinizlə bağlı foto tapılmadı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "No photo was found for your document. Please contact support.",
                    "Фотография для вашего документа не найдена. Пожалуйста, обратитесь в службу поддержки."),
            e(730, "Daxil etdiyiniz FİN kod ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду. Пожалуйста, проверьте данные."),
            e(731, "Seçdiyiniz sənəd növü üzrə uyğun sənəd tapılmadı. Zəhmət olmasa sənəd növünü yoxlayın.",
                    "No document was found for the selected document type. Please check the document type.",
                    "Не найден документ для выбранного типа документа. Пожалуйста, проверьте тип документа."),
            e(732, "Sənədinizlə bağlı foto məlumatı tapılmadı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "No photo information was found for your document. Please contact support.",
                    "Не найдена фотография, связанная с вашим документом. Пожалуйста, обратитесь в службу поддержки."),
            e(720, "Daxil etdiyiniz FİN kod ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду. Пожалуйста, проверьте данные."),
            e(761, "Bu sənəd məlumatları ilə istifadəçi artıq qeydiyyatdan keçib.",
                    "A user with this document information is already registered.",
                    "Пользователь с указанными данными документа уже зарегистрирован."),
            e(763, "Təqdim edilən şəklə uyğun istifadəçi tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No user matching the provided photo was found. Please check the details.",
                    "Пользователь, соответствующий предоставленному фото, не найден. Пожалуйста, проверьте данные."),
            e(765, "Göstərilən identifikator ilə istifadəçi tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No user was found with the provided identifier. Please check the details.",
                    "Пользователь с указанным идентификатором не найден. Пожалуйста, проверьте данные."),
            e(8004, "FİN kodunuza bağlı məlumat tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No information linked to your FIN code was found. Please check the details.",
                    "Информация, связанная с вашим FIN-кодом, не найдена. Пожалуйста, проверьте данные."),
            e(8006, "Ödəniş zamanı xəta baş verdi. Zəhmət olmasa bir az sonra yenidən cəhd edin.",
                    "An error occurred during payment. Please try again in a moment.",
                    "Во время оплаты произошла ошибка. Пожалуйста, повторите попытку через некоторое время."),
            e(8013, "Bu xidmət hazırda mümkün deyil. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "This service is currently unavailable. Please contact support.",
                    "Данный сервис в настоящее время недоступен. Пожалуйста, обратитесь в службу поддержки."),
            e(8032, "Sistemdə müvəqqəti nasazlıq yarandı. Zəhmət olmasa bir az sonra yenidən cəhd edin.",
                    "A temporary system issue occurred. Please try again in a moment.",
                    "Произошла временная неполадка в системе. Пожалуйста, повторите попытку через некоторое время."),
            e(8034, "Kartınız bu xidmətə qoşulmayıb. Zəhmət olmasa kartınızı qoşub yenidən cəhd edin.",
                    "Your card is not connected to this service. Please connect your card and try again.",
                    "Ваша карта не подключена к данному сервису. Пожалуйста, подключите карту и повторите попытку.")
    );

    private SimaKycErrorCatalog() {
    }

    public static String message(Integer simaCode, String acceptLanguage) {
        Tr row = BY_CODE.get(simaCode);
        if (row == null) {
            row = BY_CODE.get(70000);
        }
        return pick(row, acceptLanguage);
    }

    public static String dailyLimit(int limit, String acceptLanguage) {
        String key = langKey(acceptLanguage);
        if (key.startsWith("en")) {
            return "You have reached the daily attempt limit (" + limit
                    + " attempts). Please try again tomorrow.";
        }
        if (key.startsWith("ru")) {
            return "Вы достигли дневного лимита попыток (" + limit
                    + " попытки). Пожалуйста, повторите попытку завтра.";
        }
        return "Gündəlik cəhd sayı limiti (" + limit + " dəfə) aşılıb. Zəhmət olmasa sabah yenidən cəhd edin.";
    }

    public static String totalLimit(int limit, String acceptLanguage) {
        String key = langKey(acceptLanguage);
        if (key.startsWith("en")) {
            return "You have reached the total verification limit (" + limit
                    + " attempts) for your account. Please contact support.";
        }
        if (key.startsWith("ru")) {
            return "Вы достигли общего лимита проверок (" + limit
                    + " попыток) для вашего аккаунта. Пожалуйста, обратитесь в службу поддержки.";
        }
        return "Hesabınız üzrə ümumi yoxlama limiti (" + limit
                + " dəfə) aşılıb. Zəhmət olmasa dəstək xidmətinə müraciət edin.";
    }

    /** Score gate: liveness miss → 751, else similarity → 752. */
    public static int scoreGateCode(Double livenessScore, Double similarityScore) {
        if (livenessScore == null || livenessScore < SimaVerificationGate.SCORE_THRESHOLD) {
            return 751;
        }
        return 752;
    }

    private static Map.Entry<Integer, Tr> e(int code, String az, String en, String ru) {
        return Map.entry(code, new Tr(az, en, ru));
    }

    private static String pick(Tr row, String acceptLanguage) {
        String key = langKey(acceptLanguage);
        if (key.startsWith("en")) {
            return row.en;
        }
        if (key.startsWith("ru")) {
            return row.ru;
        }
        return row.az;
    }

    private static String langKey(String acceptLanguage) {
        return acceptLanguage == null ? "az" : acceptLanguage.toLowerCase();
    }
}
