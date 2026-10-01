package sg.edu.iss.cats.repository;

import sg.edu.iss.cats.model.Holiday;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Integer>{

    public boolean existsByHolidayDate(LocalDate date);
    
}
