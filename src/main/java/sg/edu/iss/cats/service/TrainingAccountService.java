package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

/** Annual allocations are authoritative. Usage is derived from applications,
 * avoiding mutable balance arithmetic and preventing rejected/cancelled requests
 * from permanently consuming entitlement. Course fees belong to the start year;
 * working training days belong to the calendar year in which they occur. */
@Service
public class TrainingAccountService {
    private final TrainingYearRepository years;
    private final AppRepo applications;
    private final HolidayRepository holidays;
    private final UserRepository users;

    public TrainingAccountService(TrainingYearRepository years, AppRepo applications,
                                  HolidayRepository holidays, UserRepository users) {
        this.years = years;
        this.applications = applications;
        this.holidays = holidays;
        this.users = users;
    }

    @Getter @AllArgsConstructor
    public static class Summary {
        private final int year;
        private final double dayLimit;
        private final BigDecimal feeLimit;
        private final double daysCommitted;
        private final BigDecimal feesCommitted;
        private final double daysCompleted;
        private final BigDecimal feesCompleted;

        public double getDaysRemaining() { return dayLimit - daysCommitted; }
        public BigDecimal getBudgetRemaining() { return feeLimit.subtract(feesCommitted); }
        public double getDaysUsed() { return daysCompleted; }
        public BigDecimal getBudgetUsed() { return feesCompleted; }
    }

    public boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.existsByHolidayDate(date);
    }

    public Map<Integer, Double> workingDaysByYear(Course course) {
        Map<Integer, Double> result = new HashMap<>();
        if (course.getStartDate() == null || course.getEndDate() == null) return result;
        boolean halfDay = course.getDuration() == CourseDuration.HALFDAYAM
                || course.getDuration() == CourseDuration.HALFDAYPM;
        for (LocalDate date = course.getStartDate(); !date.isAfter(course.getEndDate()); date = date.plusDays(1)) {
            if (isWorkingDay(date)) {
                result.merge(date.getYear(), halfDay ? 0.5 : 1.0, Double::sum);
            }
        }
        return result;
    }

    public double workingDays(Course course) {
        return workingDaysByYear(course).values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private boolean committed(Application app) {
        return app.getStatus() == Status.APPLIED || app.getStatus() == Status.UPDATED
                || app.getStatus() == Status.APPROVED || app.getStatus() == Status.COMPLETED;
    }

    private Summary calculate(User user, int year, Integer excludedApplicationId, double dayLimit, BigDecimal feeLimit) {
        double days = 0, completedDays = 0;
        BigDecimal fees = BigDecimal.ZERO, completedFees = BigDecimal.ZERO;
        for (Application application : applications.findByUser_UserId(user.getUserId())) {
            if (excludedApplicationId != null && excludedApplicationId.equals(application.getId())) continue;
            if (!committed(application)) continue;
            Course course = application.getCourse();
            double yearDays = workingDaysByYear(course).getOrDefault(year, 0.0);
            days += yearDays;
            if (course.getStartDate().getYear() == year) fees = fees.add(course.getFee());
            if (application.getStatus() == Status.COMPLETED) {
                completedDays += yearDays;
                if (course.getStartDate().getYear() == year) completedFees = completedFees.add(course.getFee());
            }
        }
        return new Summary(year, dayLimit, feeLimit, days, fees, completedDays, completedFees);
    }

    @Transactional
    public TrainingYear allocation(User user, int year) {
        return years.findByUser_UserIdAndYear(user.getUserId(), year).orElseGet(() -> {
            TrainingYear record = new TrainingYear();
            record.setUser(user);
            record.setYear(year);
            if (year == LocalDate.now(ZoneId.of("Asia/Singapore")).getYear()) {
                // Backwards compatibility with the legacy current-year seeded balances.
                Summary completed = calculate(user, year, null, 0, BigDecimal.ZERO);
                record.setDayLimit((user.getActualDays() == null ? 0 : user.getActualDays())
                        + completed.getDaysCompleted());
                record.setFeeLimit((user.getActualAllowance() == null ? BigDecimal.ZERO : user.getActualAllowance())
                        .add(completed.getFeesCompleted()));
            } else {
                record.setDayLimit(user.getDesignation() == Designation.ADMINISTRATIVE ? 5.0 : 10.0);
                record.setFeeLimit(new BigDecimal("2000.00"));
            }
            return years.save(record);
        });
    }

    @Transactional
    public Summary summary(User user, int year) {
        return summaryExcluding(user, year, null);
    }

    @Transactional
    public Summary summaryExcluding(User user, int year, Integer excludedApplicationId) {
        TrainingYear t = allocation(user, year);
        return calculate(user, year, excludedApplicationId, t.getDayLimit(), t.getFeeLimit());
    }

    @Transactional
    public void validateCapacity(User user, Course course, Integer excludedId) {
        Map<Integer, Double> requested = workingDaysByYear(course);
        for (Map.Entry<Integer, Double> entry : requested.entrySet()) {
            Summary balance = summaryExcluding(user, entry.getKey(), excludedId);
            if (balance.getDaysRemaining() + 1e-8 < entry.getValue()) {
                throw new IllegalArgumentException("Insufficient training days for " + entry.getKey());
            }
        }
        int feeYear = course.getStartDate().getYear();
        if (summaryExcluding(user, feeYear, excludedId).getBudgetRemaining().compareTo(course.getFee()) < 0) {
            throw new IllegalArgumentException("Insufficient training budget for " + feeYear);
        }
    }

    @Transactional
    public TrainingYear updateAllocation(User user, int year, double limitDays, BigDecimal limitFee) {
        if (year < 2000 || year > 9999 || limitDays < 0 || limitFee == null || limitFee.signum() < 0) {
            throw new IllegalArgumentException("Invalid annual entitlement");
        }
        Summary used = calculate(user, year, null, limitDays, limitFee);
        if (used.getDaysRemaining() < -1e-8 || used.getBudgetRemaining().signum() < 0) {
            throw new IllegalArgumentException("Annual entitlement cannot be lower than existing commitments");
        }
        TrainingYear record = allocation(user, year);
        record.setDayLimit(limitDays);
        record.setFeeLimit(limitFee);
        return years.save(record);
    }

    @Transactional
    public void syncLegacyCurrentYear(User user) {
        int year = LocalDate.now(ZoneId.of("Asia/Singapore")).getYear();
        Summary current = summary(user, year);
        user.setBudgetedDays(current.getDaysRemaining());
        user.setBudgetedAllowance(current.getBudgetRemaining());
        user.setActualDays(current.getDayLimit() - current.getDaysCompleted());
        user.setActualAllowance(current.getFeeLimit().subtract(current.getFeesCompleted()));
        users.save(user);
    }
}
