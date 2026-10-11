package sg.edu.iss.cats.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.iss.cats.model.TrainingProvider;
public interface TrainingProviderRepository extends JpaRepository<TrainingProvider, Integer> {
    List<TrainingProvider> findByEnabledTrueOrderByNameAsc();
    List<TrainingProvider> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
