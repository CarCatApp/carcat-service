package com.carland.carland_service.service.impl;


import com.carland.carland_service.dto.response.AppointmentResponse;
import com.carland.carland_service.entity.Appointment;
import com.carland.carland_service.entity.AutoService;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.AppointmentStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.exceptions.*;
import com.carland.carland_service.repository.AppointmentRepository;
import com.carland.carland_service.repository.AutoServiceRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.RangeRepository;
import com.carland.carland_service.service.PushNotificationService;
import com.carland.carland_service.service.impl.Helper;
import com.carland.carland_service.dto.response.RangeResponse;
import com.carland.carland_service.service.RangeService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

/**
 * tr: Takvimdeki zaman aralıkları (Range) üzerinden randevu işlemlerini yöneten servis; müşteri adına randevu alma (booking) işlemini yapar. Randevu onaylama/reddetme ve müşteri iptali metodları şu an devre dışıdır (null döner, eski gövdeleri yorum satırındadır).
 * en: Service managing appointment operations over calendar time slots (Range); books an appointment on behalf of a customer. The booking decision and customer cancellation methods are currently disabled (they return null; their old bodies are commented out).
 */
@Service
@RequiredArgsConstructor
public class RangeServiceImpl implements RangeService {

    private final RangeRepository rangeRepository;
    private final CustomerRepository customerRepository;
    private final AutoServiceRepository autoServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final Helper helper;
    private final PushNotificationService pushNotificationService;


//    @Value("${appointment.last.delete.time}")
//    private Long lastDeleteHours;

    /**
     * tr: Verilen zaman aralığı (rangeId) için müşteri adına randevu oluşturur. USER rolü şarttır (InvalidStatusException), parametre eksikse MissingFieldException, müşteri yoksa UserNotFoundException, aralık yoksa ResourceNotFoundException; aralık dolu/PENDING ise veya müşterinin aynı gün aynı kategoride randevusu varsa AlreadyExistsException, mola (BREAK) ise ResourceNotFoundException fırlatır. Kapasite dolarsa aralığı FULL yapar ve randevu detayıyla RangeResponse döner.
     * en: Books an appointment for the customer on the given time slot (rangeId). Requires the USER role (InvalidStatusException); throws MissingFieldException for missing parameters, UserNotFoundException if the customer is not found, ResourceNotFoundException if the range does not exist, AlreadyExistsException if the range is full/PENDING or the customer already has a same-day appointment in the same category, and ResourceNotFoundException for BREAK slots. Marks the range FULL when capacity is reached and returns a RangeResponse with the appointment details.
     */
    @Override
    @Transactional
    public RangeResponse bookAppointment(Long rangeId, String role, String phoneNumber,
                                         String userIdHeader, String timezone, String acceptLanguage) {

        if (rangeId == null || role == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        if (!role.equals(UserRoles.USER.name())) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumber(Long.valueOf(userIdHeader), phoneNumber);
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Range range = rangeRepository.findByRangeId(rangeId);
        if (range == null) {
            throw new ResourceNotFoundException(MessagesLangValues.RANGE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        if (range.getStatus().equals(RangeStatus.PENDING.name()) ||
                range.getAppointments().size() >= range.getWorkerCount()) {
            throw new AlreadyExistsException(MessagesLangValues.ALREADY_BOOKED.getMessageByLang(acceptLanguage));
        }
        if (range.getStatus().equals(RangeStatus.BREAK.name())) {
            throw new ResourceNotFoundException(MessagesLangValues.BREAK_TIME.getMessageByLang(acceptLanguage));
        }

        AutoService autoService = autoServiceRepository.findById(range.getCalendar().getAutoService().getId())
                .orElseThrow(() -> new UserNotFoundException(MessagesLangValues.DOCTOR_NOT_FOUND.getMessageByLang(acceptLanguage)));

        OffsetDateTime dayStart = range.getStart().toLocalDate().atStartOfDay().atOffset(range.getStart().getOffset());
        OffsetDateTime dayEnd = range.getStart().toLocalDate().atTime(23, 59, 59).atOffset(range.getStart().getOffset());

        Optional<Appointment> existing = appointmentRepository
                .findByCustomer_UserIdAndServiceCategoryAndAppointmentDateBetweenAndRange_Calendar_AutoService_Id(
                        customer.getUserId(),
                        range.getCalendar().getServiceCategory(),
                        dayStart,
                        dayEnd,
                        range.getCalendar().getAutoService().getId()
                );


        if(existing.isPresent()){
            throw new AlreadyExistsException(MessagesLangValues.ALREADY_BOOKED_SAME_DAY.getMessageByLang(acceptLanguage));
        }


        OffsetDateTime appointmentDateUtc = range.getStart();

        Appointment appointment = Appointment.builder()
                .appointmentDate(appointmentDateUtc)
                .appointmentStart(range.getStart())
                .appointmentEnd(range.getEnd())
                .autoService(autoService)
                .range(range)
                .serviceCategory(range.getCalendar().getServiceCategory())
                .customer(customer)
                .status(AppointmentStatus.PENDING.name())
                .build();
        appointmentRepository.save(appointment);

        range.getAppointments().add(appointment);

        if (range.getAppointments().size() >= range.getWorkerCount()) {
            range.setStatus(RangeStatus.FULL.name());
        }


        rangeRepository.save(range);

        return RangeResponse.builder()
                .rangeId(range.getRangeId())
                .start(helper.getLocalTimeFromUtcUseTZ(range.getStart(), timezone))
                .end(helper.getLocalTimeFromUtcUseTZ(range.getEnd(), timezone))
                .status(range.getStatus())
                .appointmentResponses(List.of(convertToResponse(appointment, timezone, acceptLanguage)))
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .freeCount(range.getWorkerCount() - range.getAppointments().size())
                .build();
    }

    /**
     * tr: Randevu talebini kabul/ret etme metodu; şu an devre dışıdır ve her zaman null döner (eski implementasyon aşağıda yorum satırındadır).
     * en: Method for accepting/rejecting a booking request; currently disabled and always returns null (the old implementation is commented out below).
     */
    @Override
    public RangeResponse decideOnBooking(Long rangeId, boolean accepted, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        return null;
    }


//    @Override
//    @Transactional
//    public RangeResponse decideOnBooking(Long rangeId, boolean accepted, String role, String phoneNumber,
//                                         String userIdHeader, String timezone, String acceptLanguage) {
//
//        if (role == null || !role.equals(UserRoles.DOCTOR.name())) {
//            throw new InvalidStatusException(MessagesLangValues.ONLY_DOCTOR_ALLOWED.getMessageByLang(acceptLanguage));
//        }
//        if (rangeId == null || phoneNumber == null || userIdHeader == null) {
//            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
//        }
//
//        Doctor doctor = doctorRepository.findByUserIdAndPhoneNumberAndStatus(
//                Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());
//        if (doctor == null) {
//            throw new UserNotFoundException(MessagesLangValues.DOCTOR_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//        Range range = rangeRepository.findByRangeId(rangeId);
//        if (range == null) {
//            throw new ResourceNotFoundException(MessagesLangValues.APPOINTMENT_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//        Patient patient = range.getAppointment().getPatient();
//
//        if (!range.getStatus().equals(RangeStatus.PENDING.name())) {
//            throw new InvalidStatusException(MessagesLangValues.APPOINTMENT_STATUS_ALREADY_SET.getMessageByLang(acceptLanguage));
//        }
//
//        if (!range.getCalendar().getDoctor().getUserId().equals(doctor.getUserId())) {
//            throw new InvalidStatusException(MessagesLangValues.APPOINTMENT_NOT_FOR_DOCTOR.getMessageByLang(acceptLanguage));
//        }
//
//        OffsetDateTime rangeStartUtc = range.getStart();
//        if (rangeStartUtc.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
//            throw new InvalidStatusException(MessagesLangValues.APPOINTMENT_DATE_PASSED.getMessageByLang(acceptLanguage));
//        }
//
//        if (!range.isBooked()) {
//            throw new InvalidStatusException(MessagesLangValues.APPOINTMENT_NOT_BOOKED.getMessageByLang(acceptLanguage));
//        }
//
//        Appointment appointment = range.getAppointment();
//        if (appointment == null) {
//            throw new ResourceNotFoundException(MessagesLangValues.APPOINTMENT_DATA_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//
//        OffsetDateTime decisionTimeUtc = OffsetDateTime.now(ZoneOffset.UTC);
//
//        if (accepted) {
//            range.setStatus(RangeStatus.ACCEPTED.name());
//            appointment.setAppointmentDate(decisionTimeUtc);
//            appointment.setStatus(RangeStatus.ACCEPTED.name());
//            pushNotificationService.sendBookingUpdateNotificationToPatientByDoctor(range, acceptLanguage, timezone, true, patient);
//
//        } else {
//            pushNotificationService.sendBookingUpdateNotificationToPatientByDoctor(range, acceptLanguage, timezone, false, patient);
//
//            appointment.setAppointmentDate(decisionTimeUtc);
//            appointment.setStatus(RangeStatus.REJECTED.name());
//            appointment.setRange(null);
//
//            range.setStatus(RangeStatus.AVAILABLE.name());
//            range.setBooked(false);
//            range.setAppointment(null);
//        }
//
//        rangeRepository.save(range);
//        appointmentRepository.save(appointment);
//
//
//        return RangeResponse.builder()
//                .rangeId(rangeId)
//                .booked(range.isBooked())
//                .start(helper.getLocalTimeFromUtcUseTZ(range.getStart(), timezone))
//                .end(helper.getLocalTimeFromUtcUseTZ(range.getEnd(), timezone))
//                .status(range.getStatus())
//                .message(accepted
//                        ? MessagesLangValues.APPOINTMENT_ACCEPTED.getMessageByLang(acceptLanguage)
//                        : MessagesLangValues.APPOINTMENT_REJECTED.getMessageByLang(acceptLanguage))
//                .build();
//    }

    /**
     * tr: Müşterinin kendi randevusunu iptal etme metodu; şu an devre dışıdır ve her zaman null döner (eski implementasyon aşağıda yorum satırındadır).
     * en: Method for a customer cancelling their own booking; currently disabled and always returns null (the old implementation is commented out below).
     */
    @Override
    public RangeResponse deleteBookingByCustomer(Long rangeId, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        return null;
    }


//    @Override
//    @Transactional
//    public RangeResponse deleteBookingByPatient(Long rangeId, String role, String phoneNumber,
//                                                String userIdHeader, String timezone, String acceptLanguage) {
//
//        if (role == null || !role.equals(UserRoles.USER.name())) {
//            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
//        }
//        if (rangeId == null || phoneNumber == null || userIdHeader == null) {
//            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
//        }
//
//        Patient patient = patientRepository.findByUserIdAndPhoneNumber(Long.valueOf(userIdHeader), phoneNumber);
//        if (patient == null) {
//            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//        if (!patient.getStatus().equals(UserStatus.ACTIVE.name())) {
//            throw new InvalidStatusException(MessagesLangValues.USER_NOT_ACTIVE.getMessageByLang(acceptLanguage));
//        }
//
//        Appointment appointment = appointmentRepository.findByPatientAndRange_RangeId(patient, rangeId);
//        if (appointment == null) {
//            throw new ResourceNotFoundException(MessagesLangValues.APPOINTMENT_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//        if (!(appointment.getStatus().equals(AppointmentStatus.PENDING.name())
//                || appointment.getStatus().equals(AppointmentStatus.ACCEPTED.name()))) {
//            throw new InvalidStatusException(MessagesLangValues.APPOINTMENT_STATUS_ALREADY_SET.getMessageByLang(acceptLanguage));
//        }
//
//        Range range = rangeRepository.findByAppointment(appointment);
//        if (range == null) {
//            throw new ResourceNotFoundException(MessagesLangValues.APPOINTMENT_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//
//        ZonedDateTime nowLocal = ZonedDateTime.now(ZoneId.of(timezone));
//        ZonedDateTime rangeStartLocal = helper.toZonedDateTime(range.getStart(), timezone);
//
//        if (rangeStartLocal.isBefore(nowLocal)) {
//            throw new InvalidStatusException(MessagesLangValues.PAST_DATE_NOT_ALLOWED.getMessageByLang(acceptLanguage));
//        }
//
//        if (rangeStartLocal.isBefore(nowLocal.plusHours(lastDeleteHours))) {
//            throw new InvalidStatusException(MessagesLangValues.DELETE_TIME_EXPIRED.getMessageByLang(acceptLanguage));
//        }
//
//        pushNotificationService.sendBookingCancellationNotificationToDoctorByPatient(range, acceptLanguage, timezone);
//
//        appointment.setStatus(AppointmentStatus.DELETED_BY_PATIENT.name());
//        appointment.setRange(null);
//
//        range.setStatus(RangeStatus.AVAILABLE.name());
//        range.setBooked(false);
//        range.setAppointment(null);
//
//        rangeRepository.save(range);
//        appointmentRepository.save(appointment);
//
//
//        return RangeResponse.builder()
//                .rangeId(rangeId)
//                .booked(range.isBooked())
//                .start(helper.getLocalTimeFromUtcUseTZ(range.getStart(), timezone))
//                .end(helper.getLocalTimeFromUtcUseTZ(range.getEnd(), timezone))
//                .status(range.getStatus())
//                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
//                .build();
//
//    }

    /**
     * tr: Appointment entity'sini kullanıcının saat dilimine göre biçimlendirilmiş tarih alanlarıyla AppointmentResponse'a çevirir; appointment null ise null döner.
     * en: Converts an Appointment entity to an AppointmentResponse with dates formatted in the user's timezone; returns null when the appointment is null.
     */
    public AppointmentResponse convertToResponse(Appointment appointment, String timezone, String acceptLanguage) {
        if (appointment == null) return null;

        OffsetDateTime appointmentDateLocal = helper.getLocalDateTimeFromUtcUseTZ(appointment.getAppointmentDate(), timezone);
        OffsetDateTime appointmentStartLocal = helper.getLocalDateTimeFromUtcUseTZ(appointment.getAppointmentStart(), timezone);
        OffsetDateTime appointmentEndLocal = helper.getLocalDateTimeFromUtcUseTZ(appointment.getAppointmentEnd(), timezone);

        String appointmentDateString = helper.formatAppointmentDate(appointmentDateLocal, acceptLanguage);
        String appointmentStartString = helper.formatAppointmentDate(appointmentStartLocal, acceptLanguage);
        String appointmentEndString = helper.formatAppointmentDate(appointmentEndLocal, acceptLanguage);

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentDate(appointmentDateString)
                .appointmentStart(appointmentStartString)
                .appointmentEnd(appointmentEndString)
                .status(appointment.getStatus())
                .serviceCategory(appointment.getServiceCategory())
                .autoServiceId(appointment.getAutoService() != null ? appointment.getAutoService().getId() : null)
                .autoServiceName(appointment.getAutoService() != null ? appointment.getAutoService().getName() : null)
                .autoServiceNumber(appointment.getAutoService() != null ? appointment.getAutoService().getPhoneNumber() : null)
                .serviceCategory(appointment.getServiceCategory())
                .customerNumber(appointment.getCustomer() != null ? appointment.getCustomer().getPhoneNumber() : null)
                .customerName(appointment.getCustomer() != null ? appointment.getCustomer().getName() + " " + appointment.getCustomer().getSurname() : null)
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }


}
