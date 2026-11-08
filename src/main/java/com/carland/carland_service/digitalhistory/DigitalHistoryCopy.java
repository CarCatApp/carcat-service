package com.carland.carland_service.digitalhistory;

import java.util.List;

/**
 * tr: PDF və yoxlama səhifəsinin sabit mətnləri. Dil buraxılış anında seçilir.
 * en: Fixed copy for the PDF and the verify page. The language is chosen when the report is issued.
 */
final class DigitalHistoryCopy {

    private final String lang;

    private DigitalHistoryCopy(String lang) {
        this.lang = lang == null ? "az" : lang;
    }

    static DigitalHistoryCopy of(String lang) {
        return new DigitalHistoryCopy(DigitalHistoryLang.normalize(lang));
    }

    String lang() {
        return lang;
    }

    String documentTitle() {
        return switch (lang) {
            case "en" -> "CarCat — Digital history report";
            case "ru" -> "CarCat — Отчёт цифровой истории";
            default -> "CarCat — Rəqəmsal tarixçə hesabatı";
        };
    }

    String title() {
        return switch (lang) {
            case "en" -> "Digital history report";
            case "ru" -> "Отчёт цифровой истории";
            default -> "Rəqəmsal tarixçə hesabatı";
        };
    }

    String issued() {
        return switch (lang) {
            case "en" -> "Issued";
            case "ru" -> "Выпущено";
            default -> "Buraxılıb";
        };
    }

    String mileage() {
        return switch (lang) {
            case "en" -> "Current mileage";
            case "ru" -> "Текущий пробег";
            default -> "Cari yürüş";
        };
    }

    String mileageOn(String day) {
        if (day == null || day.isBlank()) {
            return "";
        }
        return switch (lang) {
            case "en" -> "on " + day;
            case "ru" -> day;
            default -> day + " tarixində";
        };
    }

    String serviceRecords() {
        return switch (lang) {
            case "en" -> "Service records";
            case "ru" -> "Записи сервиса";
            default -> "Servis qeydi";
        };
    }

    String centers() {
        return switch (lang) {
            case "en" -> "Service centers";
            case "ru" -> "Сервисные центры";
            default -> "Servis mərkəzi";
        };
    }

    String technical() {
        return switch (lang) {
            case "en" -> "Technical details";
            case "ru" -> "Технические данные";
            default -> "Texniki məlumatlar";
        };
    }

    String certificateNumber() {
        return switch (lang) {
            case "en" -> "Registration certificate number";
            case "ru" -> "Номер регистрационного свидетельства";
            default -> "Qeydiyyat şəhadətnaməsinin nömrəsi";
        };
    }

    String certificateDate() {
        return switch (lang) {
            case "en" -> "Certificate issue date";
            case "ru" -> "Дата выдачи свидетельства";
            default -> "Şəhadətnamənin verilmə tarixi";
        };
    }

    String plate() {
        return switch (lang) {
            case "en" -> "Registration plate";
            case "ru" -> "Регистрационный номер";
            default -> "Qeydiyyat nişanı";
        };
    }

    String brandModel() {
        return switch (lang) {
            case "en" -> "Make and model";
            case "ru" -> "Марка и модель";
            default -> "Marka və model";
        };
    }

    String vin() {
        return switch (lang) {
            case "en" -> "VIN";
            case "ru" -> "VIN";
            default -> "Ban nömrəsi (VIN)";
        };
    }

    String year() {
        return switch (lang) {
            case "en" -> "Year of manufacture";
            case "ru" -> "Год выпуска";
            default -> "Buraxılış ili";
        };
    }

    String color() {
        return switch (lang) {
            case "en" -> "Colour";
            case "ru" -> "Цвет";
            default -> "Rəngi";
        };
    }

    String volume() {
        return switch (lang) {
            case "en" -> "Engine capacity";
            case "ru" -> "Объём двигателя";
            default -> "Mühərrik həcmi";
        };
    }

    String engineNumber() {
        return switch (lang) {
            case "en" -> "Engine number";
            case "ru" -> "Номер двигателя";
            default -> "Mühərrik nömrəsi";
        };
    }

    String bodyType() {
        return switch (lang) {
            case "en" -> "Type";
            case "ru" -> "Тип";
            default -> "Növü";
        };
    }

    String manualSentence(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        String list = join(fields);
        if ("en".equals(lang)) {
            String verb = fields.size() == 1 ? "was entered" : "were entered";
            return list + " " + verb + " manually by the user.";
        }
        if ("ru".equals(lang)) {
            String verb = fields.size() == 1 ? "указано" : "указаны";
            return list + " " + verb + " пользователем вручную.";
        }
        return list + " istifadəçi tərəfindən manual daxil edilib.";
    }

    String chart() {
        return switch (lang) {
            case "en" -> "Mileage trend";
            case "ru" -> "Динамика пробега";
            default -> "Yürüş dinamikası";
        };
    }

    String average(String perMonth) {
        return switch (lang) {
            case "en" -> "Average mileage: ~ " + perMonth + " km / month";
            case "ru" -> "Средний пробег: ~ " + perMonth + " км / мес";
            default -> "Orta yürüş: ~ " + perMonth + " km / ay";
        };
    }

    String history() {
        return switch (lang) {
            case "en" -> "Service history";
            case "ru" -> "История сервиса";
            default -> "Servis tarixçəsi";
        };
    }

    String historyNote() {
        return switch (lang) {
            case "en" -> "Records run from newest to oldest — the latest visit is at the top.";
            case "ru" -> "Записи идут от новых к старым — последнее посещение сверху.";
            default -> "Qeydlər ən yenidən ən köhnəyə doğru sıralanıb – son ziyarət yuxarıdadır.";
        };
    }

    String empty() {
        return switch (lang) {
            case "en" -> "No records";
            case "ru" -> "Нет записей";
            default -> "Qeyd yoxdur";
        };
    }

    String lineTotal() {
        return switch (lang) {
            case "en" -> "Amount";
            case "ru" -> "Сумма";
            default -> "Ümumi məbləğ";
        };
    }

    String grandTotal() {
        return switch (lang) {
            case "en" -> "Total service cost";
            case "ru" -> "Общая стоимость обслуживания";
            default -> "Ümumi servis xərci";
        };
    }

    String centersTitle() {
        return switch (lang) {
            case "en" -> "Service centers that worked on this vehicle";
            case "ru" -> "Сервисные центры, обслуживавшие автомобиль";
            default -> "Xidmət göstərən servis mərkəzləri";
        };
    }

    String visitWord() {
        return switch (lang) {
            case "en" -> "visits";
            case "ru" -> "визитов";
            default -> "ziyarət";
        };
    }

    String spent() {
        return switch (lang) {
            case "en" -> "total cost";
            case "ru" -> "общая сумма";
            default -> "ümumi xərc";
        };
    }

    String centerFallback() {
        return switch (lang) {
            case "en" -> "Service center";
            case "ru" -> "Сервисный центр";
            default -> "Servis mərkəzi";
        };
    }

    String verifyTitle() {
        return switch (lang) {
            case "en" -> "Check that this report is original";
            case "ru" -> "Проверьте подлинность отчёта";
            default -> "Hesabatın orijinallığını yoxlayın";
        };
    }

    String verifyLead() {
        return switch (lang) {
            case "en" -> "Scan the QR code or open:";
            case "ru" -> "Отсканируйте QR-код или откройте:";
            default -> "QR kodu skan edin və ya daxil olun:";
        };
    }

    String verifyBody() {
        return switch (lang) {
            case "en" -> "This report is compiled only from data passed to the CarCat platform. "
                    + "Some events for this vehicle may not have been reported to CarCat. "
                    + "Use the report together with a technical inspection, as an aid to your decision. "
                    + "(c) 2026 CarCat MMC.";
            case "ru" -> "Этот отчёт составлен только по данным, переданным на платформу CarCat. "
                    + "Некоторые события по этому автомобилю могли не быть сообщены CarCat. "
                    + "Используйте отчёт вместе с техническим осмотром как помощь при решении. "
                    + "(c) 2026 CarCat MMC.";
            default -> "Bu hesabat yalnız CarCat platformasına ötürülmüş məlumatlar əsasında tərtib olunub. "
                    + "Bu avtomobillə bağlı bəzi hadisələr CarCat-a bildirilməmiş ola bilər. "
                    + "Hesabatdan avtomobilin texniki baxışı ilə yanaşı, qərarvermə üçün köməkçi vasitə kimi istifadə edin. "
                    + "(c) 2026 CarCat MMC.";
        };
    }

    String footer(String code) {
        return "CarCat · " + title() + " · " + code;
    }

    String page(int number, int total) {
        return switch (lang) {
            case "en" -> "Page " + number + " / " + total;
            case "ru" -> "Страница " + number + " / " + total;
            default -> "Səhifə " + number + " / " + total;
        };
    }

    String missing() {
        return switch (lang) {
            case "en" -> "Report not found";
            case "ru" -> "Отчёт не найден";
            default -> "Hesabat tapılmadı";
        };
    }

    String openPdf() {
        return switch (lang) {
            case "en" -> "Open the PDF";
            case "ru" -> "Открыть PDF";
            default -> "PDF-i aç";
        };
    }

    private String join(List<String> fields) {
        String and = switch (lang) {
            case "en" -> "and";
            case "ru" -> "и";
            default -> "və";
        };
        if (fields.size() == 1) {
            return fields.get(0);
        }
        StringBuilder list = new StringBuilder(fields.get(0));
        for (int i = 1; i < fields.size(); i++) {
            String word = lowerFirst(fields.get(i));
            if (i == fields.size() - 1) {
                list.append(' ').append(and).append(' ').append(word);
            } else {
                list.append(", ").append(word);
            }
        }
        return list.toString();
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int first = value.codePointAt(0);
        return new String(Character.toChars(Character.toLowerCase(first))) + value.substring(Character.charCount(first));
    }
}
