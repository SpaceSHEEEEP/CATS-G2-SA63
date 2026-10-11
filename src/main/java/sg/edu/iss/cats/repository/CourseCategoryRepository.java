package sg.edu.iss.cats.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.iss.cats.model.CourseCategory;
import sg.edu.iss.cats.model.CourseType;
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, CourseType> {}
