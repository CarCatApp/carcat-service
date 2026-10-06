package com.carland.carland_service.repository;

import com.carland.carland_service.entity.BookingStaffNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStaffNoteRepository extends JpaRepository<BookingStaffNote, Long> {

    List<BookingStaffNote> findByKindOrderBySortOrderAscIdAsc(String kind);
}
