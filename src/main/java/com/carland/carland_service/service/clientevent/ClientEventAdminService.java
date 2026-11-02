package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.entity.ClientErrorEvent;
import com.carland.carland_service.entity.ClientErrorIssue;
import com.carland.carland_service.repository.ClientErrorEventRepository;
import com.carland.carland_service.repository.ClientErrorIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientEventAdminService {

    public static final int PAGE_SIZE = 20;
    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final LocalDateTime WIDE_FROM = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final Sort EVENT_SORT = Sort.by(Sort.Direction.DESC, "occurredAt", "id");
    private static final Sort ISSUE_SORT = Sort.by(Sort.Direction.DESC, "lastSeen", "id");
    private static final Set<String> TABS = Set.of(
            "overview", "fatal", "server", "client", "nonfatal", "device", "timeline");

    private final ClientErrorEventRepository events;
    private final ClientErrorIssueRepository issues;

    public UserView user(long userId, String tab, LocalDate from, LocalDate to, int page) {
        String resolved = TABS.contains(tab) ? tab : "overview";
        LocalDateTime fromUtc = fromUtc(from);
        LocalDateTime toUtc = toUtc(to);
        boolean list = isList(resolved);

        long fatal = 0;
        long nonFatal = 0;
        long api = 0;
        for (Object[] row : events.countByType(userId, fromUtc, toUtc)) {
            long count = num(row[1]);
            switch (String.valueOf(row[0])) {
                case "fatal" -> fatal = count;
                case "nonFatal" -> nonFatal = count;
                case "apiError" -> api = count;
                default -> {
                }
            }
        }

        List<EventRow> rows = List.of();
        PageWindow window = new PageWindow(1, 1, 1, 1, 0);
        if (list) {
            Page<ClientErrorEvent> result = eventPage(
                    ClientEventSpecs.events(userId, null, resolved, fromUtc, toUtc), page);
            rows = result.getContent().stream().map(this::row).toList();
            window = window(result);
        }

        return new UserView(
                userId,
                resolved,
                from,
                to,
                list,
                fatal + nonFatal + api,
                fatal,
                nonFatal,
                api,
                ClientEventLabels.time(events.lastSeen(userId, fromUtc, toUtc)),
                days(userId, 7),
                days(userId, 30),
                top(userId, fromUtc, toUtc),
                rows,
                breakdown(events.countByOs(userId, fromUtc, toUtc), true),
                apps(userId, fromUtc, toUtc),
                breakdown(events.countByNetwork(userId, fromUtc, toUtc), false),
                events.countInstalls(userId, fromUtc, toUtc),
                installs(userId, fromUtc, toUtc),
                window
        );
    }

    public IssueListView issues(String eventType, LocalDate from, LocalDate to, int page) {
        String type = switch (eventType == null ? "all" : eventType) {
            case "fatal", "nonFatal", "apiError" -> eventType;
            default -> "all";
        };
        Page<ClientErrorIssue> result = issuePage(
                ClientEventSpecs.issues(type, from == null ? null : fromUtc(from), to == null ? null : toUtc(to)),
                page);
        List<IssueRow> rows = result.getContent().stream().map(this::issueRow).toList();
        return new IssueListView(type, from, to, rows, window(result));
    }

    public IssueEventsView issueEvents(long issueId, int page) {
        ClientErrorIssue issue = issues.findById(issueId).orElse(null);
        if (issue == null) {
            return null;
        }
        Page<ClientErrorEvent> result = eventPage(ClientEventSpecs.events(null, issueId, null, null, null), page);
        List<EventRow> rows = result.getContent().stream().map(this::row).toList();
        return new IssueEventsView(issueRow(issue), rows, window(result));
    }

    public DetailView detail(long id) {
        return events.findById(id).map(this::detailOf).orElse(null);
    }

    private List<DayRow> days(long userId, int length) {
        LocalDate today = LocalDate.now(BAKU);
        LocalDate start = today.minusDays(length - 1L);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : events.countByDay(userId, fromUtc(start), toUtc(today))) {
            counts.put(String.valueOf(row[0]), num(row[1]));
        }
        List<DayRow> out = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            String key = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
            out.add(new DayRow(ClientEventLabels.dayLabel(key), counts.getOrDefault(key, 0L)));
        }
        return out;
    }

    private List<TopRow> top(long userId, LocalDateTime from, LocalDateTime to) {
        List<TopRow> out = new ArrayList<>();
        for (Object[] row : events.topIssues(userId, from, to)) {
            long issueId = num(row[0]);
            issues.findById(issueId).ifPresent(issue -> out.add(new TopRow(
                    issue.getId(),
                    issue.getTitle(),
                    ClientEventLabels.type(issue.getEventType()),
                    num(row[1]))));
        }
        return out;
    }

    private List<Breakdown> apps(long userId, LocalDateTime from, LocalDateTime to) {
        List<Breakdown> out = new ArrayList<>();
        for (Object[] row : events.countByApp(userId, from, to)) {
            String version = row[0] == null ? "" : String.valueOf(row[0]);
            String build = row[1] == null ? "" : String.valueOf(row[1]);
            out.add(new Breakdown(version + " (" + build + ")", num(row[2])));
        }
        return out;
    }

    private List<Breakdown> breakdown(List<Object[]> rows, boolean os) {
        List<Breakdown> out = new ArrayList<>();
        for (Object[] row : rows) {
            String raw = row[0] == null ? "" : String.valueOf(row[0]);
            String label = os ? ClientEventLabels.os(raw) : ClientEventLabels.network(raw);
            if (label.isBlank()) {
                label = "Naməlum";
            }
            out.add(new Breakdown(label, num(row[1])));
        }
        return out;
    }

    private List<String> installs(long userId, LocalDateTime from, LocalDateTime to) {
        return events.installIds(userId, from, to).stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .limit(8)
                .toList();
    }

    private Page<ClientErrorEvent> eventPage(org.springframework.data.jpa.domain.Specification<ClientErrorEvent> spec,
                                             int page) {
        int index = Math.max(page, 1) - 1;
        Page<ClientErrorEvent> result = events.findAll(spec, PageRequest.of(index, PAGE_SIZE, EVENT_SORT));
        if (result.getTotalPages() > 0 && index >= result.getTotalPages()) {
            index = result.getTotalPages() - 1;
            result = events.findAll(spec, PageRequest.of(index, PAGE_SIZE, EVENT_SORT));
        }
        return result;
    }

    private Page<ClientErrorIssue> issuePage(org.springframework.data.jpa.domain.Specification<ClientErrorIssue> spec,
                                             int page) {
        int index = Math.max(page, 1) - 1;
        Page<ClientErrorIssue> result = issues.findAll(spec, PageRequest.of(index, PAGE_SIZE, ISSUE_SORT));
        if (result.getTotalPages() > 0 && index >= result.getTotalPages()) {
            index = result.getTotalPages() - 1;
            result = issues.findAll(spec, PageRequest.of(index, PAGE_SIZE, ISSUE_SORT));
        }
        return result;
    }

    private EventRow row(ClientErrorEvent event) {
        return new EventRow(
                event.getId(),
                ClientEventLabels.time(event.getOccurredAt()),
                ClientEventLabels.type(event.getEventType()),
                event.getMessage(),
                event.getAppVersion() + " (" + event.getBuildNumber() + ")",
                ClientEventLabels.os(event.getOsName()) + " " + event.getOsVersion(),
                ClientEventLabels.network(event.getNetwork()),
                event.getInstallId(),
                event.getEndpoint() == null ? "" : event.getEndpoint(),
                "apiError".equals(event.getEventType()) ? ClientEventLabels.statusCode(event.getStatusCode()) : "",
                event.isExpected(),
                ClientEventLabels.user(event.getUserId())
        );
    }

    private IssueRow issueRow(ClientErrorIssue issue) {
        return new IssueRow(
                issue.getId(),
                issue.getTitle(),
                ClientEventLabels.type(issue.getEventType()),
                issue.getOccurrenceCount(),
                issue.getAffectedUserCount(),
                issue.getAffectedInstallCount(),
                ClientEventLabels.time(issue.getFirstSeen()),
                ClientEventLabels.time(issue.getLastSeen()),
                issue.isHighFrequency() ? "Yüksək tezlik" : "Adi",
                ClientEventLabels.issueStatus(issue.getIssueStatus()),
                issue.getEndpoint() == null ? "" : issue.getEndpoint(),
                issue.isHighFrequency()
        );
    }

    private DetailView detailOf(ClientErrorEvent event) {
        List<Crumb> crumbs = new ArrayList<>();
        if (event.getBreadcrumbs() != null) {
            for (Map<String, Object> item : event.getBreadcrumbs()) {
                String raw = item.get("timestamp") == null ? "" : String.valueOf(item.get("timestamp"));
                crumbs.add(new Crumb(
                        raw,
                        ClientEventLabels.crumbTime(raw),
                        item.get("category") == null ? "" : String.valueOf(item.get("category")),
                        item.get("message") == null ? "" : String.valueOf(item.get("message"))
                ));
            }
            crumbs.sort(Comparator.comparing(Crumb::rawTime));
        }
        return new DetailView(
                event.getId(),
                event.getIssueId(),
                event.getUserId(),
                ClientEventLabels.time(event.getOccurredAt()),
                ClientEventLabels.type(event.getEventType()),
                event.getMessage(),
                event.getStackTrace() == null ? "" : event.getStackTrace(),
                crumbs,
                event.getAppVersion() + " (" + event.getBuildNumber() + ")",
                event.getPackageName(),
                ClientEventLabels.os(event.getOsName()) + " " + event.getOsVersion(),
                ClientEventLabels.network(event.getNetwork()),
                event.getEnvironment(),
                event.getInstallId(),
                event.getSessionId(),
                event.getEndpoint() == null ? "" : event.getEndpoint(),
                "apiError".equals(event.getEventType()) ? ClientEventLabels.statusCode(event.getStatusCode()) : "",
                event.getContextRoute() == null ? "" : event.getContextRoute(),
                event.getContextLibrary() == null ? "" : event.getContextLibrary(),
                ClientEventLabels.memory(event.getMemoryRssBytes()),
                event.isExpected(),
                ClientEventLabels.user(event.getUserId())
        );
    }

    private static PageWindow window(Page<?> page) {
        int totalPages = Math.max(page.getTotalPages(), 1);
        int current = page.getTotalElements() == 0 ? 1 : page.getNumber() + 1;
        int start = Math.max(1, current - 2);
        int end = Math.min(totalPages, current + 2);
        if (start > end) {
            start = end;
        }
        return new PageWindow(current, totalPages, start, end, page.getTotalElements());
    }

    private static LocalDateTime fromUtc(LocalDate bakuDate) {
        if (bakuDate == null) {
            return WIDE_FROM;
        }
        return bakuDate.atStartOfDay().minusHours(4);
    }

    private static LocalDateTime toUtc(LocalDate bakuDate) {
        if (bakuDate == null) {
            return LocalDateTime.now(ZoneOffset.UTC).plusDays(2);
        }
        return bakuDate.plusDays(1).atStartOfDay().minusHours(4);
    }

    private static boolean isList(String tab) {
        return switch (tab) {
            case "fatal", "server", "client", "nonfatal", "timeline" -> true;
            default -> false;
        };
    }

    private static long num(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public record UserView(
            long userId,
            String tab,
            LocalDate from,
            LocalDate to,
            boolean listTab,
            long total,
            long fatal,
            long nonFatal,
            long api,
            String lastSeen,
            List<DayRow> days7,
            List<DayRow> days30,
            List<TopRow> top,
            List<EventRow> rows,
            List<Breakdown> osRows,
            List<Breakdown> appRows,
            List<Breakdown> networkRows,
            long installCount,
            List<String> installIds,
            PageWindow page
    ) {
    }

    public record IssueListView(
            String type,
            LocalDate from,
            LocalDate to,
            List<IssueRow> rows,
            PageWindow page
    ) {
    }

    public record IssueEventsView(IssueRow issue, List<EventRow> rows, PageWindow page) {
    }

    public record DetailView(
            long id,
            long issueId,
            Long userId,
            String time,
            String typeLabel,
            String message,
            String stack,
            List<Crumb> crumbs,
            String app,
            String packageName,
            String os,
            String network,
            String environment,
            String installId,
            String sessionId,
            String endpoint,
            String statusLabel,
            String route,
            String library,
            String memory,
            boolean expected,
            String userLabel
    ) {
    }

    public record EventRow(
            long id,
            String time,
            String typeLabel,
            String message,
            String app,
            String os,
            String network,
            String installId,
            String endpoint,
            String statusLabel,
            boolean expected,
            String userLabel
    ) {
    }

    public record IssueRow(
            long id,
            String title,
            String typeLabel,
            long count,
            long users,
            long installs,
            String firstSeen,
            String lastSeen,
            String frequencyLabel,
            String statusLabel,
            String endpoint,
            boolean highFrequency
    ) {
    }

    public record DayRow(String label, long count) {
    }

    public record TopRow(long id, String title, String typeLabel, long count) {
    }

    public record Breakdown(String label, long count) {
    }

    public record Crumb(String rawTime, String time, String category, String message) {
    }

    public record PageWindow(int currentPage, int totalPages, int windowStart, int windowEnd, long totalItems) {
    }
}
