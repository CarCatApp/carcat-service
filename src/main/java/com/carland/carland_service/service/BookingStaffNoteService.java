package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.dto.booking.StaffNoteView;
import com.carland.carland_service.dto.booking.StaffNotesResponse;
import com.carland.carland_service.dto.request.AdminBookingNoteSaveRequest;
import com.carland.carland_service.dto.response.AdminBookingNoteRow;
import com.carland.carland_service.entity.BookingStaffNote;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingStaffNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * tr: Admin not kataloqu və partnerin seçdiyi səbəbin bookingə yazılacak kopyası.
 * en: Admin note catalogue and the snapshot written onto a booking.
 */
@Service
@RequiredArgsConstructor
public class BookingStaffNoteService {

    private static final int NOTE_MAX = 500;

    private final BookingStaffNoteRepository noteRepository;
    private final ServiceCategoryJson json;

    public record Applied(String code, String note) {}

    @Transactional(readOnly = true)
    public List<AdminBookingNoteRow> rows(String kind) {
        List<AdminBookingNoteRow> rows = new ArrayList<>();
        for (BookingStaffNote note : noteRepository.findByKindOrderBySortOrderAscIdAsc(requireKind(kind))) {
            rows.add(toRow(note));
        }
        return rows;
    }

    @Transactional
    public AdminBookingNoteRow save(AdminBookingNoteSaveRequest request) {
        AdminBookingNoteSaveRequest body = request == null ? new AdminBookingNoteSaveRequest() : request;
        String kind = requireKind(body.getKind());
        String az = text(body.getTitleAz());
        if (az.isEmpty()) {
            throw MissingFieldException.required("titleAz");
        }
        BookingStaffNote note = body.getId() == null
                ? new BookingStaffNote()
                : noteRepository.findById(body.getId())
                .orElseThrow(() -> new ResourceNotFoundException("note not found"));
        if (note.getId() != null && !kind.equals(note.getKind())) {
            throw new ConflictException("note kind cannot change");
        }
        note.setKind(kind);
        note.setTitleJson(json.write(az, text(body.getTitleEn()), text(body.getTitleRu())));
        note.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        if (note.getCode() == null || note.getCode().isBlank()) {
            note.setCode(kind + "_" + System.nanoTime());
        }
        return toRow(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw MissingFieldException.required("id");
        }
        if (!noteRepository.existsById(id)) {
            throw new ResourceNotFoundException("note not found");
        }
        noteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public StaffNotesResponse list(String kind, String acceptLanguage) {
        String wanted = requireKind(kind);
        String lang = BookingMineService.langOf(acceptLanguage);
        List<StaffNoteView> items = new ArrayList<>();
        for (BookingStaffNote note : noteRepository.findByKindOrderBySortOrderAscIdAsc(wanted)) {
            items.add(StaffNoteView.builder().id(note.getId()).title(sentence(note, lang)).build());
        }
        return StaffNotesResponse.builder().kind(wanted).items(items).build();
    }

    /**
     * tr: Katalog seçimi kodu və o andaki cümleyi döner. Əl ilə yazılan mətnin kodu yoxdur.
     * en: A catalogue pick returns its code and the sentence at this moment. Manual text has no code.
     */
    @Transactional(readOnly = true)
    public Applied apply(String kind, BookingRejectRequest request, String acceptLanguage) {
        BookingRejectRequest body = request == null ? new BookingRejectRequest() : request;
        String lang = BookingMineService.langOf(acceptLanguage);
        if (body.getNoteId() != null) {
            BookingStaffNote note = noteRepository.findById(body.getNoteId())
                    .orElseThrow(() -> new ResourceNotFoundException("note not found"));
            if (!requireKind(kind).equals(note.getKind())) {
                throw new ResourceNotFoundException("note not found");
            }
            return new Applied(note.getCode(), sentence(note, lang));
        }
        String manual = text(body.getReason());
        if (manual.isEmpty()) {
            throw MissingFieldException.required("reason");
        }
        if (manual.length() > NOTE_MAX) {
            throw new ConflictException("reason is too long");
        }
        return new Applied(null, manual);
    }

    private AdminBookingNoteRow toRow(BookingStaffNote note) {
        Map<String, String> titles = json.read(note.getTitleJson());
        return AdminBookingNoteRow.builder()
                .id(note.getId())
                .kind(note.getKind())
                .code(note.getCode())
                .titleAz(titles.getOrDefault("az", ""))
                .titleEn(titles.getOrDefault("en", ""))
                .titleRu(titles.getOrDefault("ru", ""))
                .sortOrder(note.getSortOrder())
                .build();
    }

    private String sentence(BookingStaffNote note, String lang) {
        Map<String, String> titles = json.read(note.getTitleJson());
        String picked = titles.get(lang);
        if (picked != null && !picked.isBlank()) {
            return picked;
        }
        for (String fallback : List.of("az", "en", "ru")) {
            String value = titles.get(fallback);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    static String requireKind(String kind) {
        String value = kind == null ? "" : kind.trim().toLowerCase();
        if (BookingStaffNote.CANCEL.equals(value) || BookingStaffNote.NO_SHOW.equals(value)) {
            return value;
        }
        throw MissingFieldException.required("kind");
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
