package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;

@Service
public class BudgetService {

    // Provisional rule: pending and accepted applications reserve budget;
    // cancelled, rejected and deleted applications do not.
    private static final EnumSet<ApplicationStatus> USING_BUDGET = EnumSet.of(
            ApplicationStatus.APPLIED, ApplicationStatus.UPDATED,
            ApplicationStatus.APPROVED, ApplicationStatus.COMPLETED);

    public BigDecimal remainingBudget(User user, List<Application> applications) {
        BigDecimal remaining = user.getBudget();
        int currentYear = LocalDate.now().getYear();

        for (Application application : applications) {
            if (!USING_BUDGET.contains(application.getApplicationStatus())) {
                continue;
            }

            // Catalogue courses use Course; employee-entered courses store details on Application.
            LocalDate startDate = application.getCourse() == null
                    ? application.getExternalStartDate() : application.getCourse().getStartDate();
            BigDecimal fee = application.getCourse() == null
                    ? application.getExternalFee() : application.getCourse().getFee();

            if (startDate != null && startDate.getYear() == currentYear && fee != null) {
                remaining = remaining.subtract(fee);
            }
        }

        return remaining;
    }
}
