package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingPartnerRepository extends JpaRepository<BookingPartner, Long> {

    List<BookingPartner> findAllByOrderByIdDesc();

    Optional<BookingPartner> findByHqUserId(Long hqUserId);

    boolean existsByHqUserId(Long hqUserId);
}
