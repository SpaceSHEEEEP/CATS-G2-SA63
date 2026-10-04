package sg.edu.iss.cats.repository;

import sg.edu.iss.cats.model.Holiday;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Integer>{

    public boolean existsByHolidayDate(LocalDate date);
    
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate startDate, LocalDate endDate);
}
