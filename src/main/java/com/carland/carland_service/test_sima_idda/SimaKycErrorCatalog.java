package com.carland.carland_service.test_sima_idda;

import java.util.Map;

/**
 * CRCT-259: SIMA numeric codes → user AZ/EN/RU. Flutter shows {@code message}, not raw SIMA text.
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
            e(70002, "Daxil etdiyiniz məlumatlarda xəta var. Zəhmət olmasa yenidən yoxlayıb daxil edin.",
                    "There is an error in the information you entered. Please check and enter it again.",
                    "В введённых вами данных есть ошибка. Пожалуйста, проверьте и введите заново."),
            e(710, "Bu xidmət hazırda əlçatan deyil. Zəhmət olmasa daha sonra cəhd edin.",
                    "This service is currently unavailable. Please try again later.",
                    "Данный сервис в настоящее время недоступен. Пожалуйста, попробуйте позже."),
            e(750, "Şəxsiyyət yoxlanışı zamanı xəta baş verdi. Zəhmət olmasa yenidən cəhd edin.",
                    "An error occurred during identity verification. Please try again.",
                    "Произошла ошибка при проверке личности. Пожалуйста, повторите попытку."),
            e(7080, "Şəkil yüklənmədi. Zəhmət olmasa kameranın işlədiyinə əmin olub yenidən cəhd edin.",
                    "The image failed to upload. Please make sure your camera is working and try again.",
                    "Изображение не загрузилось. Пожалуйста, убедитесь, что камера работает, и повторите попытку."),
            e(7081, "Şəkil formatında problem var. Zəhmət olmasa yenidən çəkib göndərin.",
                    "There is a problem with the image format. Please retake and resend it.",
                    "Возникла проблема с форматом изображения. Пожалуйста, сделайте снимок заново и отправьте его."),
            e(7082, "Şəkil formatı dəstəklənmir. Zəhmət olmasa yenidən cəhd edin.",
                    "The image format is not supported. Please try again.",
                    "Формат изображения не поддерживается. Пожалуйста, повторите попытку."),
            e(751, "Canlılıq yoxlanışı uğursuz oldu. Zəhmət olmasa kameraya birbaşa baxaraq təkrar cəhd edin.",
                    "The liveness check was unsuccessful. Please look directly at the camera and try again.",
                    "Проверка на «живость» не пройдена. Пожалуйста, посмотрите прямо в камеру и повторите попытку."),
            e(752, "Şəxsiyyətinizi təsdiqləyə bilmədik. Zəhmət olmasa üzünüzün aydın görünməsinə diqqət edib yenidən cəhd edin.",
                    "We were unable to verify your identity. Please make sure your face is clearly visible and try again.",
                    "Нам не удалось подтвердить вашу личность. Пожалуйста, убедитесь, что лицо чётко видно, и повторите попытку."),
            e(7530, "Kadr çərçivəsində yalnız sizin üzünüzün olduğuna əmin olub yenidən cəhd edin.",
                    "Please make sure only your face is within the frame and try again.",
                    "Пожалуйста, убедитесь, что в кадре только ваше лицо, и повторите попытку."),
            e(716, "Əməliyyat tamamlanmadı. Zəhmət olmasa bir az sonra yenidən cəhd edin.",
                    "The operation could not be completed. Please try again in a moment.",
                    "Операция не была завершена. Пожалуйста, повторите попытку через некоторое время."),
            e(713, "Yalnız “Sənəd nömrəsi” və ya “Doğum tarixi” xanalarından biri doldurulmalıdır.",
                    "Only one of “Document Number” or “Date of Birth” should be filled.",
                    "Необходимо заполнить только одно из полей: «Номер документа» или «Дата рождения»."),
            e(714, "“Sənəd nömrəsi” və ya “Doğum tarixi” xanalarından ən azı biri doldurulmalıdır.",
                    "At least one of “Document Number” or “Date of Birth” must be filled.",
                    "Необходимо заполнить хотя бы одно из полей: «Номер документа» или «Дата рождения»."),
            e(722, "Daxil etdiyiniz FIN kod və ya sənəd nömrəsi ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code or document number you entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду или номеру документа. Пожалуйста, проверьте данные."),
            e(721, "Sənədinizlə bağlı foto məlumatı tapılmadı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "No photo information was found for your document. Please contact support.",
                    "Не найдена фотография, связанная с вашим документом. Пожалуйста, обратитесь в службу поддержки."),
            e(7072, "Daxil etdiyiniz FIN kod və ya sənəd nömrəsi ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code or document number entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду или номеру документа. Пожалуйста, проверьте данные."),
            e(7071, "Sənədinizlə bağlı foto məlumatı tapılmadı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "No photo information was found for your document. Please contact support.",
                    "Не найдена фотография, связанная с вашим документом. Пожалуйста, обратитесь в службу поддержки."),
            e(730, "Daxil etdiyiniz FIN kod ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду. Пожалуйста, проверьте данные."),
            e(731, "Seçdiyiniz sənəd növü üzrə uyğun sənəd tapılmadı. Zəhmət olmasa sənəd növünü yoxlayın.",
                    "No document was found for the selected document type. Please check the document type.",
                    "Не найден документ для выбранного типа документа. Пожалуйста, проверьте тип документа."),
            e(732, "Sənədinizlə bağlı foto məlumatı tapılmadı. Zəhmət olmasa dəstək xidmətinə müraciət edin.",
                    "No photo information was found for your document. Please contact support.",
                    "Не найдена фотография, связанная с вашим документом. Пожалуйста, обратитесь в службу поддержки."),
            e(720, "Daxil etdiyiniz FIN kod ilə uyğun aktiv sənəd tapılmadı. Zəhmət olmasa məlumatları yoxlayın.",
                    "No active document was found matching the FIN code entered. Please check the details.",
                    "Не найден активный документ, соответствующий введённому FIN-коду. Пожалуйста, проверьте данные."),
            e(761, "Bu sənəd məlumatları ilə istifadəçi artıq qeydiyyatdan keçib.",
                    "A person with this document information is already registered.",
                    "Пользователь с указанными данными документа уже зарегистрирован."),
            e(763, "Təqdim edilən şəklə uyğun istifadəçi tapılmadı.",
                    "No user matching the provided photo was found.",
                    "Пользователь, соответствующий предоставленному фото, не найден."),
            e(765, "Göstərilən identifikator ilə istifadəçi tapılmadı.",
                    "No user was found with the provided identifier.",
                    "Пользователь с указанным идентификатором не найден.")
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
