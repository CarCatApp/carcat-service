package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * tr: Appointment entity'si için JPA repository; müşteri randevularını sorgular.
 * en: JPA repository for the Appointment entity; queries customer appointments.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {



    /** tr: Müşteri, servis kategorisi, tarih aralığı ve servis merkezine göre randevu bulur. / en: Finds an appointment by customer, service category, date range and auto service. */
    Optional<Appointment> findByCustomer_UserIdAndServiceCategoryAndAppointmentDateBetweenAndRange_Calendar_AutoService_Id(
            Long customerUserId,
            String serviceCategory,
            OffsetDateTime startOfDay,
            OffsetDateTime endOfDay,
            Long autoServiceId
    );

}
