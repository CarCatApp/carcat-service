package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingRatingCreateRequest;
import com.carland.carland_service.dto.booking.BookingRatingView;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Rating;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRatingServiceTest {

    @Mock RatingRepository ratingRepository;
    @Mock BranchRepository branchRepository;
    @Mock PartnerRepository partnerRepository;
    @InjectMocks BookingRatingService service;

    Partner partner;
    Branch branch;

    @BeforeEach
    void setUp() {
        partner = Partner.builder().id(1L).name("Hyper").source("hyper").active(true).build();
        branch = Branch.builder().id(7L).name("Babek").partner(partner).active(true).build();
    }

    @Test
    void addScoreUpdatesBranchAndPartnerAverage() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating row = inv.getArgument(0);
            row.setId(11L);
            return row;
        });
        Rating stored = Rating.builder().id(11L).branch(branch).userId(77L).score(5).build();
        when(ratingRepository.findByBranch_IdIn(any())).thenReturn(List.of(stored));
        when(branchRepository.findByPartnerIdOrderByIdAsc(1L)).thenReturn(List.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerRepository.save(any(Partner.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRatingView out = service.add(7L, 77L, BookingRatingCreateRequest.builder()
                .score(5)
                .text("yaxşı")
                .build());

        assertEquals(11L, out.getId());
        assertEquals(7L, out.getBranchId());
        assertEquals(5, out.getScore());
        assertEquals(5.0, out.getRating());
        assertEquals(1L, out.getRatingCount());
        assertEquals(5.0, out.getPartnerRating());
        assertEquals(1L, out.getPartnerRatingCount());
    }

    @Test
    void addCommentWithoutScore() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating row = inv.getArgument(0);
            row.setId(12L);
            return row;
        });
        Rating comment = Rating.builder().id(12L).branch(branch).userId(77L).text("ok").build();
        Rating scored = Rating.builder().id(11L).branch(branch).userId(76L).score(4).build();
        when(ratingRepository.findByBranch_IdIn(any())).thenReturn(List.of(scored, comment));
        when(branchRepository.findByPartnerIdOrderByIdAsc(1L)).thenReturn(List.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerRepository.save(any(Partner.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRatingView out = service.add(7L, 77L, BookingRatingCreateRequest.builder()
                .text("ok")
                .build());

        assertNull(out.getScore());
        assertEquals(2L, out.getRatingCount());
        assertEquals(4.0, out.getRating());
    }

    @Test
    void addRejectsEmptyBody() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        assertThrows(MissingFieldException.class, () -> service.add(7L, 77L, new BookingRatingCreateRequest()));
    }

    @Test
    void addRejectsScoreOutOfRange() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        assertThrows(MissingFieldException.class, () -> service.add(7L, 77L,
                BookingRatingCreateRequest.builder().score(6).build()));
    }

    @Test
    void addMissingBranch() {
        when(branchRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.add(7L, 77L,
                BookingRatingCreateRequest.builder().score(5).build()));
    }
}
