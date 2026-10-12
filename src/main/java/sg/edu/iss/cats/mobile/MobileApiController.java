package sg.edu.iss.cats.mobile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.TrainingAccountService;
import sg.edu.iss.cats.service.TrainingLedgerService;

@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {
    private final MobileAccessService access;
    private final CourseRepository courses;
    private final AppRepo apps;
    private final HolidayRepository holidays;
    private final TrainingAccountService accounts;
    private final TrainingLedgerService ledgers;
    public MobileApiController(MobileAccessService access, CourseRepository courses, AppRepo apps,
            HolidayRepository holidays, TrainingAccountService accounts, TrainingLedgerService ledgers) {
        this.access = access; this.courses = courses; this.apps = apps; this.holidays = holidays;
        this.accounts = accounts; this.ledgers = ledgers;
    }

    public record PairRequest(String pairingCode, String deviceName) {}
    public record PairResponse(String accessToken, String tokenType) {}
    public record CourseRow(int id, String title, String category, String start, String end,
            String provider, String location, String duration, String fee) {}
    public record ApplicationRow(int id, String course, String category, String status,
            String start, String end, String fee, String managerReason) {}
    public record HolidayRow(String date, String name) {}

    @PostMapping(value = "/pair", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PairResponse pair(@RequestBody PairRequest request) {
        try { return new PairResponse(access.redeem(request.pairingCode(), request.deviceName()), "Bearer"); }
        catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired pairing code"); }
    }

    @GetMapping("/snapshot")
    public Map<String, Object> snapshot(@RequestHeader(value = "Authorization", required = false) String authorization) {
        final User user;
        try { user = access.verifyBearer(authorization); }
        catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device token expired or revoked"); }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Singapore"));
        int year = today.getYear();
        List<CourseRow> catalogue = courses.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getArchived()))
                .map(c -> new CourseRow(c.getCourseId(), c.getCourseName(), c.getCourseType().name(),
                        c.getStartDate().toString(), c.getEndDate().toString(), c.getTrainingProvider(),
                        c.getLocation(), c.getDuration().name(), c.getFee().toPlainString()))
                .toList();
        List<ApplicationRow> applications = apps.findByUser_UserId(user.getUserId()).stream()
                .map(a -> new ApplicationRow(a.getId(), a.getCourse().getCourseName(),
                        a.getCourse().getCourseType().name(), a.getStatus().name(),
                        a.getCourse().getStartDate().toString(), a.getCourse().getEndDate().toString(),
                        a.getCourse().getFee().toPlainString(), a.getManagerReason()))
                .toList();
        List<HolidayRow> dates = holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(
                today.withDayOfYear(1), today.plusYears(1).withDayOfYear(1).minusDays(1))
                .stream().map(h -> new HolidayRow(h.getHolidayDate().toString(), h.getHolidayName())).toList();
        var summary = accounts.summary(user, year);
        var ledger = ledgers.forEmployee(user, year);
        return Map.of("syncedAt", java.time.Instant.now().toString(), "year", year,
                "employee", user.getName(), "courses", catalogue, "applications", applications,
                "holidays", dates, "remainingDays", summary.getDaysRemaining(),
                "remainingBudget", summary.getBudgetRemaining(), "ledger", ledger.entries());
    }
}
