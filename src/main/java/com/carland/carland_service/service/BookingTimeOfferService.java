package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.ChatMessageView;
import com.carland.carland_service.dto.booking.TimeOfferDay;
import com.carland.carland_service.dto.booking.TimeOfferOptions;
import com.carland.carland_service.dto.booking.TimeOfferRequest;
import com.carland.carland_service.dto.booking.TimeOfferSlot;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingMessage;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.BookingTimeProposal;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.CalendarStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingMessageRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BookingTimeProposalRepository;
import com.carland.carland_service.repository.RangeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * tr: Şube, rezervasyonun kendi hizmetinin boş saatini teklif eder. Saat, müşteri kabul edince değişir.
 * en: The branch offers a free hour of the booking's own service. The hour changes when the customer accepts.
 */
@Service
@RequiredArgsConstructor
public class BookingTimeOfferService {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final BookingRepository bookingRepository;
    private final BookingMessageRepository messageRepository;
    private final BookingTimeProposalRepository proposalRepository;
    private final RangeRepository rangeRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final BookingChatService bookingChatService;
    private final BookingChatLiveService chatLiveService;
    private final BookingPushService bookingPushService;

    @Transactional(readOnly = true)
    public TimeOfferOptions options(Long userId, boolean mustChangePassword, Long bookingId, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        requireOpen(booking);
        Range current = booking.getRange();
        if (current == null || booking.getBranch() == null) {
            return TimeOfferOptions.builder().serviceName("").days(List.of()).build();
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<Range> rows = rangeRepository.findOpenRanges(
                booking.getBranch().getId(), now, now.plusDays(21));
        Map<String, TimeOfferDay> days = new LinkedHashMap<>();
        for (Range range : rows) {
            if (!offerable(current, range, booking)) {
                continue;
            }
            ZonedDateTime start = range.getStart().atZoneSameInstant(StaffSlotWindows.ZONE);
            String key = start.toLocalDate().format(DAY_KEY);
            TimeOfferDay day = days.computeIfAbsent(key, ignored -> TimeOfferDay.builder()
                    .day(key)
                    .label(start.toLocalDate().format(DAY))
                    .slots(new ArrayList<>())
                    .build());
            day.getSlots().add(TimeOfferSlot.builder()
                    .rangeId(range.getRangeId())
                    .start(start.toLocalTime().format(CLOCK))
                    .end(range.getEnd() == null ? null
                            : range.getEnd().atZoneSameInstant(StaffSlotWindows.ZONE).toLocalTime().format(CLOCK))
                    .build());
        }
        return TimeOfferOptions.builder()
                .serviceName(serviceName(booking, current))
                .days(new ArrayList<>(days.values()))
                .build();
    }

    @Transactional
    public ChatMessageView propose(Long userId, boolean mustChangePassword, Long bookingId,
                                   TimeOfferRequest request, String acceptLanguage) {
        Booking booking = loadStaffBooking(userId, mustChangePassword, bookingId, acceptLanguage);
        requireOpen(booking);
        if (request == null || request.getRangeId() == null) {
            throw MissingFieldException.required("rangeId");
        }
        Range current = booking.getRange();
        Range target = rangeRepository.lockByRangeId(request.getRangeId())
                .orElseThrow(() -> new ResourceNotFoundException("slot not found"));
        if (!offerable(current, target, booking)) {
            throw new ConflictException("slotFull");
        }
        supersedePending(booking);
        String text = request.getText() == null ? "" : request.getText().trim();
        if (text.length() > 2000) {
            throw new MissingFieldException("Text exceeds 2000 characters");
        }
        BookingTimeProposal proposal = proposalRepository.save(BookingTimeProposal.builder()
                .booking(booking)
                .fromRangeId(current == null ? null : current.getRangeId())
                .toRangeId(target.getRangeId())
                .status("pending")
                .createdByUserId(userId)
                .createdAt(OffsetDateTime.now())
                .build());
        boolean peerOnline = chatLiveService.hasPeerListener(booking.getId(), "branch");
        BookingMessage message = messageRepository.save(BookingMessage.builder()
                .booking(booking)
                .clientMessageId("offer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .fromRole("branch")
                .authorUserId(userId)
                .kind("time_proposal")
                .body(text)
                .delivery(peerOnline ? "delivered" : "sent")
                .rebook(false)
                .proposalId(proposal.getId())
                .proposalStatus("pending")
                .offerFrom(clock(current == null ? null : current.getStart()))
                .offerTo(clock(target.getStart()))
                .offerDay(day(target.getStart()))
                .createdAt(OffsetDateTime.now())
                .build());
        proposal.setMessageId(message.getId());
        proposalRepository.save(proposal);
        ChatMessageView view = bookingChatService.toView(message);
        chatLiveService.publishNewMessageAfterCommit(booking.getId(), view);
        if (!peerOnline) {
            bookingPushService.chatNewMessage(booking, "time_proposal", text);
        }
        return view;
    }

    @Transactional
    public ChatMessageView accept(Long customerUserId, String bookingKey, Long proposalId) {
        return decide(customerUserId, bookingKey, proposalId, "accepted");
    }

    @Transactional
    public ChatMessageView decline(Long customerUserId, String bookingKey, Long proposalId) {
        return decide(customerUserId, bookingKey, proposalId, "declined");
    }

    private ChatMessageView decide(Long customerUserId, String bookingKey, Long proposalId, String status) {
        Booking booking = loadOwned(customerUserId, bookingKey);
        if (proposalId == null) {
            throw MissingFieldException.required("proposalId");
        }
        BookingTimeProposal proposal = proposalRepository.findByIdAndBooking_Id(proposalId, booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("proposal not found"));
        if (!"pending".equals(proposal.getStatus())) {
            throw new ConflictException("proposalClosed");
        }
        if (!"open".equals(BookingChatService.stateOf(booking))) {
            throw new ConflictException("conversationClosed");
        }
        if ("accepted".equals(status)) {
            Range target = rangeRepository.lockByRangeId(proposal.getToRangeId())
                    .orElseThrow(() -> new ResourceNotFoundException("slot not found"));
            if (!offerable(booking.getRange(), target, booking)) {
                throw new ConflictException("slotFull");
            }
            booking.setRange(target);
            bookingRepository.save(booking);
        }
        proposal.setStatus(status);
        proposal.setDecidedAt(OffsetDateTime.now());
        proposalRepository.save(proposal);
        BookingMessage message = messageRepository.findById(proposal.getMessageId())
                .orElseThrow(() -> new ResourceNotFoundException("message not found"));
        message.setProposalStatus(status);
        messageRepository.save(message);
        ChatMessageView view = bookingChatService.toView(message);
        chatLiveService.publishTimeProposalAfterCommit(booking.getId(), view);
        return view;
    }

    private void supersedePending(Booking booking) {
        List<BookingTimeProposal> open = proposalRepository.findByBooking_IdAndStatus(booking.getId(), "pending");
        for (BookingTimeProposal row : open) {
            row.setStatus("superseded");
            row.setDecidedAt(OffsetDateTime.now());
            proposalRepository.save(row);
            if (row.getMessageId() == null) {
                continue;
            }
            messageRepository.findById(row.getMessageId()).ifPresent(message -> {
                message.setProposalStatus("superseded");
                messageRepository.save(message);
                chatLiveService.publishTimeProposalAfterCommit(booking.getId(), bookingChatService.toView(message));
            });
        }
    }

    private boolean offerable(Range current, Range candidate, Booking booking) {
        if (candidate == null || candidate.getRangeId() == null || candidate.getStart() == null) {
            return false;
        }
        if (current != null && candidate.getRangeId().equals(current.getRangeId())) {
            return false;
        }
        if (!sameService(current, candidate)) {
            return false;
        }
        if (candidate.getCalendar() == null || candidate.getCalendar().getBranch() == null
                || booking.getBranch() == null
                || !booking.getBranch().getId().equals(candidate.getCalendar().getBranch().getId())) {
            return false;
        }
        Calendar calendar = candidate.getCalendar();
        if (CalendarStatus.HIDDEN.name().equals(calendar.getStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(candidate.getDayHidden())) {
            return false;
        }
        String status = candidate.getStatus();
        if (status != null && !"AVAILABLE".equals(status)) {
            return false;
        }
        if (!candidate.getStart().isAfter(OffsetDateTime.now())) {
            return false;
        }
        int capacity = candidate.getWorkerCount() == null ? 0 : candidate.getWorkerCount();
        long taken = bookingRepository.countByRange_RangeIdAndStatusIn(
                candidate.getRangeId(), BookingStatus.occupyingCapacity());
        if (taken >= capacity) {
            return false;
        }
        return booking.getCarId() == null || !bookingRepository.existsByCarIdAndRange_RangeIdAndStatusInAndIdNot(
                booking.getCarId(), candidate.getRangeId(), BookingStatus.holdingCarOnRange(), booking.getId());
    }

    static boolean sameService(Range current, Range candidate) {
        if (current == null || candidate == null) {
            return false;
        }
        String target = current.getSlotTarget() == null ? "" : current.getSlotTarget().trim();
        String other = candidate.getSlotTarget() == null ? "" : candidate.getSlotTarget().trim();
        if (!target.equals(other)) {
            return false;
        }
        if (StaffSlotTargets.PACKAGE.equals(target)) {
            Long left = current.getCarePackage() == null ? null : current.getCarePackage().getId();
            Long right = candidate.getCarePackage() == null ? null : candidate.getCarePackage().getId();
            return left != null && left.equals(right);
        }
        if (StaffSlotTargets.INDIVIDUAL.equals(target)) {
            Long left = current.getIndividualService() == null ? null : current.getIndividualService().getId();
            Long right = candidate.getIndividualService() == null ? null : candidate.getIndividualService().getId();
            return left != null && left.equals(right);
        }
        return StaffSlotTargets.REPAIR_INSPECTION.equals(target);
    }

    private static String serviceName(Booking booking, Range range) {
        if (range == null || range.getSlotTarget() == null) {
            return "";
        }
        if (StaffSlotTargets.REPAIR_INSPECTION.equals(range.getSlotTarget())) {
            return "Təmir və diaqnostika";
        }
        if (StaffSlotTargets.PACKAGE.equals(range.getSlotTarget())) {
            if (booking.getPackageName() != null && !booking.getPackageName().isBlank()) {
                return booking.getPackageName().trim();
            }
            if (range.getCarePackage() != null && range.getCarePackage().getName() != null) {
                return range.getCarePackage().getName();
            }
        }
        if (StaffSlotTargets.INDIVIDUAL.equals(range.getSlotTarget()) && range.getIndividualService() != null) {
            return azTitle(range.getIndividualService().getTitleJson());
        }
        return "";
    }

    private static String azTitle(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonNode node = JSON.readTree(json);
            String az = node.path("az").asText("");
            if (!az.isBlank()) {
                return az;
            }
            return node.path("en").asText("");
        } catch (Exception ex) {
            return "";
        }
    }

    private static String clock(OffsetDateTime time) {
        if (time == null) {
            return "";
        }
        return time.atZoneSameInstant(StaffSlotWindows.ZONE).toLocalTime().format(CLOCK);
    }

    private static String day(OffsetDateTime time) {
        if (time == null) {
            return "";
        }
        return time.atZoneSameInstant(StaffSlotWindows.ZONE).toLocalDate().format(DAY);
    }

    private void requireOpen(Booking booking) {
        if (!"open".equals(BookingChatService.stateOf(booking))) {
            throw new ConflictException("conversationClosed");
        }
    }

    private Booking loadStaffBooking(Long userId, boolean mustChangePassword, Long bookingId, String acceptLanguage) {
        if (bookingId == null) {
            throw MissingFieldException.required("bookingId");
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        Long branchId = booking.getBranch() == null ? null : booking.getBranch().getId();
        bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
        return booking;
    }

    private Booking loadOwned(Long customerUserId, String bookingKey) {
        if (bookingKey == null || bookingKey.isBlank()) {
            throw MissingFieldException.required("bookingId");
        }
        Booking booking;
        if (bookingKey.regionMatches(true, 0, "CC-", 0, 3)) {
            booking = bookingRepository.findByRef(bookingKey.toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        } else {
            Long id;
            try {
                id = Long.valueOf(bookingKey.trim());
            } catch (NumberFormatException ex) {
                throw new ResourceNotFoundException("booking not found");
            }
            booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        }
        if (booking.getCustomerUserId() == null || !booking.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("booking is not yours");
        }
        return booking;
    }
}
