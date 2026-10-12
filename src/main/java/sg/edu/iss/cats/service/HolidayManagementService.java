package sg.edu.iss.cats.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.Holiday;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.HolidayRepository;

@Service
public class HolidayManagementService {
    private final HolidayRepository holidays;
    private final AppRepo applications;
    public HolidayManagementService(HolidayRepository holidays, AppRepo applications) {
        this.holidays = holidays; this.applications = applications;
    }

    @Transactional
    public void save(Integer id, LocalDate date, String name) {
        if (date == null || date.getYear() < 2000 || date.getYear() > 9999
                || name == null || name.isBlank() || name.strip().length() > 255)
            throw new IllegalArgumentException("Holiday date and name (max 255 characters) are required");
        Holiday holiday = id == null ? new Holiday() : holidays.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found"));
        if (holidays.existsByHolidayDateAndHolidayIdNot(date, id == null ? -1 : id))
            throw new IllegalArgumentException("A holiday is already recorded on this date");
        if (id == null && applications.hasActiveApplicationOnDate(date))
            throw new IllegalArgumentException("Adding a holiday would change existing application balances");
        if (id != null && !date.equals(holiday.getHolidayDate())
                && (applications.hasActiveApplicationOnDate(holiday.getHolidayDate())
                    || applications.hasActiveApplicationOnDate(date)))
            throw new IllegalArgumentException("Changing this date would change a recorded training entitlement");
        holiday.setHolidayDate(date);
        holiday.setHolidayName(name.strip());
        holidays.save(holiday);
    }

    @Transactional
    public void delete(Integer id) {
        Holiday holiday = holidays.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found"));
        if (applications.hasActiveApplicationOnDate(holiday.getHolidayDate()))
            throw new IllegalArgumentException("Holiday affects recorded courses and cannot be deleted");
        holidays.delete(holiday);
    }
}
