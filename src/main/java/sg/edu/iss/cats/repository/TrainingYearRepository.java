package sg.edu.iss.cats.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.iss.cats.model.TrainingYear;

public interface TrainingYearRepository extends JpaRepository<TrainingYear, Integer> {
    Optional<TrainingYear> findByUser_UserIdAndYear(Integer userId, int year);
}
