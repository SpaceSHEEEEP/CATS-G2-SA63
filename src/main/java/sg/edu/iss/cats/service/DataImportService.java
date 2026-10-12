package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.Designation;
import sg.edu.iss.cats.model.Holiday;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.TrainingProvider;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.repository.TrainingProviderRepository;
import sg.edu.iss.cats.repository.UserRepository;

/** Restricted, insert-only import of structured CATS data, never executable SQL. */
@Service
public class DataImportService {
    public static final int MAX_BYTES = 512_000;

    public record ImportFile(String format, String source,
            List<UserRow> users, List<ProviderRow> providers, List<CourseRow> courses,
            List<HolidayRow> holidays, List<ApplicationRow> applications) {}
    public record UserRow(String name, String username, String email, Role role,
            String managerUsername, String passwordHash, Boolean active, Boolean admin,
            Designation designation, BigDecimal budgetedAllowance,
            BigDecimal actualAllowance, Double budgetedDays, Double actualDays) {}
    public record ProviderRow(String name, Boolean enabled) {}
    public record CourseRow(String name, CourseType type, CourseDuration duration,
            String startDate, String endDate, String location, String provider,
            BigDecimal fee, Boolean archived) {}
    public record HolidayRow(String date, String name) {}
    public record ApplicationRow(String username, String courseName, Status status,
            String userReason, String managerReason, String workDissemination) {}
    public record Summary(int users, int providers, int courses, int holidays, int applications) {}

    private final UserRepository users;
    private final CourseRepository courses;
    private final HolidayRepository holidays;
    private final TrainingProviderRepository providers;
    private final AppRepo applications;
    private final JsonMapper json;

    public DataImportService(UserRepository users, CourseRepository courses,
            HolidayRepository holidays, TrainingProviderRepository providers,
            AppRepo applications, JsonMapper json) {
        this.users = users; this.courses = courses; this.holidays = holidays;
        this.providers = providers; this.applications = applications; this.json = json;
    }

    @Transactional
    public Summary importFile(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("Upload a JSON file of at most 500 KB.");
        final ImportFile input;
        try { input = json.readValue(bytes, ImportFile.class); }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid CATS import JSON.", ex); }

        List<UserRow> userRows = rows(input == null ? null : input.users());
        List<ProviderRow> providerRows = rows(input == null ? null : input.providers());
        List<CourseRow> courseRows = rows(input == null ? null : input.courses());
        List<HolidayRow> holidayRows = rows(input == null ? null : input.holidays());
        List<ApplicationRow> appRows = rows(input == null ? null : input.applications());
        validate(input, userRows, providerRows, courseRows, holidayRows, appRows);

        // Validate reference integrity and collisions before any write. Transactional
        // rollback still protects against concurrent inserts and constraint failures.
        Map<String, User> knownUsers = new HashMap<>();
        for (User row : users.findAll()) knownUsers.put(key(row.getUsername()), row);
        Map<String, Course> knownCourses = new HashMap<>();
        Set<String> ambiguousExistingCourses = new HashSet<>();
        for (Course row : courses.findAll()) {
            if (knownCourses.putIfAbsent(key(row.getCourseName()), row) != null)
                ambiguousExistingCourses.add(key(row.getCourseName()));
        }
        Map<String, TrainingProvider> knownProviders = new HashMap<>();
        for (TrainingProvider row : providers.findAll())
            knownProviders.put(key(row.getName()), row);
        Set<String> newUsernames = new HashSet<>();
        for (UserRow row : userRows) {
            if (knownUsers.containsKey(key(row.username())) || users.existsByUsername(row.username()))
                throw new IllegalArgumentException("Username already exists: " + row.username());
            newUsernames.add(key(row.username()));
        }
        Set<String> newCourses = new HashSet<>();
        for (CourseRow row : courseRows) {
            if (knownCourses.containsKey(key(row.name())))
                throw new IllegalArgumentException("Course already exists: " + row.name());
            newCourses.add(key(row.name()));
        }
        for (ProviderRow row : providerRows)
            if (knownProviders.containsKey(key(row.name())))
                throw new IllegalArgumentException("Provider already exists: " + row.name());
        for (HolidayRow row : holidayRows)
            if (holidays.existsByHolidayDate(date(row.date())))
                throw new IllegalArgumentException("Holiday already exists: " + row.date());
        for (UserRow row : userRows) {
            if (row.managerUsername() != null && !row.managerUsername().isBlank() &&
                    !newUsernames.contains(key(row.managerUsername())) &&
                    !knownUsers.containsKey(key(row.managerUsername())))
                throw new IllegalArgumentException("Manager does not exist: " + row.managerUsername());
        }
        for (ApplicationRow row : appRows) {
            if (!newUsernames.contains(key(row.username())) &&
                    !knownUsers.containsKey(key(row.username())))
                throw new IllegalArgumentException("Application user does not exist: " + row.username());
            if (!newCourses.contains(key(row.courseName())) &&
                    !knownCourses.containsKey(key(row.courseName())))
                throw new IllegalArgumentException("Application course does not exist: " + row.courseName());
            if (ambiguousExistingCourses.contains(key(row.courseName())))
                throw new IllegalArgumentException("Ambiguous existing course name: " + row.courseName());
        }

        for (UserRow row : userRows) {
            User user = new User();
            user.setName(row.name()); user.setUsername(row.username()); user.setEmail(row.email());
            user.setRole(row.role());
            user.setAdmin(row.admin() == null ? row.role() == Role.ROLE_ADMIN : row.admin());
            // Null means enabled for accounts whose source does not specify an active flag.
            user.setActive(row.active());
            if (row.designation() != null) user.setDesignation(row.designation());
            user.setPassword(row.passwordHash()); // Preserve imported BCrypt hash verbatim.
            user.setBudgetedAllowance(row.budgetedAllowance());
            user.setActualAllowance(row.actualAllowance());
            user.setBudgetedDays(row.budgetedDays()); user.setActualDays(row.actualDays());
            knownUsers.put(key(row.username()), users.save(user));
        }
        for (UserRow row : userRows) {
            if (row.managerUsername() != null && !row.managerUsername().isBlank()) {
                User employee = knownUsers.get(key(row.username()));
                employee.setManager(knownUsers.get(key(row.managerUsername())));
                users.save(employee);
            }
        }

        int createdProviders = 0;
        for (ProviderRow row : providerRows) {
            TrainingProvider provider = new TrainingProvider();
            provider.setName(row.name()); provider.setEnabled(row.enabled() == null || row.enabled());
            knownProviders.put(key(row.name()), providers.save(provider));
            createdProviders++;
        }
        for (CourseRow row : courseRows) {
            TrainingProvider provider = knownProviders.get(key(row.provider()));
            if (provider == null) {
                provider = new TrainingProvider();
                provider.setName(row.provider()); provider.setEnabled(true);
                provider = providers.save(provider);
                knownProviders.put(key(row.provider()), provider);
                createdProviders++;
            }
            Course course = new Course(row.name(), row.type(), date(row.startDate()),
                    date(row.endDate()), row.location(), row.provider(), row.fee(), row.duration());
            course.setProvider(provider); course.setArchived(row.archived());
            knownCourses.put(key(row.name()), courses.save(course));
        }
        for (HolidayRow row : holidayRows) {
            Holiday holiday = new Holiday();
            holiday.setHolidayDate(date(row.date())); holiday.setHolidayName(row.name());
            holidays.save(holiday);
        }
        for (ApplicationRow row : appRows) {
            Application app = new Application();
            app.setUser(knownUsers.get(key(row.username())));
            app.setCourse(knownCourses.get(key(row.courseName())));
            app.setStatus(row.status()); app.setUserReason(row.userReason());
            app.setManagerReason(row.managerReason());
            app.setWorkDissemination(row.workDissemination());
            applications.save(app);
        }
        return new Summary(userRows.size(), createdProviders, courseRows.size(),
                holidayRows.size(), appRows.size());
    }

    private static <T> List<T> rows(List<T> rows) { return rows == null ? List.of() : rows; }

    private static void validate(ImportFile input, List<UserRow> users, List<ProviderRow> providers,
            List<CourseRow> courses, List<HolidayRow> holidays, List<ApplicationRow> apps) {
        if (input == null || !"cats-import-v1".equals(input.format()) ||
                users.size() > 100 || providers.size() > 200 || courses.size() > 500 ||
                holidays.size() > 500 || apps.size() > 1000 ||
                users.size() + providers.size() + courses.size() + holidays.size() + apps.size() == 0)
            throw new IllegalArgumentException("Unsupported CATS import format or record count.");

        Set<String> userNames = new HashSet<>(), courseNames = new HashSet<>(), providerNames = new HashSet<>();
        Set<LocalDate> holidayDates = new HashSet<>();
        Map<String, String> managerLinks = new HashMap<>();
        for (UserRow row : users) {
            if (row == null) throw new IllegalArgumentException("Null user row.");
            required(row.name(), 255, "Name"); required(row.username(), 255, "Username");
            required(row.email(), 255, "Email");
            if (row.role() == null || !row.email().matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"))
                throw new IllegalArgumentException("Invalid role or email for " + row.username());
            if (row.passwordHash() == null || !row.passwordHash().matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))
                throw new IllegalArgumentException("passwordHash must contain a valid 60-character BCrypt hash for " + row.username());
            if (row.budgetedAllowance() != null) amount(row.budgetedAllowance(), "Budgeted allowance");
            if (row.actualAllowance() != null) amount(row.actualAllowance(), "Actual allowance");
            if (row.budgetedDays() != null) days(row.budgetedDays(), "Budgeted days");
            if (row.actualDays() != null) days(row.actualDays(), "Actual days");
            if (!userNames.add(key(row.username()))) throw new IllegalArgumentException("Duplicate username in upload.");
            if (row.managerUsername() != null && !row.managerUsername().isBlank()) {
                required(row.managerUsername(), 255, "Manager username");
                managerLinks.put(key(row.username()), key(row.managerUsername()));
                if (key(row.username()).equals(key(row.managerUsername())))
                    throw new IllegalArgumentException("User cannot manage themselves.");
            }
        }
        // Reject cycles in uploaded reporting relationships before any data is saved.
        for (String username : managerLinks.keySet()) {
            Set<String> path = new HashSet<>();
            String step = username;
            while (step != null && managerLinks.containsKey(step)) {
                if (!path.add(step)) throw new IllegalArgumentException("Cyclic manager relationships in upload.");
                step = managerLinks.get(step);
            }
        }
        for (ProviderRow row : providers) {
            if (row == null) throw new IllegalArgumentException("Null provider row.");
            required(row.name(), 150, "Provider name");
            if (!providerNames.add(key(row.name()))) throw new IllegalArgumentException("Duplicate provider in upload.");
        }
        for (CourseRow row : courses) {
            if (row == null) throw new IllegalArgumentException("Null course row.");
            required(row.name(), 255, "Course name"); required(row.provider(), 150, "Provider");
            required(row.location(), 255, "Course location");
            if (row.type() == null || row.duration() == null || row.fee() == null ||
                    date(row.endDate()).isBefore(date(row.startDate())))
                throw new IllegalArgumentException("Invalid course fields.");
            amount(row.fee(), "Course fee");
            if (!courseNames.add(key(row.name()))) throw new IllegalArgumentException("Duplicate course in upload.");
        }
        for (HolidayRow row : holidays) {
            if (row == null) throw new IllegalArgumentException("Null holiday row.");
            required(row.name(), 255, "Holiday name");
            if (!holidayDates.add(date(row.date()))) throw new IllegalArgumentException("Duplicate holiday date in upload.");
        }
        for (ApplicationRow row : apps) {
            if (row == null || row.status() == null)
                throw new IllegalArgumentException("Invalid application status.");
            required(row.username(), 255, "Application username");
            required(row.courseName(), 255, "Application course");
            required(row.userReason(), 1000, "Application reason");
            if (row.managerReason() != null && row.managerReason().length() > 255)
                throw new IllegalArgumentException("Application manager reason too long.");
            if (row.workDissemination() != null && row.workDissemination().length() > 1000)
                throw new IllegalArgumentException("Work dissemination too long.");
        }
    }
    private static void amount(BigDecimal amount, String label) {
        if (amount.signum() < 0 || amount.precision() > 38 || amount.scale() > 2)
            throw new IllegalArgumentException("Invalid " + label);
    }
    private static void days(Double days, String label) {
        if (!Double.isFinite(days) || days < 0) throw new IllegalArgumentException("Invalid " + label);
    }
    private static String required(String value, int max, String name) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException(name + " must be between 1 and " + max + " characters.");
        return value;
    }
    private static LocalDate date(String text) {
        try { return LocalDate.parse(text); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid date in import.", ex); }
    }
    private static String key(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
