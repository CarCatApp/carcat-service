package com.carland.carland_service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * tr: Uygulama genelinde kullanılan hata/bilgi mesajlarının Azerice/İngilizce/Rusça karşılıklarını tutan enum; dile göre mesaj döndürür.
 * en: Enum holding Azerbaijani/English/Russian versions of error/info messages used across the app; returns the message by language.
 */
@Getter
@AllArgsConstructor
public enum MessagesLangValues {
    PLACE_NOT_FOUND(
            "Bölgə tapılmadı",
            "Place not found",
            "DJJD"),

    MISSING_BODY(
            "Məlumatlar əksikdir!",
            "Missing body!",
            "Данные отсутствуют!"
    ),
    PDF_NOT_FOUND(
            "Pdf fayl tapılmadı",
            "PDF file not found",
            "PDF файл не найден"
    ),

    USER_ALREADY_INVITED(
            "İstifadəçi dəvət edilib",
            "The user has already been invited",
            "Пользователь уже был приглашен"
    ),

    HOSPITAL_ALREADY_EXISTS(
            "xəstəxana artiq mövcuddur",
            "Hospital already exists",
            "Больница уже существует"
    ),

    ONLY_DOCTOR_ALLOWED(
            "Yalnız həkim seçə bilər",
            "Only doctor allowed",
            "Только для врача"
    ),

    RANGE_NOT_FOUND(
            "Zaman periodu tapılmadı",
            "Time slot not found",
            "Временной интервал не найден"
    ),

    ALREADY_BOOKED(
            "Sifariş edilib",
            "Already booked",
            "Уже забронировано"
    ),

    BREAK_TIME(
            "Fasilə nəzərdə tutulub",
            "Break time scheduled",
            "Запланирован перерыв"
    ),

    BRANCH_NAME_EMPTY(
            "Şöbə adı boşdur",
            "Branch name is empty",
            "Название отделения пусто"
    ),

    TEMPLATE_ALREADY_EXISTS(
            "Şablon artıq mövcuddur",
            "Template already exists",
            "Шаблон уже существует"
    ),

    SERVICE_ALREADY_EXISTS(
            "Bu servis artıq şablonda mövcuddur",
            "This service already exists in the template",
            "Эта услуга уже существует в шаблоне"
    ),

    USER_NOT_ACTIVE(
            "Istifadeci aktiv deyil",
            "User is not active",
            "Пользователь не активен"
    ),

    PAST_DATE_NOT_ALLOWED(
            "Keçmiş tarix seçilə bilməz",
            "Past date is not allowed",
            "Выбор прошедшей даты невозможен"
    ),

    START_AFTER_END(
            "Başlama bitişdən sonra ola bilməz",
            "Start time cannot be after end time",
            "Время начала не может быть после времени окончания"
    ),

    SUCCESS(
            "Uğurla tamamlandı",
            "Success",
            "Успешно"
    ),

    START_TIME_ALREADY_PASSED(
            "Başlama saatı keçmişdir",
            "Start time has already passed",
            "Время начала уже прошло"
    ),

    INVALID_RANGE_MINUTES(
            "Zaman vahidi düzgün deyil",
            "Invalid range minutes",
            "Неверная длительность интервала"
    ),

    APPOINTMENT_NOT_FOUND(
            "Görüş tapılmadı",
            "Appointment not found",
            "Встреча не найдена"
    ),

    MISSING_FIELDS(
            "Mobil nömrə, ad, soyad, doğum tarixi boş ola bilməz",
            "Mobile number, name, surname, and birth date cannot be empty",
            "Номер телефона, имя, фамилия и дата рождения не могут быть пустыми"
    ),

    MISSING_USER_FIELDS(
            "İstifadəçi adı, ID və şifrə boş ola bilməz",
            "Username, ID and password cannot be empty",
            "Имя пользователя, ID и пароль не могут быть пустыми"
    ),

    CALENDAR_NOT_FOUND(
            "Təqvim tapılmadı",
            "Calendar not found",
            "Календарь не найден"
    ),

    CALENDAR_EMPTY(
            "Təqvim boşdur",
            "Calendar is empty",
            "Календарь пуст"
    ),

    USER_NOT_FOUND(
            "İstifadəçi mövcud deyil",
            "User not found",
            "Пользователь не найден"
    ),

    CAR_ALREADY_EXISTS(
            "Bu vin kod ilə avtomobil mövcuddur",
            "A car with this VIN code already exists",
            "Автомобиль с таким VIN-кодом уже существует"
    ),

    PLATE_NUMBER_ALREADY_EXISTS(
            "Bu qeydiyyat nişanı artıq mövcuddur",
            "This license plate number already exists",
            "Этот регистрационный номер уже существует"
    ),

    APPOINTMENT_STATUS_ALREADY_SET(
            "Görüş statusu təyin edilib",
            "Appointment status already set",
            "Статус встречи уже установлен"
    ),

    APPOINTMENT_NOT_FOR_DOCTOR(
            "Görüş istəyi sizə aid deyil",
            "Appointment request not for you",
            "Запрос встречи не для вас"
    ),

    APPOINTMENT_ACCEPTED(
            "Qəbul edildi",
            "Accepted",
            "Принято"
    ),

    APPOINTMENT_REJECTED(
            "Rədd edildi",
            "Rejected",
            "Отклонено"
    ),

    APPOINTMENT_DATE_PASSED(
            "Görüş tarixi keçib",
            "Appointment date has passed",
            "Дата встречи прошла"
    ),

    APPOINTMENT_NOT_BOOKED(
            "Görüş üçün sifariş yoxdur",
            "No booking for appointment",
            "Нет бронирования для встречи"
    ),

    APPOINTMENT_DATA_NOT_FOUND(
            "Görüş üçün məlumat yoxdur",
            "Appointment data not found",
            "Данные встречи не найдены"
    ),

    MISSING_ID(
            "ID boş ola bilməz",
            "ID cannot be empty",
            "ID не может быть пустым"
    ),

    MISSING_PHONE_NUMBER(
            "Telefon nömrəsi boş ola bilməz",
            "Phone number cannot be empty",
            "Номер телефона не может быть пустым"
    ),

    INVALID_ROLE_PERMISSION(
            "Əməliyyat üçün səlahiyyətiniz yoxdur!",
            "You dont have permission for operation!",
            "У вас нет прав для операции!"
    ),

    USER_ALREADY_ACTIVE(
            "İstifadəçi aktivdir",
            "User is already active",
            "Пользователь уже активен"
    ),

    INVITE_NOT_FOUND(
            "Dəvət mövcud deyil",
            "Invite does not exist",
            "Приглашение не существует"
    ),

    DOCTOR_NOT_FOUND(
            "Həkim tapılmadı",
            "Doctor not found",
            "Доктор не найден"
    ),

    HOSPITAL_NOT_FOUND(
            "Xəstəxana tapılmadı",
            "Hospital not found",
            "Больница не найдена"
    ),

    HOSPITAL_SET_ERROR(
            "Hospital servis xəta verdi",
            "Hospital service error",
            "Ошибка сервиса больницы"
    ),

    HISTORY_NOT_BELONG_TO_USER(
            "Tarixçə sizə aid deyil",
            "History does not belong to you",
            "История не принадлежит вам"
    ),

    BRANCH_NOT_FOUND(
            "Şöbə tapılmadı",
            "Branch not found",
            "Отделение не найдено"
    ),

    INVALID_ROLE(
            "Yanlış səlahiyyət",
            "Invalid role",
            "Неверная роль"
    ),

    DELETE_TIME_EXPIRED(
            "Son silmə vaxtı keçib",
            "Delete time expired",
            "Время удаления истекло"
    ),

    SITUATION_ALREADY_EXISTS(
            "Bu status artıq mövcuddur",
            "This situation already exists",
            "Этот статус уже существует"
    ),

    DATA_SAVED_SUCCESSFULLY(
            "Məlumatlar yadda saxlanıldı",
            "Data saved successfully",
            "Данные успешно сохранены"
    ),

    CUSTOMER_INFORMATION_MISSING(
            "Ad və FIN kodu boş ola bilməz",
            "Name and FIN code cannot be empty",
            "Имя и код FIN не могут быть пустыми"
    ),

    NAME_TOO_SHORT(
            "Ad ən azı 3 simvol olmalıdır",
            "Name must be at least 3 characters",
            "Имя должно содержать не менее 3 символов"
    ),

    INVALID_MAIL(
            "E-poçt ünvanı düzgün deyil",
            "Invalid e-mail address",
            "Неверный адрес электронной почты"
    ),

    INVALID_PIN(
            "FIN kodu düzgün deyil",
            "Invalid FIN code",
            "Неверный код FIN"
    ),

    MAIL_ALREADY_EXISTS(
            "Bu e-poçt ünvanı artıq istifadə olunur",
            "This e-mail address is already in use",
            "Этот адрес электронной почты уже используется"
    ),

    PIN_ALREADY_EXISTS(
            "Bu FIN kodu artıq istifadə olunur",
            "This FIN code is already in use",
            "Этот код FIN уже используется"
    ),

    INVALID_PHOTO_NAME(
            "Şəkil adı qəbul edilmədi",
            "Invalid photo name",
            "Недопустимое название фото"),

    INVALID_PHOTO_FORMAT(
            "Şəkil formatı qəbul edilmədi",
            "Invalid photo format",
            "Недопустимый формат фото"),

    FILE_CANT_SET(
            "Fayl yadda saxlanılmadı",
            "File could not be saved",
            "Не удалось сохранить фaйл"),

    TEST_NOT_FOUND(
            "Analiz cavabı tapılmadı",
            "Test result not found",
            "Результат анализа не найден"),

    PHOTO_NOT_FOUND(
            "Profil şəkli tapılmadı",
            "Profile photo not found",
            "Фотография профиля не найдена"),

    DEVICE_TOKEN_NOT_FOUND(
            "Device token tapılmadı",
            "Device token not found",
            "Токен устройства не найден"
    ),

    BOOKING_REQUEST_TITLE(
            "Görüş istəyi",
            "Appointment Request",
            "Запрос на прием"
    ),

    BOOKING_REACT_INFO(
            "Görüş istəyi qəbulu",
            "Appointment Response",
            "Ответ на запись"),

    BOOKING_NOTIFICATION_ERROR(
            "Bildiriş göndərmə xətası",
            "Notification sending error",
            "Ошибка при отправке уведомления"),

    BOOKING_CANCEL_BY_PATIENT(
            "Pasiyent görüşü ləğv etdi",
            "Patient cancelled the appointment",
            "Пациент отменил запись"
    ),

    APPOINTMENT_DELETED_BY_PATIENT(
            "Görüş sifarişçi tərəfindən silinib",
            "Appointment deleted by patient",
            "Встреча удалена пациентом"
    ),

    APPOINTMENT_REJECTED_BY_DOCTOR(
            "Görüş həkim tərəfindən silinib",
            "Appointment rejected by doctor",
            "Встреча отклонена врачом"
    ),

    DOCTOR_AT_WORK(
            "İşdədir",
            "At work",
            "На работе"
    ),

    DOCTOR_NOT_AT_WORK(
            "İşdə deyil",
            "Not at work",
            "Не на работе"
    ),

    CAR_NOT_MATCH_WITH_CUSTOMER(
            "Avtomobil bu istifadəçiyə aid deyil",
            "The car does not belong to this user",
            "Автомобиль не принадлежит этому пользователю"),

    CAR_NOT_FOUND(
            "Avtomobil tapılmadı",
            "Car not found",
            "Автомобиль не найден"),

    TEMPLATE_NOT_FOUND(
            "Şablon tapılmadı",
            "Template not found",
            "Шаблон не найден"),

    AUTO_SERVICE_NOT_FOUND(
            "Avto servis tapılmadı",
            "Auto Service not found",
            "Автоcepвиc не найден"),

    SERVICE_NOT_FOUND(
            "Xidmət tapılmadı",
            "Service not found",
            "Yслуга не найден"),

    RECORD_NOT_FOUND(
            "Son xidmət tapılmadı",
            "Last service not found",
            "Последняя услуга не найдена"),

    RECORD_ALREADY_EXISTS(
            "Son xidmət mövcuddur",
            "Last service already exists",
            "Последняя услуга уже существует"),

    SERVICE_AUTO_MECHANIC(
            "Avto mexanik",
            "Auto Mechanic",
            "Автомеханик"),

    SERVICE_AUTOMOTIVE_TECHNICIAN(
            "Avtotexnik",
            "Automotive Technician",
            "Автотехник"),

    SERVICE_AUTO_ELECTRICIAN(
            "Avtoelektrik",
            "Auto Electrician",
            "Автоэлектрик"),

    SERVICE_CAR_PAINTER(
            "Boyaçı",
            "Car Painter",
            "Автомаляр"),

    SERVICE_BODY_REPAIR_TECHNICIAN(
            "Kuzov ustası",
            "Auto Body Repair Technician",
            "Кузовщик"),

    SERVICE_TIRE_TECHNICIAN(
            "Şin ustası",
            "Tire Technician",
            "Шиномонтажник"),

    SERVICE_LUBE_TECHNICIAN(
            "Yağ ustası",
            "Lube Technician",
            "Мастер по замене масла"),

    SERVICE_DIAGNOSTIC_TECHNICIAN(
            "Diaqnostika mütəxəssisi",
            "Diagnostic Technician",
            "Диагност"),

    AUTO_SERVICE_ALREADY_EXISTS(
            "Avto servis mövcuddur",
            "Auto Service already exists",
            "Автоcepвиc уже существует"),

    PARTNER_NOT_FOUND(
            "Partner tapılmadı",
            "Partner not found",
            "Партнер не найден"),

    PARTNER_ALREADY_EXISTS(
            "Partner mövcuddur",
            "Partner already exists",
            "Партнер уже существует"),

    CALENDAR_ALREADY_EXISTS(
            "Seçilən günə cədvəl mövcuddur",
            "Calendar already exists for selected day",
            "Расписание уже существует на выбранный день"
    ),

    ALREADY_BOOKED_SAME_DAY(
            "Eyni anda ikisini verme qaqas",
            "",
            ""),

    BRAND_NOT_FOUND("Marka tapılmadı",
            "Brand not found",
            "Марка не найдена"),

    MODEL_NOT_FOUND("Model tapılmadı",
            "Model not found",
            "Moдел не найдена"),

    NOTIFICATION_NOT_FOUND(
            "Bildiriş tapılmadı",
            "Notification not found",
            "Notification не найдена"),

    COLOR_NOT_FOUND("Rəng tapılmadı",
            "Color not found",
            "Color not found"),


    BODY_TYPE_NOT_FOUND(
            "tapılmadı",
            "tapılmadı",
            "tapılmadı"),


    TRANSMISSION_TYPE_NOT_FOUND(
            "tapılmadı",
            "tapılmadı",
            "tapılmadı"),


    ENGINE_TYPE_NOT_FOUND(
            "tapılmadı",
            "tapılmadı",
            "tapılmadı"),

    MODEL_YEAR_NOT_FOUND(
            "tapılmadı",
            "tapılmadı",
            "tapılmadı"),


    CAR_PHOTO_NOT_FOUND(
            "Avtomobil şəkli tapılmadı",
            "Car photo not found",
            "Автомобиль фото не найден"
    ),

    SIMA_ALREADY_VERIFIED(
            "İstifadəçi artıq SIMA ilə təsdiqlənib",
            "User is already SIMA verified",
            "Пользователь уже подтверждён через SIMA"
    ),

    SIMA_PROFILE_LOCKED(
            "Təsdiqlənmiş ad, soyad və FIN dəyişdirilə bilməz",
            "Verified name, surname and FIN cannot be changed",
            "Подтверждённые имя, фамилия и FIN нельзя изменить"
    ),

    PHOTO_AI_PREPARING(
            "Şəkiliniz hazırlanır",
            "Your photo is being prepared",
            "Ваше фото готовится"
    ),

    PHOTO_AI_UPLOAD_BLOCKED(
            "Fotonuz AI tərəfindən hazırlanır, bir neçə saniyə gözləyin",
            "Your photo is being prepared by AI, please wait a few seconds",
            "Фото готовится с помощью ИИ, подождите несколько секунд"
    ),

    PHOTO_AI_FAILED(
            "Foto hazırlanmadı. Bir az sonra yenidən cəhd edin",
            "The photo could not be created. Please try again later",
            "Не удалось создать фото. Попробуйте позже"
    ),

    PHOTO_AI_GENERATE_LIMIT(
            "Bu avtomobil üçün AI foto limiti bitib (3)",
            "AI photo limit reached for this car (3)",
            "Лимит AI-фото для этого автомобиля исчерпан (3)"
    );

    private final String azMessage;
    private final String enMessage;
    private final String ruMessage;

    public String getMessageByLang(String lang) {
        if (lang == null) return azMessage;
        String key = lang.toLowerCase();
        if (key.startsWith("en")) return enMessage;
        if (key.startsWith("ru")) return ruMessage;
        return azMessage;
    }
}
